package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import lombok.Data;

import java.util.UUID;

@Data
public class InstitutionResource {

    @Schema(description = "${swagger.onboarding.institutions.model.id}")
    private UUID id;

    @Schema(description = "${swagger.onboarding.institutions.model.name}")
    private String description;

    @Schema(description = "${swagger.onboarding.institutions.model.parentDescription}")
    private String parentDescription;

    @Schema(description = "${swagger.onboarding.institutions.model.externalId}")
    private String externalId;

    @Schema(description = "${swagger.onboarding.institutions.model.originId}")
    private String originId;

    @Schema(description = "${swagger.onboarding.institutions.model.institutionType}")
    private String institutionType;

    @Schema(description = "${swagger.onboarding.institutions.model.digitalAddress}")
    private String digitalAddress;

    @Schema(description = "${swagger.onboarding.institutions.model.address}")
    private String address;

    @Schema(description = "${swagger.onboarding.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${swagger.onboarding.institutions.model.city}")
    private String city;

    @Schema(description = "${swagger.onboarding.institutions.model.county}")
    private String county;

    @Schema(description = "${swagger.onboarding.institutions.model.country}")
    private String country;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.onboarding.institutions.model.userRole}")
    private SelfCareAuthority userRole;

}
