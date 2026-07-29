package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class BillingDataDto {

    @Schema(description = "${swagger.onboarding.institutions.model.name}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String businessName;

    @Schema(description = "${swagger.onboarding.institutions.model.address}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String registeredOffice;

    @Schema(description = "${swagger.onboarding.institutions.model.digitalAddress}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    @NotBlank
    private String digitalAddress;

    @Schema(description = "${swagger.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCodeInvoicing}")
    private String taxCodeInvoicing;

    @Schema(description = "${swagger.onboarding.institutions.model.vatNumber}")
    private String vatNumber;

    @Schema(description = "${swagger.onboarding.institutions.model.recipientCode}")
    private String recipientCode;

    @Schema(description = "${swagger.onboarding.institutions.model.publicServices}")
    private Boolean publicServices;

    @Schema(description = "${swagger.onboarding.institutions.model.certified}")
    private boolean certified;

    @Schema(description = "${swagger.onboarding.institutions.model.legalForm}")
    private String legalForm;
}
