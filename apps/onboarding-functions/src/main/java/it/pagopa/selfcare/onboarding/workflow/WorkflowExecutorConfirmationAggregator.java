package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingWorkflowString;

public class WorkflowExecutorConfirmationAggregator extends WorkflowExecutorContractRegistrationAggregator {

    public WorkflowExecutorConfirmationAggregator(ObjectMapper objectMapper, TaskOptions optionsRetry, OnboardingMapper onboardingMapper) {
        super(objectMapper, optionsRetry, onboardingMapper);
    }

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingWorkflowString = getOnboardingWorkflowString(super.objectMapper(), onboardingWorkflow);
        ctx.callActivity(CREATE_AGGREGATES_CSV_ACTIVITY, onboardingWorkflowString, super.optionsRetry(), String.class).await();
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, super.optionsRetry(), String.class).await();
        ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, super.optionsRetry(), String.class).await();
        return Optional.of(OnboardingStatus.PENDING);
    }

}
