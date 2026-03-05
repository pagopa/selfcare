package it.pagopa.selfcare.onboarding.controller.response;

import it.pagopa.selfcare.onboarding.controller.request.AdditionalInformationsDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class OnboardingResponse {

    private String id;
    private String productId;
    private String workflowType;
    private InstitutionResponse institution;
    private String pricingPlan;
    private List<UserOnboardingResponse> users;
    private BillingResponse billing;
    private String status;
    private AdditionalInformationsDto additionalInformations;
    private Boolean isAggregator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String referenceOnboardingId;
    private UserRequesterResponse userRequester;

}
