package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.client.model.GPUData;
import it.pagopa.selfcare.onboarding.model.AggregateInstitution;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class OnboardingProductDto {

    @Schema(description = "${openapi.onboarding.institutions.model.users}", required = true)
    @NotEmpty
    private List<@Valid UserDto> users;

    @Schema(description = "${openapi.onboarding.institutions.model.billingData}")
    @Valid
    private BillingDataDto billingData;

    @Schema(description = "${openapi.onboarding.institution.model.locationData}")
    private InstitutionLocationDataDto institutionLocationData;

    @Schema(description = "${openapi.onboarding.institutions.model.institutionType}", required = true)
    @NotNull
    private InstitutionType institutionType;

    @Schema(description = "${openapi.onboarding.institutions.model.origin}")
    private String origin;

    @Schema(description = "${openapi.onboarding.institutions.model.originId}")
    private String originId;

    @Schema(description = "${openapi.onboarding.institutions.model.pricingPlan}")
    private String pricingPlan;

    @Schema(description = "${openapi.onboarding.institutions.model.pspData}")
    @Valid
    private PspDataDto pspData;

    @Schema(description = "${openapi.onboarding.institutions.model.geographicTaxonomies}")
    private List<@Valid GeographicTaxonomyDto> geographicTaxonomies;

    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations}")
    @Valid
    private CompanyInformationsDto companyInformations;

    @Schema(description = "${openapi.onboarding.institutions.model.assistance}")
    @Valid
    private AssistanceContactsDto assistanceContacts;

    @Schema(description = "${openapi.onboarding.product.model.id}", required = true)
    @NotNull
    private String productId;

    @Schema(description = "${openapi.onboarding.institutions.model.taxCode}")
    private String taxCode;

    @Schema(description = "${openapi.onboarding.institutions.model.subunitCode}")
    private String subunitCode;

    @Schema(description = "${openapi.onboarding.institutions.model.subunitType}")
    private String subunitType;

    @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations}")
    private AdditionalInformationsDto additionalInformations;

    @Schema(description = "${openapi.onboarding.institutions.model.isAggregator}")
    private Boolean isAggregator;

    @Schema(description = "${openapi.onboarding.institutions.model.aggregates}")
    private List<AggregateInstitution> aggregates;

    @Schema(description = "${openapi.onboarding.institutions.model.gpuData}")
    private GPUData gpuData;

    @Schema(description = "${openapi.onboarding.institutions.model.istatCode}")
    private String istatCode;

    @Schema(description = "${openapi.onboarding.institutions.model.atecoCodes}")
    private List<String> atecoCodes;

    @Schema(description = "${openapi.onboarding.institutions.model.payment}")
    private PaymentDto payment;

    @Schema(description = "${openapi.onboarding.institutions.model.requester}")
    private UserRequestDto userRequester;

}
