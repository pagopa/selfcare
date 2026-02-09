package it.pagopa.selfcare.auth.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OtpResendRequest {

  @NotEmpty(message = "otpUuid is required")
  @NotNull(message = "otpUuid is required")
  private String otpUuid;
}
