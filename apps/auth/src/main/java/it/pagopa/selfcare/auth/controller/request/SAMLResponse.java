package it.pagopa.selfcare.auth.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SAMLResponse {
  @NotEmpty(message = "SAMLResponse is required")
  @NotNull(message = "SAMLResponse is required")
  private String SAMLResponse;
}
