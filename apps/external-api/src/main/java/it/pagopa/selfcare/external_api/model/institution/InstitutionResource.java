package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.UUID;

@Data
public class InstitutionResource {

    @Schema(description = "${swagger.external_api.institutions.model.id}")
    @NotNull
    private UUID id;

    @Schema(description = "${swagger.external_api.institutions.model.name}")
    private String description;

    @Schema(description = "${swagger.external_api.institutions.model.externalId}")
    private String externalId;

    @Schema(description = "${swagger.external_api.institutions.model.originId}")
    @NotBlank
    private String originId;

    @Schema(description = "${swagger.external_api.institutions.model.institutionType}")
    private InstitutionType institutionType;

    @Schema(description = "${swagger.external_api.institutions.model.digitalAddress}")
    private String digitalAddress;

    @Schema(description = "${swagger.external_api.institutions.model.status}")
    private String status;

    @Schema(description = "${swagger.external_api.institutions.model.address}")
    private String address;

    @Schema(description = "${swagger.external_api.institutions.model.zipCode}")
    private String zipCode;

    @Schema(description = "${swagger.external_api.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.external_api.institutions.model.taxCodeInvoicing}")
    private String taxCodeInvoicing;

    @Schema(description = "${swagger.external_api.institutions.model.origin}")
    @NotBlank
    private String origin;

    @Schema(description = "${swagger.external_api.institutions.model.productRoles}")
    private Collection<String> userProductRoles;

    @Schema(description = "${swagger.external_api.institutions.model.recipientCode}")
    private String recipientCode;

    @Schema(description = "${swagger.external_api.institutions.model.companyInformations}")
    private CompanyInformationsResource companyInformations;

    @Schema(description = "${swagger.external_api.institutions.model.assistance}")
    private AssistanceContactsResource assistanceContacts;

    @Schema(description = "${swagger.external_api.institutions.model.pspData}")
    private PspDataResource pspData;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.dpoData}")
    private DpoDataResource dpoData;

    @Schema(description = "${swagger.external_api.institutions.model.subUnitCode}")
    private String subunitCode;

    @Schema(description = "${swagger.external_api.institutions.model.subUnitType}")
    private String subunitType;

    @Schema(description = "${swagger.external_api.institutions.model.rootParent}")
    private RootParentResource rootParent;

    @Schema(description = "${swagger.external_api.institutions.model.aooParentCode}")
    private String aooParentCode;

    @Schema(description = "${swagger.external_api.institutions.model.country}")
    private String country;

    @Schema(description = "${swagger.external_api.institutions.model.county}")
    private String county;

    @Schema(description = "${swagger.external_api.institutions.model.city}")
    private String city;

}
