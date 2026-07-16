package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingDto {

    @Schema(description = "${openapi.onboarding.institutions.model.users}", required = true)
    @NotEmpty
    private List<@Valid UserDto> users;

    @Schema(description = "${openapi.onboarding.institutions.model.billingData}", required = true)
    @NotNull
    @Valid
    private BillingDataDto billingData;

    @Schema(description = "${openapi.onboarding.institutions.model.institutionType}", required = true)
    @NotNull
    private InstitutionType institutionType;

    @Schema(description = "${openapi.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${openapi.onboarding.institutions.model.pricingPlan}")
    private String pricingPlan;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData}")
    @Valid
    private PspDataDto pspData;

    @Schema(description = "${openapi.onboarding.institutions.model.geographicTaxonomies}", required = true)
    private List<@Valid GeographicTaxonomyDto> geographicTaxonomies;

    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsDto companyInformations;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance}")
    @Valid
    private AssistanceContactsDto assistanceContacts;

}
