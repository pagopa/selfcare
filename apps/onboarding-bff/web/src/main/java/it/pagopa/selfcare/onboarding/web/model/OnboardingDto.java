package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingDto {

    @Schema(description = "${swagger.onboarding.institutions.model.users}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Valid
    private List<UserDto> users;

    @Schema(description = "${swagger.onboarding.institutions.model.billingData}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Valid
    private BillingDataDto billingData;

    @Schema(description = "${swagger.onboarding.institutions.model.institutionType}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private InstitutionType institutionType;

    @Schema(description = "${swagger.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.onboarding.institutions.model.pricingPlan}")
    private String pricingPlan;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData}")
    @Valid
    private PspDataDto pspData;

    @Schema(description = "${swagger.onboarding.institutions.model.geographicTaxonomies}", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<GeographicTaxonomyDto> geographicTaxonomies;

    @Schema(description = "${swagger.onboarding.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsDto companyInformations;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance}")
    @Valid
    private AssistanceContactsDto assistanceContacts;

}
