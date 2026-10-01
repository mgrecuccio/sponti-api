package com.mgrtech.sponti_api.auth.internal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtProperties properties;
    private final SecretKey secretKey;
    private final Clock clock;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String issueAccessToken(Long userId, String phoneNumber, Collection<String> roles) {
        var now = Instant.now(clock);
        var expiresAt = now.plus(properties.accessTokenMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .claim("number", phoneNumber)
                .claim("roles", List.copyOf(roles))
                .claim("typ", "access")
                .signWith(secretKey())
                .compact();
    }

    public boolean isValidAccessToken(String token) {
        try {
            var claims = parse(token).getPayload();
            return "access".equals(claims.get("typ", String.class))
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(Date.from(Instant.now(clock)))
                    && properties.issuer().equals(claims.getIssuer());
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getPayload().getSubject());
    }

    public List<String> extractRoles(String token) {
        Object roles = parse(token).getPayload().get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clock(() -> Date.from(Instant.now(clock)))
                .build()
                .parseSignedClaims(token);
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
