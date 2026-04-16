package it.pagopa.selfcare.onboarding.controller.request;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingUserRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    private InstitutionType institutionType;

    private String subunitCode;

    @NotEmpty(message = "origin is required")
    private String origin;

    @NotEmpty(message = "originId is required")
    private String originId;

    private String taxCode;

    @NotEmpty(message = "at least one user is required")
    private List<UserRequest> users;

    private Boolean toAddOnAggregates;
}
