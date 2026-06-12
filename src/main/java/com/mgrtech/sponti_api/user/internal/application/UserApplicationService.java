package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.shared.error.EmailAlreadyUsedException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserCredentialsQuery;
import com.mgrtech.sponti_api.user.api.query.UserLookupQuery;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.api.view.CreatedUserView;
import com.mgrtech.sponti_api.user.api.view.UserCredentialsView;
import com.mgrtech.sponti_api.user.api.view.UserLookupView;
import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;
import com.mgrtech.sponti_api.user.internal.domain.UserEntity;
import com.mgrtech.sponti_api.user.internal.domain.UserPreferenceEntity;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.normalizeEmail;
import static com.mgrtech.sponti_api.user.internal.domain.UserEntity.defaultMatchingPreferencesView;

@Service
@AllArgsConstructor
public class UserApplicationService implements
        UserFacade,
        UserRegistrationFacade,
        UserCredentialsQuery,
        UserProfileQuery,
        UserLookupQuery,
        UserMatchingPreferencesQuery
{
    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredentialsView> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
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
    public Optional<UserLookupView> findByEmailForLookup(String email) {
        return userRepository.findByEmail(email)
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
    public List<Long> getMatchingEnabledUserIds() {
        return userPreferenceRepository.findMatchingEnabledUserIds();
    }

    @Override
    @Transactional
    public CreatedUserView createUser(CreateUserCommand command) {
        log.info("Registering user: email={}", command.email());
        var normalizedEmail = normalizeEmail(command.email());

        if(userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration blocked: email={} already exists", command.email());
            throw new EmailAlreadyUsedException("Email already used");
        }

        var user = new UserEntity(
                normalizedEmail,
                command.passwordHash(),
                command.displayName(),
                command.timezone()
        );

        var persistedUser = userRepository.save(user);
        userPreferenceRepository.save(new UserPreferenceEntity(persistedUser));
        log.info("User registered: userId={}", persistedUser.getId());

        return new CreatedUserView(
                persistedUser.getId(),
                persistedUser.getEmail(),
                persistedUser.getDisplayName(),
                persistedUser.getStatusAsString()
        );
    }

    @Override
    @Transactional
    public UserProfileView update(Long userId, UpdateUserCommand command) {
        log.info("Updating userId={}", userId);

        var user = userRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException("Impossible to update the profile: user not found."));

        user.update(command.displayName(), command.timezone());
        log.info("UserId={} updated.", userId);
        return UserEntity.toProfileView(user);
    }

    private UserMatchingPreferencesView toMatchingPreferencesView(UserEntity user, UserPreferenceEntity preferences) {
        return new UserMatchingPreferencesView(
                user.getId(),
                user.getTimezone(),
                preferences.isAllowChat(),
                preferences.isAllowCall(),
                preferences.getQuietHoursStart(),
                preferences.getQuietHoursEnd()
        );
    }
}
