package it.pagopa.selfcare.onboarding.dto;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OnboardingAggregateOrchestratorInput {

    private String id;
    private String productId;
    private List<String> testEnvProductIds;
    private WorkflowType workflowType;
    private Institution institution;
    private List<User> users;
    private Institution aggregate;
    private String pricingPlan;
    private Billing billing;
    private Boolean signContract;
    private LocalDateTime expiringDate;
    private OnboardingStatus status;
    private String userRequestUid;
    private String workflowInstanceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime deletedAt;
    private String reasonForReject;
    private String referenceOnboardingId;

}