package it.pagopa.selfcare.auth.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OtpVerifyRequest {

  @NotEmpty(message = "otpUuid is required")
  @NotNull(message = "otpUuid is required")
  private String otpUuid;

  @NotEmpty(message = "otp value is required")
  @NotNull(message = "otp value is required")
  private String otp;
}
