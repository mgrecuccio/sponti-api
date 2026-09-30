package com.mgrtech.sponti_api.sms.api;

import com.mgrtech.sponti_api.sms.api.command.SendOtpCommand;
import com.mgrtech.sponti_api.sms.api.command.VerifyOtpCommand;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;

public interface OtpFacade {

    VerificationView sendOtpCode(SendOtpCommand command, OtpPurpose purpose);

    VerificationResultView verifyOtpCode(VerifyOtpCommand command);
}
