package it.pagopa.selfcare.external_api.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.external_api.model.user.UserDto;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

;

@Data
public class OnboardingDto {

    @Schema(description = "${swagger.external_api.institutions.model.users}")
    @NotEmpty
    @Valid
    private List<UserDto> users;

    @Schema(description = "${swagger.external_api.institutions.model.billingData}")
    @NotNull
    @Valid
    private BillingDataDto billingData;

    @Schema(description = "${swagger.external_api.institutions.model.institutionType}")
    @NotNull
    private InstitutionType institutionType;

    @Schema(description = "${swagger.external_api.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.external_api.institutions.model.pricingPlan}")
    private String pricingPlan;

    @Schema(description = "${swagger.external_api.institutions.model.pspData}")
    @Valid
    private PspDataDto pspData;

    @Schema(description = "${swagger.external_api.institutions.model.geographicTaxonomy}")
    @NotNull
    @Valid
    private List<GeographicTaxonomyDto> geographicTaxonomies;

    @Schema(description = "${swagger.external_api.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsDto companyInformations;

    @Schema(description = "${swagger.external_api.institutions.model.assistance}")
    @Valid
    private AssistanceContactsDto assistanceContacts;
}
