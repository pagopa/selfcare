package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowAggregator;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.AGGREGATOR;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.BUILD_CONTRACT_ACTIVITY_NAME;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.CREATE_USERS_ACTIVITY;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingWorkflowString;

public record WorkflowExecutorIncrementRegistrationAggregator(ObjectMapper objectMapper, TaskOptions optionsRetry,
                                                              OnboardingMapper onboardingMapper) implements WorkflowExecutor {

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, getOnboardingWorkflowString(objectMapper, onboardingWorkflow), optionsRetry, String.class).await();
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingWithInstitutionIdString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        createInstitutionAndOnboardingAggregate(ctx, onboardingWorkflow.getOnboarding(), onboardingMapper);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowAggregator(onboarding, AGGREGATOR.name());
    }


}
