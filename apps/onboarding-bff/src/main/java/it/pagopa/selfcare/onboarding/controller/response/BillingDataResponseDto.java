package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BillingDataResponseDto {

    @Schema(description = "${openapi.onboarding.institutions.model.name}")
    private String businessName;

    @Schema(description = "${openapi.onboarding.institutions.model.address}")
    private String registeredOffice;

    @Schema(description = "${openapi.onboarding.institutions.model.digitalAddress}")
    private String digitalAddress;

    @Schema(description = "${openapi.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${openapi.onboarding.institutions.model.vatNumber}")
    private String vatNumber;

    @Schema(description = "${openapi.onboarding.institutions.model.recipientCode}")
    private String recipientCode;

    @Schema(description = "${openapi.onboarding.institutions.model.publicServices}")
    private Boolean publicServices;

    @Schema(description = "${openapi.onboarding.institutions.model.certified}")
    private boolean certified;

}
