package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.auth.internal.domain.RefreshToken;
import com.mgrtech.sponti_api.auth.internal.repository.RefreshTokenRepository;
import com.mgrtech.sponti_api.auth.internal.security.JwtProperties;
import com.mgrtech.sponti_api.shared.error.ExpiredRefreshTokenException;
import com.mgrtech.sponti_api.shared.error.InvalidRefreshTokenException;
import com.mgrtech.sponti_api.shared.error.RevokedRefreshTokenException;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final OperationalMetrics metrics;
    private final SecureRandom secureRandom;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            JwtProperties properties,
            OperationalMetrics metrics
    ) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public String issue(Long userId) {
        var rawToken = generate();
        var hash = hash(rawToken);

        var now = Instant.now();
        var expires = now.plus(properties.refreshTokenDays(), ChronoUnit.DAYS);
        repository.save(new RefreshToken(userId, hash, now, expires));

        return rawToken;
    }

    public RotateToken rotate(String presentedToken) {
        var hash = hash(presentedToken);
        var now = Instant.now();

        var existing = repository.findByTokenHash(hash)
                .orElseThrow(() -> {
                    metrics.refreshRotationFailure("invalid");
                    log.warn("Refresh token rotation rejected: reason=invalid");
                    return new InvalidRefreshTokenException("Invalid refresh token");
                });

        if (existing.isRevoked()) {
            metrics.refreshRotationFailure("revoked");
            log.warn("Refresh token rotation rejected: reason=revoked userId={}", existing.getUserId());
            throw new RevokedRefreshTokenException("Refresh token has been revoked");
        }

        if (existing.isExpired(now)) {
            metrics.refreshRotationFailure("expired");
            log.warn("Refresh token rotation rejected: reason=expired userId={}", existing.getUserId());
            throw new ExpiredRefreshTokenException("Refresh token has expired");
        }

        var newRaw = generate();
        var newHash = hash(newRaw);

        existing.revoke(now, newHash);

        var expires = now.plus(properties.refreshTokenDays(), ChronoUnit.DAYS);
        repository.save(new RefreshToken(existing.getUserId(), newHash, now, expires));

        return new RotateToken(existing.getUserId(), newRaw);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        var now = Instant.now();
        var tokens = repository.findAllByUserId(userId);
        for (RefreshToken token : tokens) {
            if (token.isActive(now)) {
                token.revoke(now, null);
            }
        }
    }

    private String generate() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    public record RotateToken(Long userId, String rawToken) {}
}
