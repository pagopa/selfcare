package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CompanyBillingDataDto {

    @Schema(description = "${swagger.onboarding.institutions.model.name}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    private String businessName;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String taxCode;

    @Schema(description = "${swagger.onboarding.institutions.model.certified}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotNull
    private boolean certified;

    @Schema(description = "${swagger.onboarding.institutions.model.digitalAddress}")
    private String digitalAddress;

}
