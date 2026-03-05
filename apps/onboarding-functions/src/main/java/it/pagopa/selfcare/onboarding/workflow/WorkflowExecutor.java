package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.AggregatesBatchOrchestratorInput;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import org.openapi.quarkus.core_json.model.DelegationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.COMPLETED;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.*;

public interface WorkflowExecutor {

    Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow);

    Optional<OnboardingStatus> executeToBeValidatedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow);

    Optional<OnboardingStatus> executePendingState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow);

    OnboardingWorkflow createOnboardingWorkflow(Onboarding onboarding);

    ObjectMapper objectMapper();

    TaskOptions optionsRetry();

    default Optional<OnboardingStatus> execute(TaskOrchestrationContext ctx, Onboarding onboarding) {
        OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflow(onboarding);
        return switch (onboarding.getStatus()) {
            case REQUEST -> executeRequestState(ctx, onboardingWorkflow);
            case TOBEVALIDATED -> executeToBeValidatedState(ctx, onboardingWorkflow);
            case PENDING -> executePendingState(ctx, onboardingWorkflow);
            case REJECTED -> executeRejectedState(ctx, onboardingWorkflow);
            default -> Optional.empty();
        };

    }

    default String createInstitutionAndOnboarding(TaskOrchestrationContext ctx, Onboarding onboarding) {
        final String onboardingString = getOnboardingString(objectMapper(), onboarding);

        //CreateInstitution activity return an institutionId that is used by CreateOnboarding activity
        String institutionId = ctx.callActivity(CREATE_INSTITUTION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        onboarding.getInstitution().setId(institutionId);
        final String onboardingWithInstitutionIdString = getOnboardingString(objectMapper(), onboarding);

        ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        ctx.callActivity(STORE_ONBOARDING_ACTIVATEDAT, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();

        /* TODO
        ctx.callActivity(REJECT_OUTDATED_ONBOARDINGS, onboardingString, optionsRetry(), String.class).await();
        */

        return onboardingWithInstitutionIdString;
    }

    default void createTestEnvironmentsOnboarding(TaskOrchestrationContext ctx, Onboarding onboarding, String onboardingWithInstitutionIdString) {
        // Create onboarding for test environments if exists (ex. prod-interop-coll)
        if (Objects.nonNull(onboarding.getTestEnvProductIds()) && !onboarding.getTestEnvProductIds().isEmpty()) {
            // Schedule each task to run in parallel
            List<Task<String>> parallelTasks = new ArrayList<>();
            onboarding.getTestEnvProductIds()
                    .forEach(testEnvProductId -> {
                        final String onboardingStringWithTestEnvProductId = onboardingStringWithTestEnvProductId(testEnvProductId, onboardingWithInstitutionIdString);
                        parallelTasks.add(ctx.callActivity(CREATE_ONBOARDING_ACTIVITY, onboardingStringWithTestEnvProductId, optionsRetry(), String.class));
                        parallelTasks.add(ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingStringWithTestEnvProductId, optionsRetry(), String.class));
                    });

            // Wait for all tasks to complete
            ctx.allOf(parallelTasks).await();
        }
    }
    default Optional<OnboardingStatus> onboardingUsersRequestActivity(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper(), onboardingWorkflow);
        ctx.callActivity(BUILD_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, optionsRetry(), String.class).await();
        ctx.callActivity(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME, onboardingWorkflowString, optionsRetry(), String.class).await();
        ctx.callActivity(SEND_MAIL_REGISTRATION_FOR_CONTRACT, onboardingWorkflowString, optionsRetry(), String.class).await();
        return Optional.of(OnboardingStatus.PENDING);
    }

    default Optional<OnboardingStatus> onboardingCompletionActivity(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        String onboardingWithInstitutionIdString = createInstitutionAndOnboarding(ctx, onboarding);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        createTestEnvironmentsOnboarding(ctx, onboarding, onboardingWithInstitutionIdString);
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, getOnboardingWorkflowString(objectMapper(), onboardingWorkflow), optionsRetry(), String.class).await();
        return Optional.of(COMPLETED);
    }

    default Optional<OnboardingStatus> onboardingCompletionUsersActivity(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        final String onboardingString = getOnboardingString(objectMapper(), onboardingWorkflow.getOnboarding());
        final String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper(), onboardingWorkflow);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        ctx.callActivity(STORE_ONBOARDING_ACTIVATEDAT, onboardingString, optionsRetry(), String.class).await();
        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingWorkflowString, optionsRetry(), String.class).await();
        return Optional.of(COMPLETED);
    }

    default Optional<OnboardingStatus> onboardingCompletionUsersEaActivity(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow, OnboardingMapper onboardingMapper) {
        final String onboardingString = getOnboardingString(objectMapper(), onboardingWorkflow.getOnboarding());
        final String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper(), onboardingWorkflow);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingString, optionsRetry(), String.class).await();

        String delegationResponseString = ctx.callActivity(RETRIEVE_AGGREGATES_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        List<DelegationResponse> delegationResponseList = readDelegationResponseList(objectMapper(), delegationResponseString);

        List<Task<String>> parallelTasks = new ArrayList<>();

        for (DelegationResponse delegation : delegationResponseList) {
            Onboarding onboardingAggregate = onboardingMapper.mapToOnboardingFromDelegation(onboardingWorkflow.getOnboarding(), delegation);
            final String onboardingAggregateString = getOnboardingString(objectMapper(), onboardingAggregate);
            parallelTasks.add(ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingAggregateString, optionsRetry(), String.class));
        }

        ctx.allOf(parallelTasks).await();

        ctx.callActivity(STORE_ONBOARDING_ACTIVATEDAT, onboardingString, optionsRetry(), String.class).await();

        ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, onboardingWorkflowString, optionsRetry(), String.class).await();
        return Optional.of(COMPLETED);
    }

    default void createInstitutionAndOnboardingAggregate(TaskOrchestrationContext ctx, Onboarding onboarding, OnboardingMapper onboardingMapper){
        // Prepare all serialized inputs for each aggregate
        List<String> aggregateInputs = onboarding.getAggregates().stream()
                .map(aggregate -> {
                    OnboardingAggregateOrchestratorInput input = onboardingMapper.mapToOnboardingAggregateOrchestratorInput(onboarding, aggregate);
                    return getOnboardingAggregateString(objectMapper(), input);
                })
                .toList();

        // Call the sub-orchestration that handles batching
        // The batchSize is read from the configuration directly into the orchestrator
        AggregatesBatchOrchestratorInput batchInput = new AggregatesBatchOrchestratorInput(
                onboarding.getId(),
                aggregateInputs,
                0
        );

        String batchInputString = getAggregatesBatchInputString(objectMapper(), batchInput);
        ctx.callSubOrchestrator(ONBOARDINGS_AGGREGATE_BATCH_ORCHESTRATOR, batchInputString, String.class).await();
    }

    default Optional<OnboardingStatus> handleOnboardingCompletionActivityWithOptionalMail(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        String onboardingWithInstitutionIdString = createInstitutionAndOnboarding(ctx, onboarding);
        ctx.callActivity(CREATE_USERS_ACTIVITY, onboardingWithInstitutionIdString, optionsRetry(), String.class).await();
        if(Boolean.TRUE.equals(onboarding.getSendMailForImport())) {
            ctx.callActivity(SEND_MAIL_COMPLETION_ACTIVITY, getOnboardingWorkflowString(objectMapper(), onboardingWorkflow), optionsRetry(), String.class).await();
        }
        return Optional.of(COMPLETED);
    }

    private String onboardingStringWithTestEnvProductId(String testEnvProductId, String onboardingWithInstitutionIdString) {
        Onboarding onboardingTestEnv = readOnboardingValue(objectMapper(), onboardingWithInstitutionIdString);
        onboardingTestEnv.setProductId(testEnvProductId);
        return getOnboardingString(objectMapper(), onboardingTestEnv);
    }

    default Optional<OnboardingStatus> executeRejectedState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        String onboardingString = getOnboardingString(objectMapper(), onboarding);
        if (Objects.isNull(onboarding.getReasonForReject()) || !onboarding.getReasonForReject().equals("REJECTED_BY_USER")) {
            ctx.callActivity(SEND_MAIL_REJECTION_ACTIVITY, onboardingString, optionsRetry(), String.class).await();
        }
        return Optional.empty();
    }

    default void postProcessor(TaskOrchestrationContext ctx, Onboarding onboarding, OnboardingStatus onboardingStatus) {
        if (COMPLETED.equals(onboardingStatus)) {
            final String onboardingString = getOnboardingString(objectMapper(), onboarding);
            ctx.callActivity(REJECT_OUTDATED_ONBOARDINGS, onboardingString, optionsRetry(), String.class).await();
        }
    }

    default void sendMailForUserActivity(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow, OnboardingMapper onboardingMapper) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        onboarding.getUsers().forEach(user -> {
            Onboarding onboardingWithSingleUser = onboardingMapper.mapToOnboardingWithSingleUser(onboardingWorkflow.getOnboarding(), user);
            onboardingWorkflow.setOnboarding(onboardingWithSingleUser);
            ctx.callActivity(SEND_MAIL_REGISTRATION_FOR_USER, getOnboardingString(objectMapper(), onboardingWorkflow.getOnboarding()), optionsRetry(), String.class).await();
        });
    }

    default void sendMailForUserRequesterActivity(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        if (Objects.nonNull(onboarding.getUserRequester().getUserMailUuid())) {
            ctx.callActivity(SEND_MAIL_REGISTRATION_FOR_USER_REQUESTER, getOnboardingString(objectMapper(), onboardingWorkflow.getOnboarding()), optionsRetry(), String.class).await();
        }
    }

    default void saveVisuraActivity(TaskOrchestrationContext ctx, Onboarding onboarding) {
        List<String> atecoCodes = onboarding.getInstitution().getAtecoCodes();
        if (Objects.nonNull(atecoCodes) && !atecoCodes.isEmpty()) {
            ctx.callActivity(SAVE_VISURA_FOR_MERCHANT, getOnboardingString(objectMapper(), onboarding), optionsRetry(), String.class).await();
        }
    }

}
