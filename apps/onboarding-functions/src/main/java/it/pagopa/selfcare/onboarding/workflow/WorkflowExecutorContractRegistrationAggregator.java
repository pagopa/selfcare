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
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingWorkflowString;
import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

public class WorkflowExecutorContractRegistrationAggregator implements WorkflowExecutor {

    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;
    protected final OnboardingMapper onboardingMapper;

    public WorkflowExecutorContractRegistrationAggregator(ObjectMapper objectMapper, TaskOptions optionsRetry, OnboardingMapper onboardingMapper) {
        this.objectMapper = objectMapper;
        this.optionsRetry = optionsRetry;
        this.onboardingMapper = onboardingMapper;
    }

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper, onboardingWorkflow);
        ctx.callActivity(CREATE_AGGREGATES_CSV_ACTIVITY, onboardingWorkflowString, optionsRetry, String.class).await();
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, optionsRetry, String.class).await();
        ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, optionsRetry, String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_FOR_CONTRACT, onboardingWorkflowString, optionsRetry, String.class).await();
        sendMailForUserActivity(ctx, onboardingWorkflow, onboardingMapper);
        sendMailForUserRequesterActivity(ctx, onboardingWorkflow);
        return Optional.of(OnboardingStatus.PENDING);
    }

    @Override
    public Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        return Optional.empty();
    }

    @Override
    public Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingWithInstitutionIdString = createInstitutionAndOnboarding(ctx, onboardingWorkflow.getOnboarding());
        Onboarding onboarding = readOnboardingValue(objectMapper(), onboardingWithInstitutionIdString);
        createInstitutionAndOnboardingAggregate(ctx, onboarding, onboardingMapper);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, getOnboardingWorkflowString(objectMapper(), onboardingWorkflow), optionsRetry, String.class).await();
        return Optional.of(OnboardingStatus.COMPLETED);
    }

    @Override
    public OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding) {
        return new OnboardingWorkflowAggregator(onboarding, AGGREGATOR.name());
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
