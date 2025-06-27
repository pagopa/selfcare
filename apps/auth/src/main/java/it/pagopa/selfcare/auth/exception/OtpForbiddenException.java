package it.pagopa.selfcare.auth.exception;

import it.pagopa.selfcare.auth.controller.response.OtpForbiddenCode;
import it.pagopa.selfcare.auth.model.OtpStatus;
import lombok.Getter;

@Getter
public class OtpForbiddenException extends  RuntimeException{
    private final OtpForbiddenCode code;
    private final Integer remainingAttempts;
    private final OtpStatus otpStatus;

    public OtpForbiddenException(String message, OtpForbiddenCode code, Integer remainingAttempts, OtpStatus otpStatus) {
        super(message);
        this.code = code;
        this.remainingAttempts = remainingAttempts;
        this.otpStatus = otpStatus;
    }

}
