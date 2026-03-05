package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class OnboardingSaRequest extends OnboardingBaseRequest {

    @NotNull(message = "institutionData is required")
    @Valid
    private InstitutionBaseRequest institution;
    @NotNull(message = "billing is required")
    @Valid
    private BillingSaRequest billing;

}
