package it.pagopa.selfcare.onboarding.controller.request;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingUserPgRequest {
    @NotEmpty(message = "productId is required")
    private String productId;
    private InstitutionType institutionType = InstitutionType.PG;
    @NotEmpty(message = "at least one user is required")
    private List<UserRequest> users;
    @NotNull(message = "taxCode is required")
    private String taxCode;
    @NotNull
    private Origin origin;
}
