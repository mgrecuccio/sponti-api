package com.mgrtech.sponti_api.sms.api.view;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contact invitation response.")
public record VerificationView(
        @Schema(description = "Verification id.", example = "8ab9730e-9a72-4104-91e6-ff78ba320b68")
        String verificationId
) {
}
