package it.pagopa.selfcare.onboarding.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "${openapi.onboarding.institutions.model.billingData}")
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

    @Schema(description = "${openapi.onboarding.institutions.model.digitalAddress}")
    private String digitalAddress;

}
