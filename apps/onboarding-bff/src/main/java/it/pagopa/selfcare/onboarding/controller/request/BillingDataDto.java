package it.pagopa.selfcare.onboarding.controller.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class BillingDataDto {

    @Schema(description = "${openapi.onboarding.institutions.model.name}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String businessName;

    @Schema(description = "${openapi.onboarding.institutions.model.address}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String registeredOffice;

    @Schema(description = "${openapi.onboarding.institutions.model.digitalAddress}", required = true)
    @JsonProperty(required = true)
    @NotBlank
    private String digitalAddress;

    @Schema(description = "${openapi.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCodeInvoicing}")
    private String taxCodeInvoicing;

    @Schema(description = "${openapi.onboarding.institutions.model.vatNumber}")
    private String vatNumber;

    @Schema(description = "${openapi.onboarding.institutions.model.recipientCode}")
    private String recipientCode;

    @Schema(description = "${openapi.onboarding.institutions.model.publicServices}")
    private Boolean publicServices;

    @Schema(description = "${openapi.onboarding.institutions.model.certified}")
    private boolean certified;

    @Schema(description = "${openapi.onboarding.institutions.model.legalForm}")
    private String legalForm;
}
