package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OnboardingBaseRequest {

    @NotEmpty(message = "productId is required")
    private String productId;

    private List<UserRequest> users;

    private List<AggregateInstitutionRequest> aggregates;

    private Boolean isAggregator;

    private String pricingPlan;

    private Boolean signContract;

    private UserRequesterDto userRequester;

}
