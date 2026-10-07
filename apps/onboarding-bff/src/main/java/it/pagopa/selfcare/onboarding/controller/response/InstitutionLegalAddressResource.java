package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InstitutionLegalAddressResource {

    @Schema(description = "${openapi.onboarding.institutions.model.address}")
    private String address;

    @Schema(description = "${openapi.onboarding.institutions.model.zipCode}")
    private String zipCode;

}
