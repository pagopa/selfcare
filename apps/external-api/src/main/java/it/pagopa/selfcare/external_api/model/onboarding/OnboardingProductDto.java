package it.pagopa.selfcare.external_api.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.external_api.model.institution.GPUData;
import it.pagopa.selfcare.external_api.model.institution.InstitutionLocationDataDto;
import it.pagopa.selfcare.external_api.model.user.UserDto;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingProductDto {

    @Schema(description = "${swagger.external_api.institutions.model.users}")
    @NotEmpty
    @Valid
    private List<UserDto> users;

    @Schema(description = "${swagger.external_api.institutions.model.billingData}")
    @Valid
    private BillingDataDto billingData;

    @Schema(description = "${swagger.external_api.institutions.model.locationData}")
    private InstitutionLocationDataDto institutionLocationData;

    @Schema(description = "${swagger.external_api.institutions.model.institutionType}")
    @NotNull
    private InstitutionType institutionType;

    @Schema(description = "${swagger.external_api.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.external_api.institutions.model.originId}")
    private String originId;

    @Schema(description = "${swagger.external_api.institutions.model.pricingPlan}")
    private String pricingPlan;

    @Schema(description = "${swagger.external_api.institutions.model.pspData}")
    @Valid
    private PspDataDto pspData;

    @Schema(description = "${swagger.external_api.institutions.model.gpuData}")
    @Valid
    private GPUData gpuData;

    @Schema(description = "${swagger.external_api.institutions.model.geographicTaxonomies}")
    @NotNull
    @Valid
    private List<GeographicTaxonomyDto> geographicTaxonomies;

    @Schema(description = "${swagger.external_api.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsDto companyInformations;

    @Schema(description = "${swagger.external_api.institutions.model.assistance}")
    @Valid
    private AssistanceContactsDto assistanceContacts;

    @Schema(description = "${swagger.external_api.product.model.id}")
    @NotNull
    private String productId;

    @Schema(description = "${swagger.external_api.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.external_api.institutions.model.subUnitCode}")
    private String subunitCode;

    @Schema(description = "${swagger.external_api.institutions.model.subUnitType}")
    private String subunitType;

    @Schema(description = "${swagger.external_api.institutions.model.additionalInformations}")
    private AdditionalInformationsDto additionalInformations;

    @Schema(description = "${swagger.external_api.institutions.model.isAggregator}", example = "false")
    private Boolean isAggregator;

    @Schema(description = "${swagger.external_api.institutions.model.istatCode}")
    private String istatCode;

    @Schema(description = "${swagger.external_api.institutions.model.aggregates}")
    @Valid
    private List<AggregateInstitutionDto> aggregates;
}
