package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.shared.error.TooManyAttemptsException;
import com.mgrtech.sponti_api.sms.api.OtpFacade;
import com.mgrtech.sponti_api.user.internal.application.command.UpdateUserCommand;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserPreferenceRepository userPreferenceRepository = mock(UserPreferenceRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final OtpFacade otpFacade = mock(OtpFacade.class);
    private final UserApplicationService service = new UserApplicationService(
            userRepository,
            userPreferenceRepository,
            eventPublisher,
            CLOCK,
            otpFacade
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
        user.verify(NOW);
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
        assertThat(user.getPhoneNumberChangedAt()).isNotNull();
        assertThat(user.isPhoneNumberVerified()).isFalse();
        assertThat(user.isWhatsAppOptIn()).isFalse();
        assertThat(user.getPhoneNumberVerifiedAt()).isNull();
    }

    @Test
    void updateProfileDoesNotPublishPhoneNumberChangedEventWhenPhoneNumberIsUnchanged() {
        var user = new UserEntity("hash", "John", "+32468009911", "UTC");
        user.verify(NOW);
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

    @Test
    void updateProfileAllowsKeepingSamePhoneNumberWithinOneDay() {
        var user = new UserEntity("hash", "John", "+32468009911", "UTC");
        ReflectionTestUtils.setField(user, "phoneNumberChangedAt", NOW.minus(1, ChronoUnit.HOURS));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndIdNot("+32468009911", 42L)).thenReturn(false);

        service.updateProfile(42L, new UpdateUserCommand(
                "John Updated",
                "Europe/Brussels",
                "+32468009911"
        ));

        verify(eventPublisher, never()).publishEvent(any());
        assertThat(user.getDisplayName()).isEqualTo("John Updated");
        assertThat(user.getTimezone()).isEqualTo("Europe/Brussels");
        assertThat(user.getPhoneNumber()).isEqualTo("+32468009911");
    }

    @Test
    void updateProfileRejectsPhoneNumberChangeWithinOneDay() {
        var user = new UserEntity("hash", "John", "+32468009911", "UTC");
        ReflectionTestUtils.setField(user, "phoneNumberChangedAt", NOW.minus(23, ChronoUnit.HOURS));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndIdNot("+32468009912", 42L)).thenReturn(false);

        assertThatThrownBy(() -> service.updateProfile(42L, new UpdateUserCommand(
                "John Updated",
                "Europe/Brussels",
                "+32468009912"
        ))).isInstanceOf(TooManyAttemptsException.class);

        verify(eventPublisher, never()).publishEvent(any());
        assertThat(user.getPhoneNumber()).isEqualTo("+32468009911");
    }

    @Test
    void updateProfileAllowsPhoneNumberChangeAfterOneDay() {
        var user = new UserEntity("hash", "John", "+32468009911", "UTC");
        ReflectionTestUtils.setField(user, "phoneNumberChangedAt", NOW.minus(25, ChronoUnit.HOURS));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndIdNot("+32468009912", 42L)).thenReturn(false);

        service.updateProfile(42L, new UpdateUserCommand(
                "John Updated",
                "Europe/Brussels",
                "+32468009912"
        ));

        verify(eventPublisher).publishEvent(new UserPhoneNumberChangedEvent(42L, "+32468009912"));
        assertThat(user.getPhoneNumber()).isEqualTo("+32468009912");
    }
}
