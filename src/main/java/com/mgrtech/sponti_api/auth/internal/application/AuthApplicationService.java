package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.AuthTokens;
import com.mgrtech.sponti_api.auth.api.command.ChangePasswordCommand;
import com.mgrtech.sponti_api.auth.api.command.LoginCommand;
import com.mgrtech.sponti_api.auth.api.command.RegisterCommand;
import com.mgrtech.sponti_api.auth.api.command.VerifyRegistrationPhoneCommand;
import com.mgrtech.sponti_api.auth.internal.security.JwtProperties;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.shared.error.BadCredentialsException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import com.mgrtech.sponti_api.sms.api.OtpFacade;
import com.mgrtech.sponti_api.sms.api.OtpPurpose;
import com.mgrtech.sponti_api.sms.api.command.VerifyOtpCommand;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.user.api.UserPasswordFacade;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.UserVerificationFacade;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserPasswordCommand;
import com.mgrtech.sponti_api.user.api.command.VerifyUserPhoneCommand;
import com.mgrtech.sponti_api.user.api.query.UserCredentialsQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.maskPhoneNumber;
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
    private final UserPasswordFacade userPasswordFacade;
    private final UserVerificationFacade userVerificationFacade;
    private final UserCredentialsQuery userCredentialsQuery;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final OperationalMetrics metrics;
    private final OtpFacade otpFacade;

    AuthApplicationService(
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            UserRegistrationFacade userRegistrationFacade,
            UserPasswordFacade userPasswordFacade,
            UserVerificationFacade userVerificationFacade,
            UserCredentialsQuery userCredentialsQuery,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties,
            OperationalMetrics metrics,
            OtpFacade otpFacade
    ) {
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.userRegistrationFacade = userRegistrationFacade;
        this.userPasswordFacade = userPasswordFacade;
        this.userVerificationFacade = userVerificationFacade;
        this.userCredentialsQuery = userCredentialsQuery;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
        this.metrics = metrics;
        this.otpFacade = otpFacade;
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
    public void changePassword(ChangePasswordCommand command) {
        log.info("Password change requested: userId={}", command.userId());

        var user = userCredentialsQuery.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        if (!passwordEncoder.matches(command.currentPassword(), user.passwordHash())) {
            metrics.authFailure("bad_current_password");
            log.warn("Password change rejected: bad current password for userId={}", command.userId());
            throw new BadCredentialsException("Bad credentials");
        }

        userPasswordFacade.updatePassword(new UpdateUserPasswordCommand(
                user.id(),
                passwordEncoder.encode(command.newPassword())
        ));
        refreshTokenService.revokeAllForUser(user.id());

        log.info("Password changed: userId={}", user.id());
    }

    @Override
    public void logout(Long userId) {
        log.info("Logout - Revoke all token for userId={} requested", userId);

        var user = userCredentialsQuery.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Impossible to revoke tokens: user does not exist."));

        refreshTokenService.revokeAllForUser(user.id());
    }

    @Override
    public VerificationResultView verifyRegistrationPhone(VerifyRegistrationPhoneCommand command) {
        var user = userCredentialsQuery.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        var result = otpFacade.verifyOtpCode(new VerifyOtpCommand(
                user.id(),
                user.phoneNumber(),
                command.verificationId(),
                command.otpCode(),
                OtpPurpose.REGISTRATION
        ));

        userVerificationFacade.verify(new VerifyUserPhoneCommand(user.id()));
        return result;
    }
}
