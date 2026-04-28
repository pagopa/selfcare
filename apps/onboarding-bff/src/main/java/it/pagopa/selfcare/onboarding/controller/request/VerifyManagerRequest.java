package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class VerifyManagerRequest {
    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}")
    @JsonProperty(required = true)
    @NotBlank
    private String companyTaxCode;
}
