package it.pagopa.selfcare.onboarding.controller.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
public class InstitutionLegalAddressResource {

    @Schema(description = "${openapi.onboarding.institutions.model.address}")
    private String address;

    @Schema(description = "${openapi.onboarding.institutions.model.zipCode}")
    private String zipCode;

}
