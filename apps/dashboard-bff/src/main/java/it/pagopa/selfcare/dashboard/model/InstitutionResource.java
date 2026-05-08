package it.pagopa.selfcare.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionResource {

    @Schema(description = "${swagger.dashboard.institutions.model.id}")
    private String id;

    @Schema(description = "${swagger.dashboard.institutions.model.externalId}")
    private String externalId;

    @Schema(description = "${swagger.dashboard.institutions.model.originId}")
    private String originId;

    @Schema(description = "${swagger.dashboard.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.dashboard.institutions.model.institutionType}")
    private String institutionType;

    @Schema(description = "${swagger.dashboard.institutions.model.name}")
    private String name;

    @Schema(description = "${swagger.dashboard.institutions.model.category}")
    private String category;

    @Schema(description = "${swagger.dashboard.institutions.model.categoryCode}")
    private String categoryCode;

    @Schema(description = "${swagger.dashboard.institutions.model.fiscalCode}")
    private String fiscalCode;

    @Schema(description = "${swagger.dashboard.institutions.model.mailAddress}")
    private String mailAddress;

    @Schema(description = "${swagger.dashboard.model.userRole}")
    private String userRole;

    @Schema(description = "${swagger.dashboard.institutions.model.status}")
    private String status;

    @Schema(description = "${swagger.dashboard.institutions.model.address}")
    private String address;

    @Schema(description = "${swagger.dashboard.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${swagger.dashboard.institutions.model.recipientCode}")
    private String recipientCode;

    @Schema(description = "${swagger.dashboard.institutions.model.geographicTaxonomy}")
    private List<GeographicTaxonomyResource> geographicTaxonomies;

    @Schema(description = "${swagger.dashboard.institutions.model.pspData.vatNumberGroup}")
    private Boolean vatNumberGroup;

    @Schema(description = "${swagger.dashboard.institutions.model.supportContact}")
    private SupportContactResource supportContact;

    @Schema(description = "${swagger.dashboard.institutions.model.vatNumber}")
    private String vatNumber;

    @Schema(description = "${swagger.dashboard.institutions.model.subunitCode}")
    private String subunitCode;

    @Schema(description = "${swagger.dashboard.institutions.model.subunitType}")
    private String subunitType;

    @Schema(description = "${swagger.dashboard.institutions.model.aooParentCode}")
    private String aooParentCode;

    @Schema(description = "${swagger.dashboard.institutions.model.parentDescription}")
    private String parentDescription;

    @Schema(description = "${swagger.dashboard.user.model.products}")
    private List<OnboardedProductResource> products;

    @Schema(description = "${swagger.dashboard.institutions.model.city}")
    private String city;

    @Schema(description = "${swagger.dashboard.institutions.model.country}")
    private String country;

    @Schema(description = "${swagger.dashboard.institutions.model.county}")
    private String county;

    @Schema(description = "${swagger.dashboard.institutions.model.delegation}")
    private boolean delegation;

}
