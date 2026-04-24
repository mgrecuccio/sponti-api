package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.shared.error.EmailAlreadyUsedException;
import com.mgrtech.sponti_api.user.api.*;
import com.mgrtech.sponti_api.user.internal.domain.UserEntity;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.normalizeEmail;

@Service
@AllArgsConstructor
public class UserApplicationService implements UserRegistrationFacade, UserCredentialsQuery, UserQueryFacade {

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredentialsView> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(this::toCredentialsView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredentialsView> findById(Long id) {
        return userRepository.findById(id)
                .map(this::toCredentialsView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileView> getProfileById(Long userId) {
        return userRepository.findById(userId)
                .map(this::toProfileView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserLookupView> findByEmailForLookup(String email) {
        return userRepository.findByEmail(email)
                .map(this::toLookupView);
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
        log.info("User registered: userId={}", persistedUser.getId());

        return new CreatedUserView(
                persistedUser.getId(),
                persistedUser.getEmail(),
                persistedUser.getDisplayName(),
                persistedUser.getStatusAsString()
        );
    }

    private UserCredentialsView toCredentialsView(UserEntity user) {
        return new UserCredentialsView(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash()
        );
    }

    private UserProfileView toProfileView(UserEntity user) {
        return new UserProfileView(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatusAsString(),
                user.getTimezone()
        );
    }

    private UserLookupView toLookupView(UserEntity user) {
        return new UserLookupView(user.getId(), user.getEmail());
    }
}
