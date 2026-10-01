package com.mgrtech.sponti_api.auth.internal.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final JwtTokenService service = new JwtTokenService(new JwtProperties(
            "01234567890123456789012345678901",
            "sponti-test",
            15,
            30
    ), CLOCK);

    @Test
    void issueAccessToken_stores_roles_as_flat_claim() {
        var token = service.issueAccessToken(
                42L,
                "+32468009911",
                List.of("ROLE_USER", "ROLE_ADMIN")
        );

        assertThat(service.extractRoles(token))
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void issueAccessToken_can_extract_user_id() {
        var token = service.issueAccessToken(
                42L,
                "+32468009911",
                List.of("ROLE_USER")
        );

        assertThat(service.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void issueAccessToken_creates_valid_access_token() {
        var token = service.issueAccessToken(
                42L,
                "+32468009911",
                List.of("ROLE_USER")
        );

        assertThat(service.isValidAccessToken(token)).isTrue();
    }
}
