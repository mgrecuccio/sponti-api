package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionRequestEvent;
import com.mgrtech.sponti_api.user.internal.domain.UserDeletionTaskEntity;
import com.mgrtech.sponti_api.user.internal.domain.UserEntity;
import com.mgrtech.sponti_api.user.internal.repository.UserDeletionTaskRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeletionApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final UserDeletionTaskRepository deletionTaskRepository = mock(UserDeletionTaskRepository.class);
    private final UserPreferenceRepository userPreferenceRepository = mock(UserPreferenceRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final UserDeletionApplicationService service = new UserDeletionApplicationService(
            deletionTaskRepository,
            userPreferenceRepository,
            userRepository,
            eventPublisher,
            CLOCK
    );

    @Test
    void requestDeletionCreatesMissingTasksMarksUserTaskCompletedAndPublishesRequestEvent() {
        var user = user(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(deletionTaskRepository.existsByUser_IdAndModule(eq(42L), any())).thenReturn(false);
        when(deletionTaskRepository.save(any(UserDeletionTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deletionTaskRepository.findByUser_IdAndModule(42L, UserDeletionModule.USER))
                .thenReturn(Optional.of(new UserDeletionTaskEntity(user, UserDeletionModule.USER)));

        service.requestDeletion(42L);

        assertThat(user.isDeletionRequested()).isTrue();
        for (UserDeletionModule module : UserDeletionModule.values()) {
            verify(deletionTaskRepository).existsByUser_IdAndModule(42L, module);
        }
        verify(deletionTaskRepository, times(UserDeletionModule.values().length)).save(any(UserDeletionTaskEntity.class));

        var eventCaptor = ArgumentCaptor.forClass(UserDeletionRequestEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new UserDeletionRequestEvent(42L));
    }

    @Test
    void requestDeletionCreatesTasksForEachRequestedUser() {
        var firstUser = user(42L);
        var secondUser = user(43L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(firstUser));
        when(userRepository.findById(43L)).thenReturn(Optional.of(secondUser));
        when(deletionTaskRepository.existsByUser_IdAndModule(anyLong(), any())).thenReturn(false);
        when(deletionTaskRepository.save(any(UserDeletionTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deletionTaskRepository.findByUser_IdAndModule(42L, UserDeletionModule.USER))
                .thenReturn(Optional.of(new UserDeletionTaskEntity(firstUser, UserDeletionModule.USER)));
        when(deletionTaskRepository.findByUser_IdAndModule(43L, UserDeletionModule.USER))
                .thenReturn(Optional.of(new UserDeletionTaskEntity(secondUser, UserDeletionModule.USER)));

        service.requestDeletion(42L);
        service.requestDeletion(43L);

        for (UserDeletionModule module : UserDeletionModule.values()) {
            verify(deletionTaskRepository).existsByUser_IdAndModule(42L, module);
            verify(deletionTaskRepository).existsByUser_IdAndModule(43L, module);
        }
        verify(deletionTaskRepository, times(UserDeletionModule.values().length * 2))
                .save(any(UserDeletionTaskEntity.class));
        verify(eventPublisher).publishEvent(new UserDeletionRequestEvent(42L));
        verify(eventPublisher).publishEvent(new UserDeletionRequestEvent(43L));
    }

    @Test
    void markTaskFailedMarksExistingTaskAsFailed() {
        var user = user(42L);
        var task = new UserDeletionTaskEntity(user, UserDeletionModule.AVAILABILITY);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(deletionTaskRepository.findByUser_IdAndModule(42L, UserDeletionModule.AVAILABILITY))
                .thenReturn(Optional.of(task));

        service.markTaskFailed(42L, UserDeletionModule.AVAILABILITY, "database unavailable");

        assertThat(task.getStatus().name()).isEqualTo("FAILED");
        assertThat(task.getErrorMessage()).isEqualTo("database unavailable");
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    void markTaskCompletedFinalizesUserWhenAllTasksAreCompleted() {
        var user = user(42L);
        var task = new UserDeletionTaskEntity(user, UserDeletionModule.SMS);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(deletionTaskRepository.findByUser_IdAndModule(42L, UserDeletionModule.SMS))
                .thenReturn(Optional.of(task));
        when(deletionTaskRepository.allCompletedForUser(42L, com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskStatus.COMPLETED))
                .thenReturn(true);

        service.markTaskCompleted(42L, UserDeletionModule.SMS);

        verify(userPreferenceRepository).deleteByUserId(42L);
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getDeletedAt()).isEqualTo(NOW);
    }

    private static UserEntity user(Long id) {
        var user = new UserEntity("hash", "John", "+32468009911", "UTC");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
