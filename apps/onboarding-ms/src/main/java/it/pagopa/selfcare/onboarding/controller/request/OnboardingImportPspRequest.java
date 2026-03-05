package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OnboardingImportPspRequest {

    @NotNull(message = "institutionData is required")
    @Valid
    private InstitutionPspRequest institution;

    private BillingRequest billing;

    @NotEmpty(message = "productId is required")
    private String productId;

    @NotNull
    private OnboardingImportContract contractImported;

}
