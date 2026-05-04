package it.pagopa.selfcare.user.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class SendEmailOtpDto {

    @NotEmpty(message = "otp is required")
    private String otp;

    @NotEmpty(message = "institutionalEmail is required")
    private String institutionalEmail;
}
