package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.AuthTokens;
import com.mgrtech.sponti_api.auth.api.LoginCommand;
import com.mgrtech.sponti_api.auth.api.RegisterCommand;
import com.mgrtech.sponti_api.auth.internal.security.JwtProperties;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.shared.error.BadCredentialsException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserCredentialsQuery;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.normalizeEmail;

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

    AuthApplicationService(
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            UserRegistrationFacade userRegistrationFacade,
            UserCredentialsQuery userCredentialsQuery,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties
    ) {
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRegistrationFacade = userRegistrationFacade;
        this.userCredentialsQuery = userCredentialsQuery;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public AuthTokens register(RegisterCommand command) {
        log.info("Registration requested: email={}", maskEmail(command.email()));
        var normalizedEmail = normalizeEmail(command.email());
        var passwordHash = passwordEncoder.encode(command.password());

        var createdUser = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        normalizedEmail,
                        passwordHash,
                        command.displayName(),
                        command.timezone()
                )
        );

        var accessToken = jwtTokenService.issueAccessToken(
                createdUser.id(),
                command.email(),
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
        log.info("Login requested: email={}", maskEmail(command.email()));

        var normalizedEmail = normalizeEmail(command.email());

        var user = userCredentialsQuery.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Bad Credentials"));

        if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
            log.warn("Login rejected: bad credentials for email={}", maskEmail(command.email()));
            throw new BadCredentialsException("Bad credentials");
        }

        String accessToken = jwtTokenService.issueAccessToken(
                user.id(),
                user.email(),
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
                user.email(),
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

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "na";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
