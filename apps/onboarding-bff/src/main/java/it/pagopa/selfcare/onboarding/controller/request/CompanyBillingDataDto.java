package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CompanyBillingDataDto {

    @Schema(description = "${openapi.onboarding.institutions.model.name}", required = true)
    @JsonProperty(required = true)
    private String businessName;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${openapi.onboarding.institutions.model.certified}", required = true)
    @JsonProperty(required = true)
    @NotNull
    private boolean certified;

    @Schema(description = "${openapi.onboarding.institutions.model.digitalAddress}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String digitalAddress;

}
