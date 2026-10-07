package it.pagopa.selfcare.onboarding.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.model.UserAuthority;
import lombok.Data;

import java.util.UUID;

@Data
public class InstitutionResource {

    @Schema(description = "${openapi.onboarding.institutions.model.id}")
    private UUID id;

    @Schema(description = "${openapi.onboarding.institutions.model.name}")
    private String description;

    @Schema(description = "${openapi.onboarding.institutions.model.parentDescription}")
    private String parentDescription;

    @Schema(description = "${openapi.onboarding.institutions.model.externalId}")
    private String externalId;

    @Schema(description = "${openapi.onboarding.institutions.model.originId}")
    private String originId;

    @Schema(description = "${openapi.onboarding.institutions.model.institutionType}")
    private String institutionType;

    @Schema(description = "${openapi.onboarding.institutions.model.digitalAddress}")
    private String digitalAddress;

    @Schema(description = "${openapi.onboarding.institutions.model.address}")
    private String address;

    @Schema(description = "${openapi.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${openapi.onboarding.institutions.model.city}")
    private String city;

    @Schema(description = "${openapi.onboarding.institutions.model.county}")
    private String county;

    @Schema(description = "${openapi.onboarding.institutions.model.country}")
    private String country;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${openapi.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${openapi.onboarding.institutions.model.userRole}")
    private UserAuthority userRole;

}
