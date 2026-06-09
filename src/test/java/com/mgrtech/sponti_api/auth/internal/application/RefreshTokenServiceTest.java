package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.auth.internal.domain.RefreshToken;
import com.mgrtech.sponti_api.auth.internal.repository.RefreshTokenRepository;
import com.mgrtech.sponti_api.auth.internal.security.JwtProperties;
import com.mgrtech.sponti_api.shared.error.ExpiredRefreshTokenException;
import com.mgrtech.sponti_api.shared.error.InvalidRefreshTokenException;
import com.mgrtech.sponti_api.shared.error.RevokedRefreshTokenException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(
            repository,
            new JwtProperties("01234567890123456789012345678901", "sponti-test", 15, 30)
    );

    @Test
    void rotate_throws_invalid_refresh_token_when_token_is_unknown() {
        when(repository.findByTokenHash(hash("unknown-token"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid refresh token");

        verify(repository, never()).save(any());
    }

    @Test
    void rotate_throws_revoked_refresh_token_when_token_was_revoked() {
        var token = new RefreshToken(1L, hash("revoked-token"), Instant.now(), Instant.now().plusSeconds(3600));
        token.revoke(Instant.now(), null);
        when(repository.findByTokenHash(hash("revoked-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.rotate("revoked-token"))
                .isInstanceOf(RevokedRefreshTokenException.class)
                .hasMessage("Refresh token has been revoked");

        verify(repository, never()).save(any());
    }

    @Test
    void rotate_throws_expired_refresh_token_when_token_is_expired() {
        var token = new RefreshToken(1L, hash("expired-token"), Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));
        when(repository.findByTokenHash(hash("expired-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.rotate("expired-token"))
                .isInstanceOf(ExpiredRefreshTokenException.class)
                .hasMessage("Refresh token has expired");

        verify(repository, never()).save(any());
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
