package it.pagopa.selfcare.onboarding.controller.request;

import it.pagopa.selfcare.onboarding.entity.GPUData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OnboardingDefaultRequest extends OnboardingBaseRequest {

    @NotNull(message = "institutionData is required")
    @Valid
    private InstitutionBaseRequest institution;
    @Valid
    private BillingRequest billing;
    @Valid
    private AdditionalInformationsDto additionalInformations;
    @Valid
    private GPUData gpuData;
    @Valid
    private PaymentRequestDto payment;
}
