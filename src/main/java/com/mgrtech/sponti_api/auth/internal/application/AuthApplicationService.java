package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.auth.internal.application.view.AuthTokens;
import com.mgrtech.sponti_api.auth.internal.application.command.*;
import com.mgrtech.sponti_api.auth.internal.security.JwtProperties;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.shared.error.BadCredentialsException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.shared.error.VerificationNotFoundException;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import com.mgrtech.sponti_api.sms.api.OtpFacade;
import com.mgrtech.sponti_api.sms.api.OtpPurpose;
import com.mgrtech.sponti_api.sms.api.command.SendOtpCommand;
import com.mgrtech.sponti_api.sms.api.command.VerifyOtpCommand;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;
import com.mgrtech.sponti_api.user.api.UserPasswordFacade;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.UserVerificationFacade;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserPasswordCommand;
import com.mgrtech.sponti_api.user.api.command.VerifyUserPhoneCommand;
import com.mgrtech.sponti_api.user.api.deletion.UserDeletionFacade;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.user.api.query.UserCredentialsQuery;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
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
    private final UserDeletionFacade userDeletionFacade;

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

        if (!"ACTIVE".equals(user.status())) {
            metrics.authFailure("inactive_user");
            throw new BadCredentialsException("Bad credentials");
        }

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

        if (!"ACTIVE".equals(user.status())) {
            metrics.authFailure("inactive_user");
            throw new BadCredentialsException("Bad credentials");
        }

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

    @Override
    public VerificationView recoverPassword(RecoverPasswordCommand command) {
        var normalizedPhoneNumber = normalizeE164PhoneNumber(command.phoneNumber());
        var maskedPhoneNumber = maskPhoneNumber(normalizedPhoneNumber);
        log.info("Recovery requested: phoneNumber={}", maskedPhoneNumber);
        var user = userCredentialsQuery.findByPhoneNumber(normalizedPhoneNumber);

        if (user.isEmpty()) {
            log.info("Recovery requested for unknown phoneNumber={}", maskedPhoneNumber);
            return new VerificationView(null);
        }

        var verificationView = otpFacade.sendOtpCode(
                new SendOtpCommand(user.get().id(), user.get().phoneNumber(), command.clientIp()),
                OtpPurpose.PASSWORD_RECOVERY
        );
        log.info("Recovery password: OTP code send for phoneNumber={}", maskedPhoneNumber);
        return verificationView;
    }

    @Override
    public void verifyPasswordRecovery(VerifyRecoveryPasswordCommand command) {
        var phoneNumber = normalizeE164PhoneNumber(command.phoneNumber());
        var maskedPhoneNumber = maskPhoneNumber(phoneNumber);
        var verificationId = command.verificationId();
        var otpCode = command.otpCode();
        log.info("Verifying phoneNumber={}, verificationId={}", maskedPhoneNumber, verificationId);

        var user = userCredentialsQuery.findByPhoneNumber(phoneNumber)
                .orElseThrow(VerificationNotFoundException::new);

        var verificationResult = otpFacade.verifyOtpCode(new VerifyOtpCommand(
                user.id(),
                phoneNumber,
                verificationId,
                otpCode,
                OtpPurpose.PASSWORD_RECOVERY
        ));

        if(verificationResult.verified()) {
            userPasswordFacade.updatePassword(new UpdateUserPasswordCommand(
                    user.id(),
                    passwordEncoder.encode(command.newPassword())
            ));
            log.info("Password has been reset for phoneNumber={}, verificationId={}",
                    maskedPhoneNumber, verificationId);
            refreshTokenService.revokeAllForUser(user.id());
            return;
        }
        log.info("Password not reset for phoneNumber={}, verificationId={}. Token verified=false",
                maskedPhoneNumber, verificationId);
    }

    @Override
    public void deleteAuthenticatedUser(Long userId) {
        log.info("Delete authenticated user for userId={}", userId);
        refreshTokenService.revokeAllForUser(userId);
        userDeletionFacade.requestDeletion(userId);
        userDeletionFacade.markTaskCompleted(userId, UserDeletionModule.AUTH);
    }
}
