package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BillingDataResponseDto {

    @Schema(description = "${swagger.onboarding.institutions.model.name}")
    private String businessName;

    @Schema(description = "${swagger.onboarding.institutions.model.address}")
    private String registeredOffice;

    @Schema(description = "${swagger.onboarding.institutions.model.digitalAddress}")
    private String digitalAddress;

    @Schema(description = "${swagger.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.onboarding.institutions.model.vatNumber}")
    private String vatNumber;

    @Schema(description = "${swagger.onboarding.institutions.model.recipientCode}")
    private String recipientCode;

    @Schema(description = "${swagger.onboarding.institutions.model.publicServices}")
    private Boolean publicServices;

    @Schema(description = "${swagger.onboarding.institutions.model.certified}")
    private boolean certified;

}
