package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowInstitution;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.INSTITUTION;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingWorkflowString;

public class WorkflowExecutorForApprove implements WorkflowExecutor {

    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;
    private final OnboardingMapper onboardingMapper;

    public WorkflowExecutorForApprove(ObjectMapper objectMapper, TaskOptions optionsRetry, OnboardingMapper onboardingMapper) {
        this.objectMapper = objectMapper;
        this.optionsRetry = optionsRetry;
        this.onboardingMapper = onboardingMapper;
    }

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        ctx.callActivity(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.TOBEVALIDATED);
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper, onboardingWorkflow);
        String onboardingString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, optionsRetry, String.class).await();
        ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY, onboardingWorkflowString, optionsRetry, String.class).await();
        ctx.callActivity(UPDATE_ONBOARDING_EXPIRING_DATE_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        sendMailForUserActivity(ctx, onboardingWorkflow, onboardingMapper);
        sendMailForUserRequesterActivity(ctx, onboardingWorkflow);
        return Optional.of(OnboardingStatus.PENDING);
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return onboardingCompletionActivity(ctx, onboardingWorkflow);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowInstitution(onboarding, INSTITUTION.name());
    }

    @Override
    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    @Override
    public TaskOptions optionsRetry() {
        return optionsRetry;
    }
}
