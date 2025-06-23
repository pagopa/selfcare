package it.pagopa.selfcare.auth.controller.response;

import it.pagopa.selfcare.auth.model.OtpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpForbidden {

    private OtpForbiddenCode otpForbiddenCode;
    private Integer remainingAttempts;
    private OtpStatus otpStatus;
}
