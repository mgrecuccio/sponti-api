package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.user.api.deletion.UserDeletionFacade;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskStatus;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.user.internal.domain.UserDeletionTaskEntity;
import com.mgrtech.sponti_api.user.internal.domain.UserEntity;
import com.mgrtech.sponti_api.user.internal.repository.UserDeletionTaskRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
class UserDeletionApplicationService implements UserDeletionFacade {

    private final UserDeletionTaskRepository deletionTaskRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public void requestDeletion(Long userId) {
        log.info("Request deletion started for user {}", userId);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Impossible to request deletion: user not found."));

        if (user.isDeleted()) {
            return;
        }

        user.requestDeletion();
        createDeletionTasks(user);
        markTaskCompleted(userId, UserDeletionModule.USER);

        eventPublisher.publishEvent(new UserDeletionRequestEvent(userId));
    }

    @Override
    @Transactional
    public void markTaskCompleted(Long userId, UserDeletionModule module) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var task = deletionTaskRepository.findByUser_IdAndModule(user.getId(), module)
                .orElseGet(() -> deletionTaskRepository.save(new UserDeletionTaskEntity(user, module)));

        task.markCompleted(Instant.now(clock));
        markDeletedIfReady(user.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markTaskFailed(Long userId, UserDeletionModule module, String reason) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var task = deletionTaskRepository.findByUser_IdAndModule(user.getId(), module)
                .orElseGet(() -> deletionTaskRepository.save(new UserDeletionTaskEntity(user, module)));

        task.markFailed(Instant.now(clock), reason);
    }

    private void createDeletionTasks(UserEntity user) {
        Stream.of(UserDeletionModule.values()).forEach(module -> {
            if (!deletionTaskRepository.existsByUser_IdAndModule(user.getId(), module)) {
                log.debug("Creating deletion task for user {}, module={}", user.getId(), module);
                deletionTaskRepository.save(new UserDeletionTaskEntity(user, module));
                log.debug("Deletion task for user {}, module={} created", user.getId(), module);
            }
        });
    }

    private void markDeletedIfReady(Long userId) {
        if (!deletionTaskRepository.allCompletedForUser(userId, UserDeletionTaskStatus.COMPLETED)) {
            return;
        }

        var user = userRepository.findById(userId)
                .orElseThrow();

        if (user.isDeleted()) {
            return;
        }

        userPreferenceRepository.deleteByUserId(userId);
        user.anonymizeForDeletion(Instant.now(clock));
        log.info("User deletion completed for userId={}", userId);
    }
}
