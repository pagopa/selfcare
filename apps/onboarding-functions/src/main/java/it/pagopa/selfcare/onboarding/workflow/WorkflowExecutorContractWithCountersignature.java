package it.pagopa.selfcare.onboarding.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.durabletask.TaskOptions;
import com.microsoft.durabletask.TaskOrchestrationContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.ManagingInstitutionGetEmailRequest;
import it.pagopa.selfcare.onboarding.dto.ManagingInstitutionSendEmail;
import it.pagopa.selfcare.onboarding.dto.UserMail;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowInstitution;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.product.entity.ManagingInstitution;
import it.pagopa.selfcare.product.entity.SigningConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.openapi.quarkus.document_json.model.DocumentResponse;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.onboarding.entity.OnboardingWorkflowType.INSTITUTION;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.*;

@Slf4j
public class WorkflowExecutorContractWithCountersignature implements WorkflowExecutor {

    private static final int DEFAULT_SIGNING_STEP = 1;
    private static final int DEFAULT_REQUIRED_SIGNATURES = 1;
    private final ObjectMapper objectMapper;
    private final TaskOptions optionsRetry;
    private final OnboardingMapper onboardingMapper;


    public WorkflowExecutorContractWithCountersignature(ObjectMapper objectMapper, TaskOptions optionsRetry, OnboardingMapper onboardingMapper) {
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
        SigningContext signingContext = loadSigningContext(ctx, onboardingWorkflow);
        List<ManagingInstitution> managingInstitutions = retrieveManagingInstitutions(ctx, signingContext.onboardingString());

        if (signingContext.signingStep() < signingContext.requiredSignatures() - managingInstitutions.size()) {
            log.info(
                    "Contract with countersignature with onboardingId={} is still in {}, signingStep={}, requiredSignatures={}, product={}",
                    onboardingWorkflow.getOnboarding().getId(),
                    OnboardingStatus.PENDING,
                    signingContext.signingStep(),
                    signingContext.requiredSignatures(),
                    onboardingWorkflow.getOnboarding().getProductId());
            return Optional.empty();
        }

        ManagingInstitution managingInstitution = resolveManagingInstitutionForStep(
                managingInstitutions,
                signingContext.signingStep(),
                onboardingWorkflow.getOnboarding().getProductId());

        notifyManagingInstitution(ctx, managingInstitution, signingContext.document(), onboardingWorkflow.getOnboarding());
        return Optional.of(OnboardingStatus.PENDING_IN_REVIEW);
    }

