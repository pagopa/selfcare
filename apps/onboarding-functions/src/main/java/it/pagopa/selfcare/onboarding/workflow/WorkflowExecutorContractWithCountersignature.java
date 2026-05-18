package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowInstitution;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.product.entity.ManagingInstitution;
import it.pagopa.selfcare.product.entity.SigningConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.openapi.quarkus.document_json.model.DocumentResponse;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.INSTITUTION;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.BUILD_CONTRACT_ACTIVITY_NAME;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.GET_LATEST_DOCUMENT_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.GET_MANAGING_INSTITUTION_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.GET_SIGNING_CONFIGURATION_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.GET_USER_EMAIL_UUID_ACTIVITY;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_MAIL_NOTIFICATION_MANAGING_INSTITUTION;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.SEND_MAIL_REGISTRATION_FOR_CONTRACT;
import static it.pagopa.selfcare.onboarding.utils.Utils.getManagingInstitutionEmailRequestString;
import static it.pagopa.selfcare.onboarding.utils.Utils.getManagingInstitutionSendEmailString;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingString;
import static it.pagopa.selfcare.onboarding.utils.Utils.getOnboardingWorkflowString;
import static it.pagopa.selfcare.onboarding.utils.Utils.readEmailList;

@Slf4j
public class WorkflowExecutorContractWithCountersignature implements WorkflowExecutor {

    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;
    private final OnboardingMapper onboardingMapper;

    private static final int DEFAULT_SIGNING_STEP = 1;
    private static final int DEFAULT_REQUIRED_SIGNATURES = 1;


    public WorkflowExecutorContractWithCountersignature(ObjectMapper objectMapper, TaskOptions optionsRetry,  OnboardingMapper onboardingMapper) {
        this.objectMapper = objectMapper;
        this.optionsRetry = optionsRetry;
        this.onboardingMapper = onboardingMapper;
    }

    @Override
    public Optional<OnboardingStatus> executeRequestState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        String onboardingWorkflowString = getOnboardingWorkflowString(objectMapper, onboardingWorkflow);
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
        String onboardingString = getOnboardingString(objectMapper, onboardingWorkflow.getOnboarding());
        String latestDocumentString =
                ctx.callActivity(GET_LATEST_DOCUMENT_ACTIVITY, onboardingString, optionsRetry, String.class).await();
        SigningConfiguration signingConfiguration =
                ctx.callActivity(GET_SIGNING_CONFIGURATION_ACTIVITY, onboardingString, optionsRetry, SigningConfiguration.class).await();

        DocumentResponse document = parseDocumentOrNull(latestDocumentString);
        int signingStep =
            Objects.nonNull(document) && Objects.nonNull(document.getSigningStep())
                ? document.getSigningStep()
                : DEFAULT_SIGNING_STEP;
        int requiredSignatures =
            Objects.nonNull(signingConfiguration)
                ? signingConfiguration.getRequiredSignatures()
                : DEFAULT_REQUIRED_SIGNATURES;

        if (signingStep < requiredSignatures - 1) {
        log.info(
            "Contract with countersignature with onboardingId={} is still in {}, signingStep={}, requiredSignatures={}",
            onboardingWorkflow.getOnboarding().getId(),
            OnboardingStatus.PENDING,
            signingStep,
            requiredSignatures);
            return Optional.empty();
        }

        ManagingInstitution managingInstitution =
            Collections.singletonList(
                ctx.callActivity(GET_MANAGING_INSTITUTION_ACTIVITY,onboardingString,optionsRetry,ManagingInstitution.class)
                    .await()).get(0);

        String managingInstitutionEmailRequestString =
            getManagingInstitutionEmailRequestString(
                objectMapper,
                managingInstitution.getInstitutionId(),
                document.getProductId(),
                onboardingWorkflow.getOnboarding().getId());

        String emailsString =
            ctx.callActivity(
                    GET_USER_EMAIL_UUID_ACTIVITY,
                    managingInstitutionEmailRequestString,
                    optionsRetry,
                    String.class)
                .await();

        List<String> emailsUuid = readEmailList(objectMapper, emailsString);
        log.info("Found {} email(s) for institution {} and product {}",
            emailsUuid.size(),
            managingInstitution.getInstitutionId(),
            document.getProductId());

        emailsUuid.forEach(
            emailUuid ->
              ctx.callActivity(
                      SEND_MAIL_NOTIFICATION_MANAGING_INSTITUTION,
                      getManagingInstitutionSendEmailString(
                          objectMapper,
                          managingInstitution.getInstitutionId(),
                          managingInstitution.getDescription(),
                          document.getProductId(),
                          emailUuid),
                      optionsRetry,
                      String.class)
                  .await()
            );

        return Optional.of(OnboardingStatus.PENDING_IN_REVIEW);
    }

    @Override
    public Optional<OnboardingStatus> executePendingInReviewState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
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

    private DocumentResponse parseDocumentOrNull(String latestDocumentString) {
        if (latestDocumentString == null) {
            return null;
        }
        try {
            return objectMapper.readValue(latestDocumentString, DocumentResponse.class);
        } catch (JsonProcessingException e) {
            throw new GenericOnboardingException("Cannot deserialize latest document payload: " + e.getMessage());
        }
    }
}
