package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GPUData;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingProductDto {

    @Schema(description = "${swagger.onboarding.institutions.model.users}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    @Valid
    private List<UserDto> users;

    @Schema(description = "${swagger.onboarding.institutions.model.billingData}")
    @Valid
    private BillingDataDto billingData;

    @Schema(description = "${swagger.onboarding.institution.model.locationData}")
    private InstitutionLocationDataDto institutionLocationData;

    @Schema(description = "${swagger.onboarding.institutions.model.institutionType}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private InstitutionType institutionType;

    @Schema(description = "${swagger.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${swagger.onboarding.institutions.model.originId}")
    private String originId;

    @Schema(description = "${swagger.onboarding.institutions.model.pricingPlan}")
    private String pricingPlan;

    @Schema(description = "${swagger.onboarding.institutions.model.pspData}")
    @Valid
    private PspDataDto pspData;

    @Schema(description = "${swagger.onboarding.institutions.model.geographicTaxonomies}")
    @Valid
    private List<GeographicTaxonomyDto> geographicTaxonomies;

    @Schema(description = "${swagger.onboarding.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsDto companyInformations;

    @Schema(description = "${swagger.onboarding.institutions.model.assistance}")
    @Valid
    private AssistanceContactsDto assistanceContacts;

    @Schema(description = "${swagger.onboarding.product.model.id}", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String productId;

    @Schema(description = "${swagger.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${swagger.onboarding.institutions.model.subunitCode}")
    private String subunitCode;

    @Schema(description = "${swagger.onboarding.institutions.model.subunitType}")
    private String subunitType;

    @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations}")
    private AdditionalInformationsDto additionalInformations;

    @Schema(description = "${swagger.onboarding.institutions.model.isAggregator}")
    private Boolean isAggregator;

    @Schema(description = "${swagger.onboarding.institutions.model.aggregates}")
    private List<AggregateInstitution> aggregates;

    @Schema(description = "${swagger.onboarding.institutions.model.gpuData}")
    private GPUData gpuData;

    @Schema(description = "${swagger.onboarding.institutions.model.istatCode}")
    private String istatCode;

    @Schema(description = "${swagger.onboarding.institutions.model.atecoCodes}")
    private List<String> atecoCodes;

    @Schema(description = "${swagger.onboarding.institutions.model.payment}")
    private PaymentDto payment;

    private UserRequestDto userRequester;

}