    @Override
    public Optional<OnboardingStatus> executePendingInReviewState(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
        SigningContext signingContext = loadSigningContext(ctx, onboardingWorkflow);

        if (signingContext.signingStep() < signingContext.requiredSignatures()) {
            log.info(
                    "Contract with countersignature with onboardingId={} is still in {}, signingStep={}, requiredSignatures={}, product={}",
                    onboardingWorkflow.getOnboarding().getId(),
                    OnboardingStatus.PENDING_IN_REVIEW,
                    signingContext.signingStep(),
                    signingContext.requiredSignatures(),
                    onboardingWorkflow.getOnboarding().getProductId());
            List<ManagingInstitution> managingInstitutions = retrieveManagingInstitutions(ctx, signingContext.onboardingString());
            ManagingInstitution managingInstitution = resolveManagingInstitutionForStep(
                    managingInstitutions,
                    signingContext.signingStep(),
                    onboardingWorkflow.getOnboarding().getProductId());
            notifyManagingInstitution(ctx, managingInstitution, signingContext.document(), onboardingWorkflow.getOnboarding());
            return Optional.empty();
        }
        log.info("Contract with onboardingId={} set to {} state", onboardingWorkflow.getOnboarding().getId(), OnboardingStatus.COMPLETED);
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


    /**
     * Loads signing-related context for the current onboarding workflow.
     * Retrieves the latest document and signing configuration, then derives the current
     * signing step and required signatures.
     *
     * @param ctx the orchestration context used to call activities
     * @param onboardingWorkflow the workflow containing onboarding data
     * @return a SigningContext with onboarding string, latest document, signing step, and required signatures
     */
    private SigningContext loadSigningContext(TaskOrchestrationContext ctx, OnboardingWorkflow onboardingWorkflow) {
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

        return new SigningContext(onboardingString, document, signingStep, requiredSignatures);
    }

    private List<ManagingInstitution> retrieveManagingInstitutions(TaskOrchestrationContext ctx, String onboardingString) {
        ManagingInstitution[] managingInstitutionArray =
                ctx.callActivity(GET_MANAGING_INSTITUTION_ACTIVITY, onboardingString, optionsRetry, ManagingInstitution[].class)
                        .await();
        return managingInstitutionArray != null ? List.of(managingInstitutionArray) : Collections.emptyList();
    }

    /**
     * Resolves the managing institution responsible for the next signing step.
     * The signing step is determined by the 'signingStep' field of the latest document, incremented by 1.
     * If no document is present, it defaults to 1.
     *
     * @param managingInstitutions the list of managing institutions associated with the product
     * @param signingStep          the current signing step, determined by the latest document
     * @param productId            the product ID, used for error message construction
     * @return the managing institution responsible for the next signing step
     * @throws GenericOnboardingException if no managing institution is found for the next signing step
     */
    private ManagingInstitution resolveManagingInstitutionForStep(
            List<ManagingInstitution> managingInstitutions,
            int signingStep,
            String productId) {
        return managingInstitutions.stream()
                .filter(mi -> mi.getSigningStep() == signingStep + 1)
                .findFirst()
                .orElseThrow(
                        () -> new GenericOnboardingException(
                                String.format("No managing institution found for product=%s and signing step=%d",
                                        productId, signingStep+1)));
    }

    /**
     * Notifies the managing institution responsible for the next signing step by sending an email.
     * The email addresses of the users associated with the managing institution are retrieved and used to send the notification.
     *
     * @param ctx                 the orchestration context used to call activities
     * @param managingInstitution the managing institution to be notified
     * @param document            the latest document, used to retrieve product information
     * @param onboarding       the onboarding, used for retrieving email addresses and constructing the notification
     */
    private void notifyManagingInstitution(
            TaskOrchestrationContext ctx,
            ManagingInstitution managingInstitution,
            DocumentResponse document,
            Onboarding onboarding) {
        ManagingInstitutionGetEmailRequest managingInstitutionGetEmailRequest = ManagingInstitutionGetEmailRequest.builder()
                .managingInstitutionId(managingInstitution.getInstitutionId())
                .productId(document.getProductId())
                .onboardingId(onboarding.getId())
                .build();

        String emailsString =
                ctx.callActivity(GET_USER_EMAIL_UUID_ACTIVITY,
                                managingInstitutionGetEmailRequest,
                                optionsRetry,
                                String.class)
                        .await();

        List<UserMail> userMails = readEmailList(objectMapper, emailsString);
        log.info("Found {} email(s) for institution {} and product {}",
                userMails.size(),
                managingInstitution.getInstitutionId(),
                document.getProductId());

        ManagingInstitutionSendEmail managingInstitutionSendEmail = ManagingInstitutionSendEmail.builder()
                .managingInstitutionId(managingInstitution.getInstitutionId())
                .onboardingInstitutionDescription(onboarding.getInstitution().getDescription())
                .productId(document.getProductId())
                .build();

        userMails.forEach(
                userMail -> {
                    managingInstitutionSendEmail.setUserId(userMail.getUserId());
                    managingInstitutionSendEmail.setUserMailUuid(userMail.getUserMailUuid());
                    ctx.callActivity(
                                    SEND_MAIL_NOTIFICATION_MANAGING_INSTITUTION,
                                    managingInstitutionSendEmail,
                                    optionsRetry,
                                    String.class)
                            .await();
                });
    }

    private record SigningContext(
            String onboardingString,
            DocumentResponse document,
            int signingStep,
            int requiredSignatures) {
    }
}
