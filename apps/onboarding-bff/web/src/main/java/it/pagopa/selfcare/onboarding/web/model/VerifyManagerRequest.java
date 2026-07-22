package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class VerifyManagerRequest {
    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}")
    @JsonProperty(required = true)
    @NotBlank
    private String companyTaxCode;
}
