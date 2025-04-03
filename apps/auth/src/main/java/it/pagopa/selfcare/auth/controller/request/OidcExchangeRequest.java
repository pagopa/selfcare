package it.pagopa.selfcare.auth.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OidcExchangeRequest {
    @NotEmpty(message = "code is required")
    @NotNull(message = "code is required")
    public String code;

    @NotEmpty(message = "redirect_uri is required")
    @NotNull(message = "redirect_uri is required")
    public String redirectUri;
}
