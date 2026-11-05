package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowInstitution;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.INSTITUTION;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingWorkflowString;

/**
 * Update of the contract of an existing adhesion (referenced by {@code referenceOnboardingId}):
 * institution and onboarding already exist, so only users, document and adhesion flag are aligned.
 */
public record WorkflowExecutorContractUpdate(ObjectMapper objectMapper, TaskOptions optionsRetry) implements WorkflowExecutor {

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return onboardingUsersRequestActivity(ctx, onboardingWorkflow);
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper, onboardingWorkflow);
        ctx.callActivity(SYNC_PRODUCT_USERS_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(STORE_ONBOARDING_ACTIVATEDAT, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(ACTIVATE_CONTRACT_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingWorkflowString, optionsRetry, String.class).await();
        ctx.callActivity(OVERRIDE_PENDING_ONBOARDINGS, onboardingString, optionsRetry, String.class).await();
        ctx.callActivity(CLEAR_CONTRACT_UPDATE_REQUIRED_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowInstitution(onboarding, INSTITUTION.name());
    }
}
