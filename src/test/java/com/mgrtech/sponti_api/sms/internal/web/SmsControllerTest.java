package com.mgrtech.sponti_api.sms.internal.web;

import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.sms.api.OtpFacade;
import com.mgrtech.sponti_api.sms.api.command.VerifyOtpCommand;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SmsController.class)
@AutoConfigureMockMvc(addFilters = false)
class SmsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OtpFacade otpFacade;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @Test
    void resendPhoneVerificationDelegatesForAuthenticatedUser() throws Exception {
        given(otpFacade.resendPhoneVerification(42L, "203.0.113.10"))
                .willReturn(new VerificationView("verification-id"));

        mockMvc.perform(post("/api/v1/otp/resend-phone-verification")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").value("verification-id"));

        verify(otpFacade).resendPhoneVerification(42L, "203.0.113.10");
    }

    @Test
    void verifyOtpAllowsMissingVerificationId() throws Exception {
        given(otpFacade.verifyOtpCode(new VerifyOtpCommand(42L, null, "123456")))
                .willReturn(new VerificationResultView(42L, true));

        mockMvc.perform(post("/api/v1/otp/verify")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "otpCode": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        verify(otpFacade).verifyOtpCode(new VerifyOtpCommand(42L, null, "123456"));
    }
}
