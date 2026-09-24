package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.shared.error.PhoneNumberAlreadyUsedException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.shared.error.UserPreferencesNotFoundException;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.UserVerificationFacade;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.command.UpdatePreferencesCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.command.VerifyUserPhoneCommand;
import com.mgrtech.sponti_api.user.api.event.UserCreatedEvent;
import com.mgrtech.sponti_api.user.api.event.UserPhoneNumberChangedEvent;
import com.mgrtech.sponti_api.user.api.query.*;
import com.mgrtech.sponti_api.user.api.view.*;
import com.mgrtech.sponti_api.user.internal.domain.UserEntity;
import com.mgrtech.sponti_api.user.internal.domain.UserPreferenceEntity;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.normalizeE164PhoneNumber;
import static com.mgrtech.sponti_api.user.internal.domain.UserEntity.defaultMatchingPreferencesView;
import static com.mgrtech.sponti_api.user.internal.domain.UserEntity.toProfileView;
import static com.mgrtech.sponti_api.user.internal.domain.UserPreferenceEntity.toMatchingPreferencesView;

@Service
@AllArgsConstructor
public class UserApplicationService implements
        UserFacade,
        UserPreferenceFacade,
        UserRegistrationFacade,
        UserCredentialsQuery,
        UserProfileQuery,
        UserLookupQuery,
        UserMatchingPreferencesQuery,
        UserContactInfoQuery,
        UserVerificationFacade
{
    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredentialsView> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(normalizeE164PhoneNumber(phoneNumber))
                .map(UserEntity::toCredentialsView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredentialsView> findById(Long id) {
        return userRepository.findById(id)
                .map(UserEntity::toCredentialsView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileView> getProfileById(Long userId) {
        return userRepository.findById(userId)
                .map(UserEntity::toProfileView);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, UserProfileView> getProfilesByIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds)
                .stream()
                .map(UserEntity::toProfileView)
                .collect(Collectors.toMap(UserProfileView::id, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserLookupView> findByPhoneNumberForLookup(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(UserEntity::toLookupView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserMatchingPreferencesView> getMatchingPreferences(Long userId) {
        return userRepository.findById(userId)
                .map(user -> userPreferenceRepository.findByUserId(userId)
                        .map(preferences -> toMatchingPreferencesView(user, preferences))
                        .orElseGet(() -> defaultMatchingPreferencesView(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPhoneNumber(Long userId) {
        return getPhoneNumber(userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getPhoneNumber(Long userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getPhoneNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMatchingEnabledUserIds() {
        return userPreferenceRepository.findMatchingEnabledUserIds();
    }

    @Override
    @Transactional
    public CreatedUserView createUser(CreateUserCommand command) {
        var phoneNumber = normalizeE164PhoneNumber(command.phoneNumber());
        log.info("Registering user: number={}", phoneNumber);

        if(userRepository.existsByPhoneNumber(phoneNumber)) {
            log.warn("Registration blocked: number={} already exists", phoneNumber);
            throw new PhoneNumberAlreadyUsedException("Phone number already used");
        }

        var user = new UserEntity(
                command.passwordHash(),
                command.displayName(),
                phoneNumber,
                command.timezone()
        );

        var persistedUser = userRepository.save(user);
        userPreferenceRepository.save(new UserPreferenceEntity(persistedUser));
        eventPublisher.publishEvent(new UserCreatedEvent(
                persistedUser.getId(),
                persistedUser.getPhoneNumber(),
                command.clientIp()
        ));
        log.info("User registered: userId={}", persistedUser.getId());

        return new CreatedUserView(
                persistedUser.getId(),
                persistedUser.getPhoneNumber(),
                persistedUser.getDisplayName(),
                persistedUser.getStatusAsString()
        );
    }

    @Override
    @Transactional
    public UserProfileView updateProfile(Long userId, UpdateUserCommand command) {
        log.info("Updating userId={}", userId);
        var phoneNumber = normalizeE164PhoneNumber(command.phoneNumber());

        var user = userRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException("Impossible to update the profile: user not found."));

        if(userRepository.existsByPhoneNumberAndIdNot(phoneNumber, userId)) {
            log.warn("Profile update blocked: number={} already exists for another user", phoneNumber);
            throw new PhoneNumberAlreadyUsedException("Phone number already used");
        }

        var phoneNumberChanged = !phoneNumber.equals(user.getPhoneNumber());
        if(phoneNumberChanged) {
            log.info("Phone number changed for userId={}. Resetting verification.", userId);
            user.resetVerification();
        }

        user.update(command.displayName(), command.timezone(), phoneNumber);
        if(phoneNumberChanged) {
            eventPublisher.publishEvent(new UserPhoneNumberChangedEvent(userId, phoneNumber));
            log.info("New OTP code requested for userId={}", userId);
        }

        log.info("UserId={} updated.", userId);
        return toProfileView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPrivateProfileView getCurrentUserProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserEntity::toPrivateProfileView)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
    }

    @Override
    @Transactional
    public UserMatchingPreferencesView updatePreferences(Long userId, UpdatePreferencesCommand command) {
        log.info("Preferences updated for userId={}: allowChat={}, allowCall={}, quietHoursStart={}, quietHoursEnd={}, pushNotificationsEnabled={}, suggestionNotificationsEnabled={}",
                userId, command.allowChat(), command.allowCall(), command.quietHoursStart(), command.quietHoursEnd(), command.pushNotificationsEnabled(), command.suggestionNotificationsEnabled());

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Impossible to update the preferences: user not found."));

        var preferences = userPreferenceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserPreferencesNotFoundException("No user preferences found."));

        preferences.update(
                command.allowChat(),
                command.allowCall(),
                command.quietHoursStart(),
                command.quietHoursEnd(),
                command.pushNotificationsEnabled(),
                command.suggestionNotificationsEnabled()
        );
        log.info("Preferences updated for userId={}: allowChat={}, allowCall={}, quietHoursStart={}, quietHoursEnd={}, pushNotificationsEnabled={}, suggestionNotificationsEnabled={}",
                userId, preferences.isAllowChat(), preferences.isAllowCall(), preferences.getQuietHoursStart(), preferences.getQuietHoursEnd(), preferences.isPushNotificationsEnabled(), preferences.isSuggestionNotificationsEnabled());
        return toMatchingPreferencesView(user, preferences);
    }

    @Override
    @Transactional
    public void verify(VerifyUserPhoneCommand command) {
        var userid = command.userId();
        log.info("verifying phone number for userid={}", userid);

        var user = userRepository.findById(userid)
                .orElseThrow(() -> new UserNotFoundException("impossible to verify the user: user not found."));

        user.verify();
        log.info("Phone number for userid={} verified", userid);
    }
}
