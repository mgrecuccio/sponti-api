package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.shared.error.EmailAlreadyUsedException;
import com.mgrtech.sponti_api.user.api.UserCredentialsQuery;
import com.mgrtech.sponti_api.user.api.UserQueryFacade;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.CreatedUserView;
import com.mgrtech.sponti_api.user.api.UserCredentialsView;
import com.mgrtech.sponti_api.user.api.UserProfileView;
import com.mgrtech.sponti_api.user.internal.domain.UserEntity;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserApplicationService implements UserRegistrationFacade, UserCredentialsQuery, UserQueryFacade {

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
    public Optional<UserProfileView> getProfileById(Long userId) {
        return userRepository.findById(userId)
                .map(this::toProfileView);
    }

    @Override
    @Transactional
    public CreatedUserView createUser(CreateUserCommand command) {
        var normalizedEmail = normalizeEmail(command.email());

        if(userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyUsedException("Email already used");
        }

        var user = new UserEntity(
                normalizedEmail,
                command.passwordHash(),
                command.displayName()
        );

        var persistedUser = userRepository.save(user);
        return new CreatedUserView(
                persistedUser.getId(),
                persistedUser.getEmail(),
                persistedUser.getDisplayName(),
                persistedUser.getStatusAsString()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
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
                user.getStatusAsString()
        );
    }
}
