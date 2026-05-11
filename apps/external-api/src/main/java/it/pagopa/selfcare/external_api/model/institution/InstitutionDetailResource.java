package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class InstitutionDetailResource {

    @Schema(description = "${swagger.external_api.institutions.model.id}")
    private UUID id;

    @Schema(description = "${swagger.external_api.institutions.model.name}")
    private String description;

    @Schema(description = "${swagger.external_api.institutions.model.externalId}")
    private String externalId;

    @Schema(description = "${swagger.external_api.institutions.model.originId}")
    private String originId;

    @Schema(description = "${swagger.external_api.institutions.model.institutionType}")
    private InstitutionType institutionType;

    @Schema(description = "${swagger.external_api.institutions.model.digitalAddress}")
    private String digitalAddress;

    @Schema(description = "${swagger.external_api.institutions.model.address}")
    private String address;

    @Schema(description = "${swagger.external_api.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${swagger.external_api.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.external_api.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.external_api.institutions.model.geographicTaxonomy}")
    private List<GeographicTaxonomyResource> geographicTaxonomies;

    @Schema(description = "${swagger.external_api.institutions.model.rea}")
    private String rea;

    @Schema(description = "${swagger.external_api.institutions.model.shareCapital}")
    private String shareCapital;

    @Schema(description = "${swagger.external_api.institutions.model.businessRegisterPlace}")
    private String businessRegisterPlace;

    @Schema(description = "${swagger.external_api.institutions.model.supportEmail}")
    private String supportEmail;

    @Schema(description = "${swagger.external_api.institutions.model.supportPhone}")
    private String supportPhone;

    @Schema(description = "${swagger.external_api.institutions.model.imported}")
    private Boolean imported;

    @Schema(description = "${swagger.external_api.institutions.model.subUnitCode}")
    private String subunitCode;

    @Schema(description = "${swagger.external_api.institutions.model.subUnitType}")
    private String subunitType;

    @Schema(description = "${swagger.external_api.institutions.model.parentDescription}")
    private String parentDescription;

    @Schema(description = "${swagger.external_api.institutions.model.aooParentCode}")
    private String aooParentCode;

    @Schema(description = "${swagger.external_api.institutions.model.country}")
    private String country;

    @Schema(description = "${swagger.external_api.institutions.model.county}")
    private String county;

    @Schema(description = "${swagger.external_api.institutions.model.city}")
    private String city;
}
