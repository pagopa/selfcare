package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class InstitutionLegalAddressResource {

    @Schema(description = "${swagger.onboarding.institutions.model.address}")
    private String address;

    @Schema(description = "${swagger.onboarding.institutions.model.zipCode}")
    private String zipCode;

}
