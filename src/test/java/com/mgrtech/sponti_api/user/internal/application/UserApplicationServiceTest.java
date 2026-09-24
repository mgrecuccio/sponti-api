package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.event.UserCreatedEvent;
import com.mgrtech.sponti_api.user.api.event.UserPhoneNumberChangedEvent;
import com.mgrtech.sponti_api.user.internal.domain.UserEntity;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserPreferenceRepository userPreferenceRepository = mock(UserPreferenceRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UserApplicationService service = new UserApplicationService(
            userRepository,
            userPreferenceRepository,
            eventPublisher
    );

    @Test
    void createUserPublishesUserCreatedEvent() {
        when(userRepository.existsByPhoneNumber("+32468009911")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return user;
        });

        service.createUser(new CreateUserCommand(
                "hash",
                "John",
                "+32468009911",
                "UTC"
        ));

        var eventCaptor = ArgumentCaptor.forClass(UserCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isEqualTo(new UserCreatedEvent(42L, "+32468009911"));
    }

    @Test
    void updateProfilePublishesPhoneNumberChangedEventWhenPhoneNumberChanges() {
        var user = new UserEntity("hash", "John", "+32468009911", "UTC");
        user.verify();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndIdNot("+32468009912", 42L)).thenReturn(false);

        service.updateProfile(42L, new UpdateUserCommand(
                "John Updated",
                "Europe/Brussels",
                "+32468009912"
        ));

        var eventCaptor = ArgumentCaptor.forClass(UserPhoneNumberChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isEqualTo(new UserPhoneNumberChangedEvent(42L, "+32468009912"));
        assertThat(user.getPhoneNumber()).isEqualTo("+32468009912");
        assertThat(user.isPhoneNumberVerified()).isFalse();
        assertThat(user.isWhatsAppOptIn()).isFalse();
        assertThat(user.getPhoneNumberVerifiedAt()).isNull();
    }

    @Test
    void updateProfileDoesNotPublishPhoneNumberChangedEventWhenPhoneNumberIsUnchanged() {
        var user = new UserEntity("hash", "John", "+32468009911", "UTC");
        user.verify();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndIdNot("+32468009911", 42L)).thenReturn(false);

        service.updateProfile(42L, new UpdateUserCommand(
                "John Updated",
                "Europe/Brussels",
                "+32468009911"
        ));

        verify(eventPublisher, never()).publishEvent(any());
        assertThat(user.getPhoneNumber()).isEqualTo("+32468009911");
        assertThat(user.isPhoneNumberVerified()).isTrue();
        assertThat(user.isWhatsAppOptIn()).isTrue();
        assertThat(user.getPhoneNumberVerifiedAt()).isNotNull();
    }
}
