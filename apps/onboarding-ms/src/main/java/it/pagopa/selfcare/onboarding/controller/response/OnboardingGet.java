package it.pagopa.selfcare.onboarding.controller.response;

import it.pagopa.selfcare.onboarding.controller.request.AdditionalInformationsDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OnboardingGet {
    private String id;
    private String productId;
    private String workflowType;
    private InstitutionResponse institution;
    private List<UserResponse> users;
    private String pricingPlan;
    private BillingResponse billing;
    private PaymentResponse payment;
    private Boolean signContract;
    private AdditionalInformationsDto additionalInformations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiringDate;
    private LocalDateTime activatedAt;
    private String status;
    private UserRequesterResponse userRequester;
    private String reasonForReject;
    private List<String> attachments;
}
