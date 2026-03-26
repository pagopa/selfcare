package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingDto {

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.users}", required = true)
    @NotEmpty
    @Valid
    private List<UserDto> users;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.billingData}", required = true)
    @NotNull
    @Valid
    private BillingDataDto billingData;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.institutionType}", required = true)
    @NotNull
    private InstitutionType institutionType;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.origin}")
    private String origin;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pricingPlan}")
    private String pricingPlan;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData}")
    @Valid
    private PspDataDto pspData;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.geographicTaxonomies}", required = true)
    @Valid
    private List<GeographicTaxonomyDto> geographicTaxonomies;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsDto companyInformations;

    @ApiModelProperty(value = "${swagger.onboarding.institutions.model.assistance}")
    @Valid
    private AssistanceContactsDto assistanceContacts;

}
