package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.AuthTokens;
import com.mgrtech.sponti_api.auth.api.LoginCommand;
import com.mgrtech.sponti_api.auth.api.RegisterCommand;
import com.mgrtech.sponti_api.auth.internal.security.JwtProperties;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.shared.error.BadCredentialsException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserCredentialsQuery;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.normalizeE164PhoneNumber;

@Service
@Transactional
class AuthApplicationService implements AuthFacade {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);
    private static final List<String> DEFAULT_ROLES = List.of("ROLE_USER");
    public static final String TOKEN_TYPE = "Bearer";

    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserRegistrationFacade userRegistrationFacade;
    private final UserCredentialsQuery userCredentialsQuery;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final OperationalMetrics metrics;

    AuthApplicationService(
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            UserRegistrationFacade userRegistrationFacade,
            UserCredentialsQuery userCredentialsQuery,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties,
            OperationalMetrics metrics
    ) {
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRegistrationFacade = userRegistrationFacade;
        this.userCredentialsQuery = userCredentialsQuery;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
        this.metrics = metrics;
    }

    @Override
    public AuthTokens register(RegisterCommand command) {
        log.info("Registration requested: number={}", maskPhoneNumber(command.phoneNumber()));
        var normalizedPhoneNumber = normalizeE164PhoneNumber(command.phoneNumber());
        var passwordHash = passwordEncoder.encode(command.password());

        var createdUser = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        passwordHash,
                        command.displayName(),
                        normalizedPhoneNumber,
                        command.timezone(),
                        command.clientIp()
                )
        );

        var accessToken = jwtTokenService.issueAccessToken(
                createdUser.id(),
                createdUser.phoneNumber(),
                DEFAULT_ROLES
        );
        var refreshToken = refreshTokenService.issue(createdUser.id());

        log.info("Registration succeeded: userId={}", createdUser.id());

        return new AuthTokens(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtProperties.accessTokenMinutes() * 60
        );
    }

    @Override
    public AuthTokens login(LoginCommand command) {
        log.info("Login requested: number={}", maskPhoneNumber(command.phoneNumber()));

        var normalizedPhoneNumber = normalizeE164PhoneNumber(command.phoneNumber());

        var user = userCredentialsQuery.findByPhoneNumber(normalizedPhoneNumber)
                .orElseThrow(() -> {
                    metrics.authFailure("unknown_phone_number");
                    log.warn("Login rejected: unknown number={}", maskPhoneNumber(command.phoneNumber()));
                    return new BadCredentialsException("Bad Credentials");
                });

        if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
            metrics.authFailure("bad_password");
            log.warn("Login rejected: bad credentials for number={}", maskPhoneNumber(command.phoneNumber()));
            throw new BadCredentialsException("Bad credentials");
        }

        String accessToken = jwtTokenService.issueAccessToken(
                user.id(),
                user.phoneNumber(),
                DEFAULT_ROLES
        );

        String refreshToken = refreshTokenService.issue(user.id());
        log.info("Login succeeded: userId={}", user.id());

        return new AuthTokens(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtProperties.accessTokenMinutes() * 60
        );
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        log.info("Refresh token requested");

        RefreshTokenService.RotateToken rotated = refreshTokenService.rotate(refreshToken);

        var user = userCredentialsQuery.findById(rotated.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String accessToken = jwtTokenService.issueAccessToken(
                user.id(),
                user.phoneNumber(),
                DEFAULT_ROLES
        );

        log.info("Refresh token rotated: userId={}", user.id());
        return new AuthTokens(
                accessToken,
                rotated.rawToken(),
                TOKEN_TYPE,
                jwtProperties.accessTokenMinutes() * 60
        );
    }

    @Override
    public void logout(Long userId) {
        log.info("Logout - Revoke all token for userId={} requested", userId);

        var user = userCredentialsQuery.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Impossible to revoke tokens: user does not exist."));

        refreshTokenService.revokeAllForUser(user.id());
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "na";
        }

        var trimmed = phoneNumber.trim();
        if (trimmed.length() <= 4) {
            return "***";
        }

        return "***" + trimmed.substring(trimmed.length() - 4);
    }
}
