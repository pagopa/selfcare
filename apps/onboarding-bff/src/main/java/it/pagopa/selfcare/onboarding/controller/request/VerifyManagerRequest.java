package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class VerifyManagerRequest {
    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}")
    @JsonProperty(required = true)
    @NotBlank
    private String companyTaxCode;
}
