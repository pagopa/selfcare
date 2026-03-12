package it.pagopa.selfcare.onboarding.workflow;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;

import java.util.Optional;

public class WorkflowExecutorForApproveGpu extends WorkflowExecutorForApprove {

    public WorkflowExecutorForApproveGpu(ObjectMapper objectMapper, TaskOptions optionsRetry, OnboardingMapper onboardingMapper) {
        super(objectMapper, optionsRetry, onboardingMapper);
    }

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingString = getOnboardingString(super.objectMapper(), onboardingWorkflow.getOnboarding());
        ctx.callSubOrchestrator(BUILD_ATTACHMENTS_SAVE_TOKENS_ACTIVITY, onboardingString, String.class);
        ctx.callActivity(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY, onboardingString, super.optionsRetry(), String.class).await();
        return Optional.of(OnboardingStatus.TOBEVALIDATED);
    }

}
