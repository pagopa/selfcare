package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.onboarding.controller.request.OnboardingDefaultRequest;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OnboardingAggregationImportRequest extends OnboardingDefaultRequest {

    @Valid
    private OnboardingImportContract onboardingImportContract;
}

