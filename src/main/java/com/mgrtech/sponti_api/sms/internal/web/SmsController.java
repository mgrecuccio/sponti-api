package com.mgrtech.sponti_api.sms.internal.web;

import com.mgrtech.sponti_api.sms.api.OtpFacade;
import com.mgrtech.sponti_api.sms.api.command.VerifyOtpCommand;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.mgrtech.sponti_api.shared.utils.AuthenticationUtils.extractUserId;
import static com.mgrtech.sponti_api.shared.utils.HttpRequestUtils.extractClientIp;

@RestController
@RequestMapping("/api/v1/otp")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "SMS", description = "SMS endpoints")
@AllArgsConstructor
public class SmsController {

    private final OtpFacade smsFacade;

    @PostMapping("/resend-phone-verification")
    @Operation(summary = "Resend phone verification OTP", description = "Send a new OTP code to the authenticated user's current phone number")
    public VerificationView resendPhoneVerification(Authentication authentication, HttpServletRequest request) {
        var userId = extractUserId(authentication);
        return smsFacade.resendPhoneVerification(userId, extractClientIp(request));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP", description = "Verify the OTP code")
    public VerificationResultView verifyOtp(
            Authentication authentication,
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        var userId = extractUserId(authentication);
        return smsFacade.verifyOtpCode(
                new VerifyOtpCommand(
                        userId,
                        request.verificationId(),
                        request.otpCode()
                )
        );
    }

    @Schema(description = "Verify OTP")
    public record VerifyOtpRequest(
            @Schema(example = "123456") @NotBlank String otpCode,
            @Schema(example = "7bf82f3b-8767-4695-8834-c001ce56facf", nullable = true) String verificationId

    ) {
    }
}
