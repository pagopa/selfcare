package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.durabletask.*;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.AggregateBatchConfig;
import it.pagopa.selfcare.onboarding.config.RetryPolicyConfig;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingAttachment;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.TelemetryService;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.CompletionService;
import it.pagopa.selfcare.onboarding.service.ContractService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.utils.InstitutionUtils;
import it.pagopa.selfcare.onboarding.workflow.*;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import org.openapi.quarkus.core_json.model.DelegationResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.*;

/** Azure Functions with HTTP Trigger integrated with Quarkus */
public class OnboardingFunctions {

  public static final String CREATED_NEW_ONBOARDING_ORCHESTRATION_WITH_INSTANCE_ID_MSG =
      "Created new Onboarding orchestration with instance ID = ";
  private static final String CREATED_NEW_BUILD_ATTACHMENTS_ORCHESTRATION_WITH_INSTANCE_ID_MSG =
      "Created new Build Attachments orchestration with instance ID = ";

  private final OnboardingService service;
  private final CompletionService completionService;
  private final ContractService contractService;
  private final ObjectMapper objectMapper;
  private final TaskOptions optionsRetry;
  private final OnboardingMapper onboardingMapper;
  private final TelemetryService telemetryService;
  private final ProductService productService;
  private final AggregateBatchConfig aggregateBatchConfig;

  public OnboardingFunctions(
      OnboardingService service,
      ObjectMapper objectMapper,
      RetryPolicyConfig retryPolicyConfig,
      CompletionService completionService,
      ContractService contractService,
      OnboardingMapper onboardingMapper,
      ProductService productService,
      AggregateBatchConfig aggregateBatchConfig,
      TelemetryService telemetryService) {
    this.service = service;
    this.objectMapper = objectMapper;
    this.completionService = completionService;
    this.contractService = contractService;
    this.onboardingMapper = onboardingMapper;
    this.productService = productService;
    this.aggregateBatchConfig = aggregateBatchConfig;
    this.telemetryService = telemetryService;
    final int maxAttempts = retryPolicyConfig.maxAttempts();
    final Duration firstRetryInterval = Duration.ofSeconds(retryPolicyConfig.firstRetryInterval());
    RetryPolicy retryPolicy = new RetryPolicy(maxAttempts, firstRetryInterval);
    retryPolicy.setBackoffCoefficient(retryPolicyConfig.backoffCoefficient());
    optionsRetry = new TaskOptions(retryPolicy);
  }

  /**
   * This HTTP-triggered function starts the orchestration. Depending on the time required to get
   * the response from the orchestration instance, there are two cases: * The orchestration
   * instances complete within the defined timeout and the response is the actual orchestration
   * instance output, delivered synchronously. * The orchestration instances can't complete within
   * the defined timeout, and the response is the default one described in http api uri
   */
  @FunctionName(START_ONBOARDING_ORCHESTRATION)
  public HttpResponseMessage startOrchestration(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.GET, HttpMethod.POST},
              authLevel = AuthorizationLevel.FUNCTION)
          HttpRequestMessage<Optional<String>> request,
      @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
      final ExecutionContext context) {

    final String onboardingId = request.getQueryParameters().get("onboardingId");
    final String timeoutString = request.getQueryParameters().get("timeout");

    Map<String, String> properties = Map.of("onboardingId", onboardingId);

    telemetryService.trackFunction(
        START_ONBOARDING_ORCHESTRATION,
        "StartOnboardingOrchestration trigger processed a request",
        SeverityLevel.Information,
        properties);

    DurableTaskClient client = durableContext.getClient();
    String instanceId = client.scheduleNewOrchestrationInstance("Onboardings", onboardingId);

    telemetryService.trackFunction(
        START_ONBOARDING_ORCHESTRATION,
        String.format(
            "%s %s", CREATED_NEW_ONBOARDING_ORCHESTRATION_WITH_INSTANCE_ID_MSG, instanceId),
        SeverityLevel.Information,
        properties);

    try {

      /* if timeout is null, caller wants response asynchronously */
      if (Objects.isNull(timeoutString)) {
        return durableContext.createCheckStatusResponse(request, instanceId);
      }

      int timeoutInSeconds = Integer.parseInt(timeoutString);
      OrchestrationMetadata metadata =
          client.waitForInstanceCompletion(instanceId, Duration.ofSeconds(timeoutInSeconds), true);

      boolean isFailed =
          Optional.ofNullable(metadata)
              .map(
                  orchestration ->
                      OrchestrationRuntimeStatus.FAILED.equals(orchestration.getRuntimeStatus()))
              .orElse(true);

      return isFailed
          ? request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build()
          : request.createResponseBuilder(HttpStatus.OK).build();
    } catch (TimeoutException timeoutEx) {
      // timeout expired - return a 202 response
      return durableContext.createCheckStatusResponse(request, instanceId);
    }
  }

  @FunctionName(ONBOARDINGS_AGGREGATE_ORCHESTRATOR)
  public void onboardingsAggregateOrchestrator(
      @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx,
      ExecutionContext functionContext) {
    String onboardingId = null;
    String onboardingAggregate = ctx.getInput(String.class);
    boolean existsDelegation;
    try {
      onboardingId = readOnboardingAggregateOrchestratorInputValue(objectMapper, onboardingAggregate).getId();
      existsDelegation =
          Boolean.parseBoolean(
              ctx.callActivity(
                      EXISTS_DELEGATION_ACTIVITY, onboardingAggregate, optionsRetry, String.class)
                  .await());
    } catch (TaskFailedException | GenericOnboardingException e) {
      telemetryService.trackFunction(
          functionContext.getFunctionName(),
          "Error checking delegation, proceeding as if it exist: " + e.getMessage(),
          SeverityLevel.Warning,
          Map.of("onboardingId", onboardingId == null ? "unknown" : onboardingId));
      existsDelegation = true;
    }
    try {
      if (!existsDelegation) {
        onboardingId =
            ctx.callActivity(
                    CREATE_AGGREGATE_ONBOARDING_REQUEST_ACTIVITY,
                    onboardingAggregate,
                    optionsRetry,
                    String.class)
                .await();
        ctx.callSubOrchestrator("Onboardings", onboardingId, String.class).await();
      }
    } catch (TaskFailedException | ResourceNotFoundException ex) {
      handleOrchestratorException(ctx, functionContext, onboardingId, ex);
      throw ex;
    }
  }

  /**
   * This orchestrator manages the batch processing of aggregate onboardings.
   * It processes aggregates in controlled batches to avoid DB overload (TooManyRequests).
   * Uses do-while pattern for sequential batch processing with controlled parallelism.
   * Uses continueAsNew only to prevent history explosion after processing multiple batches.
   */
  @FunctionName(ONBOARDINGS_AGGREGATE_BATCH_ORCHESTRATOR)
  public void onboardingsAggregateBatchOrchestrator(
      @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx,
      ExecutionContext functionContext) {

    String input = ctx.getInput(String.class);
    var batchInput = readAggregatesBatchInput(objectMapper, input);

    List<String> allInputs = batchInput.getAggregateOrchestratorInputs();
    int currentIndex = batchInput.getCurrentIndex();
    int batchSize = aggregateBatchConfig.size();
    int batchesProcessed = 0;

    // Maximum number of batches to process before calling continueAsNew to reset orchestration history.
    // This prevents history explosion in long-running orchestrations.
    final int maxBatchesBeforeContinue = aggregateBatchConfig.maxBatchesBeforeContinue();

    // Delay in seconds between batch processing to avoid DB overload (TooManyRequests).
    final int delaySecondsBetweenBatches = aggregateBatchConfig.delaySeconds();

    do {
      int endIndex = Math.min(currentIndex + batchSize, allInputs.size());
      List<String> currentBatch = allInputs.subList(currentIndex, endIndex);

      final int logCurrentIndex = currentIndex;
      final int logEndIndex = endIndex;
      log(
          ctx,
          functionContext,
          SeverityLevel.Information,
          () ->
              String.format(
                  "Processing aggregate batch [%d-%d] of %d total for onboarding %s",
                  logCurrentIndex, logEndIndex - 1, allInputs.size(), batchInput.getOnboardingId()),
          Map.of("onboardingId", batchInput.getOnboardingId()));

      // Process batch in parallel (controlled)
      List<Task<String>> batchTasks = new ArrayList<>();
      for (String aggregateInput : currentBatch) {
        batchTasks.add(ctx.callSubOrchestrator(ONBOARDINGS_AGGREGATE_ORCHESTRATOR, aggregateInput, String.class));
      }

      for (int i = 0; i < batchTasks.size(); i++) {
        try {
          batchTasks.get(i).await();
        } catch (TaskFailedException e) {
          String failedInput = currentBatch.get(i);
          log(
              ctx,
              functionContext,
              SeverityLevel.Error,
              String.format(
                  "Single aggregate orchestration failed during batch processing for onboarding %s, input [%s]: %s",
                  batchInput.getOnboardingId(), failedInput, e.getMessage()),
              Map.of("onboardingId", batchInput.getOnboardingId()));
        }
      }

      currentIndex = endIndex;
      batchesProcessed++;

      // Delay between batches (skip on last batch)
      if (currentIndex < allInputs.size()) {
        ctx.createTimer(Duration.ofSeconds(delaySecondsBetweenBatches)).await();
      }

    } while (currentIndex < allInputs.size() && batchesProcessed < maxBatchesBeforeContinue);

    // continueAsNew only if there are more items and we hit the batch limit
    if (currentIndex < allInputs.size()) {
      batchInput.setCurrentIndex(currentIndex);
      String nextInput = getAggregatesBatchInputString(objectMapper, batchInput);
      ctx.continueAsNew(nextInput);
    } else {
      log(ctx, functionContext, SeverityLevel.Information,() -> String.format(
          "Completed all aggregate batches for onboarding %s", batchInput.getOnboardingId()), Map.of("onboardingId", batchInput.getOnboardingId()));
    }
  }

  /**
   * This is the orchestrator function, which can schedule activity functions, create durable
   * timers, or wait for external events in a way that's completely fault-tolerant.
   */
  @FunctionName(ONBOARDINGS)
  public void onboardingsOrchestrator(
      @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx,
      ExecutionContext functionContext) {
    String onboardingId = ctx.getInput(String.class);
    Onboarding onboarding;
    WorkflowExecutor workflowExecutor;

    telemetryService.trackFunction(
        ONBOARDINGS,
        "OnboardingsOrchestrator trigger processed a request for onboardingId: " + onboardingId,
        SeverityLevel.Information,
        Map.of("onboardingId", onboardingId));

    try {
      onboarding =
          service
              .getOnboarding(onboardingId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          String.format("Onboarding with id %s not found!", onboardingId)));

      switch (onboarding.getWorkflowType()) {
        case CONTRACT_REGISTRATION ->
            workflowExecutor = new WorkflowExecutorContractRegistration(objectMapper, optionsRetry, onboardingMapper);
        case CONTRACT_REGISTRATION_AGGREGATOR ->
            workflowExecutor =
                new WorkflowExecutorContractRegistrationAggregator(
                    objectMapper, optionsRetry, onboardingMapper);
        case FOR_APPROVE ->
            workflowExecutor = new WorkflowExecutorForApprove(objectMapper, optionsRetry, onboardingMapper);
        case FOR_APPROVE_PT ->
            workflowExecutor = new WorkflowExecutorForApprovePt(objectMapper, optionsRetry);
        case FOR_APPROVE_GPU ->
            workflowExecutor = new WorkflowExecutorForApproveGpu(objectMapper, optionsRetry, onboardingMapper);
        case CONFIRMATION ->
            workflowExecutor = new WorkflowExecutorConfirmation(objectMapper, optionsRetry);
        case CONFIRMATION_AGGREGATE ->
            workflowExecutor = new WorkflowExecutorConfirmAggregate(objectMapper, optionsRetry);
        case CONFIRMATION_AGGREGATOR ->
                workflowExecutor =
                        new WorkflowExecutorConfirmationAggregator(
                                objectMapper, optionsRetry, onboardingMapper);
        case IMPORT -> workflowExecutor = new WorkflowExecutorImport(objectMapper, optionsRetry);
        case IMPORT_AGGREGATION -> workflowExecutor = new WorkflowExecutorImportAggregation(objectMapper, optionsRetry, onboardingMapper);
        case USERS -> workflowExecutor = new WorkflowExecutorForUsers(objectMapper, optionsRetry);
        case INCREMENT_REGISTRATION_AGGREGATOR ->
            workflowExecutor =
                new WorkflowExecutorIncrementRegistrationAggregator(
                    objectMapper, optionsRetry, onboardingMapper);
        case USERS_PG ->
            workflowExecutor = new WorkflowExecutorForUsersPg(objectMapper, optionsRetry);
        case USERS_EA ->
            workflowExecutor =
                new WorkflowExecutorForUsersEa(objectMapper, optionsRetry, onboardingMapper);
        default -> throw new IllegalArgumentException("Workflow options not found!");
      }

      Optional<OnboardingStatus> optNextStatus = workflowExecutor.execute(ctx, onboarding);
      optNextStatus.ifPresent(
          onboardingStatus -> service.updateOnboardingStatus(onboardingId, onboardingStatus));
    } catch (TaskFailedException | ResourceNotFoundException ex) {
      handleOrchestratorException(ctx, functionContext, onboardingId, ex);
      throw ex;
    }
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(BUILD_CONTRACT_ACTIVITY_NAME)
  public void buildContract(
      @DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString,
      final ExecutionContext context) {
      telemetryService.trackFunction(
        BUILD_CONTRACT_ACTIVITY_NAME,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            BUILD_CONTRACT_ACTIVITY_NAME,
            onboardingWorkflowString),
        SeverityLevel.Information,
        Map.of("onboardingWorkflow", onboardingWorkflowString));
    service.createContract(readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString));
  }

  /** This HTTP-triggered function invokes an orchestration to build attachments and save tokens */
  @FunctionName(TRIGGER_BUILD_ATTACHMENTS_AND_SAVE_TOKENS)
  public HttpResponseMessage buildAttachmentsAndSaveTokens(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.POST},
              authLevel = AuthorizationLevel.FUNCTION)
          HttpRequestMessage<Optional<String>> request,
      @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
      final ExecutionContext context) {
    context.getLogger().info("buildAttachmentsAndSaveTokens trigger processed a request");
    Optional<String> onboardingString = request.getBody();

    if (onboardingString.isEmpty()) {
      return request
          .createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("Body can not be empty")
          .build();
    }

    DurableTaskClient client = durableContext.getClient();
    String instanceId =
        client.scheduleNewOrchestrationInstance(
            "BuildAttachmentAndSaveToken", onboardingString.get());
    telemetryService.trackFunction(
        TRIGGER_BUILD_ATTACHMENTS_AND_SAVE_TOKENS,
        String.format(
            "%s %s", CREATED_NEW_BUILD_ATTACHMENTS_ORCHESTRATION_WITH_INSTANCE_ID_MSG, instanceId),
        SeverityLevel.Information,
        Map.of("instanceId", instanceId));

    return durableContext.createCheckStatusResponse(request, instanceId);
  }

  /**
   * This function is the orchestrator that manages the build attachment process, it is responsible
   * for invoking the activity function "BuildAttachment" and "saveTokenAttachment" until there are
   * no more attachment to process.
   */
  @FunctionName(BUILD_ATTACHMENTS_SAVE_TOKENS_ACTIVITY)
  public void buildAttachmentAndSaveToken(
      @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx,
      ExecutionContext functionContext)
      throws JsonProcessingException {

    String onboardingString = ctx.getInput(String.class);
    Onboarding onboarding = objectMapper.readValue(onboardingString, Onboarding.class);
    Product product = productService.getProductIsValid(onboarding.getProductId());

    product
        .getInstitutionContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getAttachments()
        .stream()
        .filter(
            attachment ->
                attachment.getWorkflowType().contains(onboarding.getWorkflowType())
                    && onboarding.getStatus().equals(attachment.getWorkflowState()))
        .forEach(
            attachment -> {
              OnboardingAttachment onboardingAttachment =
                  OnboardingAttachment.builder()
                      .attachment(attachment)
                      .onboarding(onboarding)
                      .build();
              ctx.callActivity(
                      BUILD_ATTACHMENT_ACTIVITY_NAME,
                      onboardingAttachment,
                      optionsRetry,
                      String.class)
                  .await();
              ctx.callActivity(
                      SAVE_TOKEN_WITH_ATTACHMENT_ACTIVITY_NAME,
                      onboardingAttachment,
                      optionsRetry,
                      String.class)
                  .await();
            });
    log(
        ctx,
        functionContext,
        SeverityLevel.Information,
        "BuildAttachmentAndSaveToken orchestration completed",
        Map.of("onboardingId", onboarding.getId(), "productId", onboarding.getProductId()));
  }

  private void log(
      TaskOrchestrationContext ctx,
      ExecutionContext fCtx,
      SeverityLevel severityLevel,
      Supplier<String> messageSupplier,
      Map<String, String> properties) {
    if (!ctx.getIsReplaying()) {
      telemetryService.trackFunction(
          fCtx.getFunctionName(), messageSupplier.get(), severityLevel, properties);
    }
  }

  private void log(
      TaskOrchestrationContext ctx,
      ExecutionContext fCtx,
      SeverityLevel severityLevel,
      String message,
      Map<String, String> properties) {
    if (!ctx.getIsReplaying()) {
      telemetryService.trackFunction(fCtx.getFunctionName(), message, severityLevel, properties);
    }
  }

  private void handleOrchestratorException(
      TaskOrchestrationContext ctx, ExecutionContext fCtx, String onboardingId, Exception ex) {
    log(
        ctx,
        fCtx,
        SeverityLevel.Warning,
        "Error during workflow execution: " + ex.getMessage(),
        Map.of("onboardingId", onboardingId));
    service.updateOnboardingStatusAndInstanceId(onboardingId, OnboardingStatus.FAILED, ctx.getInstanceId());
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(BUILD_ATTACHMENT_ACTIVITY_NAME)
  public void buildAttachment(
      @DurableActivityTrigger(name = "onboardingString") String onboardingAttachmentString,
      final ExecutionContext context) {
    OnboardingAttachment onboardingAttachment = readOnboardingAttachmentValue(objectMapper, onboardingAttachmentString);

    Map<String, String> properties =
        Map.of(
            "onboardingId", onboardingAttachment.getOnboarding().getId(),
            "productId", onboardingAttachment.getOnboarding().getProductId());
    telemetryService.trackFunction(
        BUILD_ATTACHMENT_ACTIVITY_NAME,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            BUILD_ATTACHMENT_ACTIVITY_NAME,
            onboardingAttachmentString),
        SeverityLevel.Information,
        properties);
    service.createAttachment(onboardingAttachment);
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME)
  public void saveToken(
      @DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString,
      final ExecutionContext context) {
    OnboardingWorkflow onboardingWorkflow =
        readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString);
    Map<String, String> properties =
        Map.of(
            "onboardingId", onboardingWorkflow.getOnboarding().getId(),
            "productId", onboardingWorkflow.getOnboarding().getProductId());
    telemetryService.trackFunction(
        SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SAVE_TOKEN_WITH_CONTRACT_ACTIVITY_NAME,
            onboardingWorkflowString),
        SeverityLevel.Information,
        properties);
    service.saveTokenWithContract(
        readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString));
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(SAVE_TOKEN_WITH_ATTACHMENT_ACTIVITY_NAME)
  public void saveTokenAttachment(
      @DurableActivityTrigger(name = "onboardingString") String onboardingAttachmentString,
      final ExecutionContext context) {
    OnboardingAttachment onboardingAttachment = readOnboardingAttachmentValue(objectMapper, onboardingAttachmentString);
    Map<String, String> properties =
        Map.of(
            "onboardingId", onboardingAttachment.getOnboarding().getId(),
            "productId", onboardingAttachment.getOnboarding().getProductId());
    telemetryService.trackFunction(
        SAVE_TOKEN_WITH_ATTACHMENT_ACTIVITY_NAME,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SAVE_TOKEN_WITH_ATTACHMENT_ACTIVITY_NAME,
            onboardingAttachmentString),
        SeverityLevel.Information,
        properties);
    service.saveTokenWithAttachment(onboardingAttachment);
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(SEND_MAIL_REGISTRATION_FOR_CONTRACT)
  public void sendMailRegistrationForContract(
      @DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString,
      final ExecutionContext context) {
    OnboardingWorkflow onboardingWorkflow = readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString);
    telemetryService.trackFunction(
        SEND_MAIL_REGISTRATION_FOR_CONTRACT,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SEND_MAIL_REGISTRATION_FOR_CONTRACT,
            onboardingWorkflowString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboardingWorkflow.getOnboarding().getId(),
            "productId", onboardingWorkflow.getOnboarding().getProductId()));
    service.sendMailRegistrationForContract(onboardingWorkflow);
  }

  @FunctionName(SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY)
  public void sendMailRegistrationForContractWhenApprove(
      @DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString,
      final ExecutionContext context) {
    OnboardingWorkflow onboardingWorkflow = readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString);
    telemetryService.trackFunction(
        SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SEND_MAIL_REGISTRATION_FOR_CONTRACT_WHEN_APPROVE_ACTIVITY,
            onboardingWorkflowString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboardingWorkflow.getOnboarding().getId(),
            "productId", onboardingWorkflow.getOnboarding().getProductId()));
    service.sendMailRegistrationForContractWhenApprove(onboardingWorkflow);
  }

  @FunctionName(SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY)
  public void sendMailRegistration(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SEND_MAIL_REGISTRATION_REQUEST_ACTIVITY,
            onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    service.sendMailRegistration(onboarding);
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(SEND_MAIL_REGISTRATION_FOR_USER)
  public void sendMailRegistrationForUser(
          @DurableActivityTrigger(name = "onboardingString") String onboardingString,
          final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        SEND_MAIL_REGISTRATION_FOR_USER,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REGISTRATION_FOR_USER, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    service.sendMailRegistrationForUser(onboarding);
  }

  @FunctionName(SEND_MAIL_REGISTRATION_FOR_USER_REQUESTER)
  public void sendMailRegistrationForUserRequester(
          @DurableActivityTrigger(name = "onboardingString") String onboardingString,
          final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        SEND_MAIL_REGISTRATION_FOR_USER_REQUESTER,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SEND_MAIL_REGISTRATION_FOR_USER_REQUESTER,
            onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    service.sendMailRegistrationForUserRequester(onboarding);
  }

  /** This is the activity function that gets invoked by the orchestrator function. */
  @FunctionName(SAVE_VISURA_FOR_MERCHANT)
  public void saveVisuraForMerchant(
          @DurableActivityTrigger(name = "onboardingString") String onboardingString,
          final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        SAVE_VISURA_FOR_MERCHANT,
        String.format(FORMAT_LOGGER_ONBOARDING_STRING, SAVE_VISURA_FOR_MERCHANT, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    service.saveVisuraForMerchant(onboarding);
  }


  @FunctionName(SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY)
  public void sendMailRegistrationApprove(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SEND_MAIL_REGISTRATION_APPROVE_ACTIVITY,
            onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    service.sendMailRegistrationApprove(onboarding);
  }

  @FunctionName(SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY)
  public void sendMailOnboardingApprove(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SEND_MAIL_ONBOARDING_APPROVE_ACTIVITY,
            onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    service.sendMailOnboardingApprove(onboarding);
  }

  @FunctionName(CREATE_INSTITUTION_ACTIVITY)
  public String createInstitutionAndPersistInstitutionId(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        CREATE_INSTITUTION_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, CREATE_INSTITUTION_ACTIVITY, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    return completionService.createInstitutionAndPersistInstitutionId(onboarding);
  }

  @FunctionName(STORE_ONBOARDING_ACTIVATEDAT)
  public void storeOnboardingActivatedAt(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        STORE_ONBOARDING_ACTIVATEDAT,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, STORE_ONBOARDING_ACTIVATEDAT, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    completionService.persistActivatedAt(readOnboardingValue(objectMapper, onboardingString));
  }

  @FunctionName(REJECT_OUTDATED_ONBOARDINGS)
  public void rejectOutdatedOnboardings(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
      Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        REJECT_OUTDATED_ONBOARDINGS,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, REJECT_OUTDATED_ONBOARDINGS, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    completionService.rejectOutdatedOnboardings(onboarding);
  }

  @FunctionName(CREATE_ONBOARDING_ACTIVITY)
  public void createOnboarding(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
      Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        CREATE_ONBOARDING_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, CREATE_ONBOARDING_ACTIVITY, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    completionService.persistOnboarding(onboarding);
  }

  @FunctionName(SEND_MAIL_COMPLETION_ACTIVITY)
  public void sendMailCompletion(
      @DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString,
      final ExecutionContext context) {
    OnboardingWorkflow onboardingWorkflow = readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString);
    telemetryService.trackFunction(
        SEND_MAIL_COMPLETION_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            SEND_MAIL_COMPLETION_ACTIVITY,
            onboardingWorkflowString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboardingWorkflow.getOnboarding().getId(),
            "productId", onboardingWorkflow.getOnboarding().getProductId()));
    completionService.sendCompletedEmail(onboardingWorkflow);
  }

  @FunctionName(SEND_MAIL_REJECTION_ACTIVITY)
  public void sendMailRejection(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        SEND_MAIL_REJECTION_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, SEND_MAIL_REJECTION_ACTIVITY, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    completionService.sendMailRejection(context, onboarding);
  }

  @FunctionName(CREATE_USERS_ACTIVITY)
  public void createOnboardedUsers(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        CREATE_USERS_ACTIVITY,
        String.format(FORMAT_LOGGER_ONBOARDING_STRING, CREATE_USERS_ACTIVITY, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    completionService.persistUsers(onboarding);
  }

  @FunctionName(CREATE_AGGREGATE_ONBOARDING_REQUEST_ACTIVITY)
  public String createAggregateOnboardingRequest(
      @DurableActivityTrigger(name = "onboardingString")
          String onboardingAggregateOrchestratorInputString,
      final ExecutionContext context) {
    OnboardingAggregateOrchestratorInput onboarding =
        readOnboardingAggregateOrchestratorInputValue(
            objectMapper, onboardingAggregateOrchestratorInputString);
    telemetryService.trackFunction(
        CREATE_AGGREGATE_ONBOARDING_REQUEST_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            CREATE_AGGREGATE_ONBOARDING_REQUEST_ACTIVITY,
            onboardingAggregateOrchestratorInputString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    return completionService.createAggregateOnboardingRequest(onboarding);
  }

  @FunctionName(CREATE_DELEGATION_ACTIVITY)
  public String createDelegationForAggregation(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        CREATE_DELEGATION_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, CREATE_DELEGATION_ACTIVITY, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    return completionService.createDelegation(onboarding);
  }

  @FunctionName(EXISTS_DELEGATION_ACTIVITY)
  public String existsDelegation(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    OnboardingAggregateOrchestratorInput onboarding =
        readOnboardingAggregateOrchestratorInputValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        EXISTS_DELEGATION_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, EXISTS_DELEGATION_ACTIVITY, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    return completionService.existsDelegation(onboarding);
  }

  @FunctionName(VERIFY_ONBOARDING_AGGREGATE_ACTIVITY)
  public String verifyOnboardingAggregate(
          @DurableActivityTrigger(name = "onboardingString") String onboardingString,
          final ExecutionContext context) {
    OnboardingAggregateOrchestratorInput onboarding =
        readOnboardingAggregateOrchestratorInputValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        VERIFY_ONBOARDING_AGGREGATE_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            VERIFY_ONBOARDING_AGGREGATE_ACTIVITY,
            onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    return completionService.verifyOnboardingAggregate(onboarding);
  }

  /**
   * This HTTP-triggered function retrieves onboarding given its identifier After that, It sends a
   * message on topics through the event bus
   */
  @FunctionName("TestSendEmail")
  public void sendTestEmail(
      @HttpTrigger(
              name = "req",
              methods = {HttpMethod.POST},
              authLevel = AuthorizationLevel.FUNCTION)
          HttpRequestMessage<Optional<String>> request,
      final ExecutionContext context) {
    context.getLogger().info("TestSendEmail trigger processed a request");
    completionService.sendTestEmail(context);
    request.createResponseBuilder(HttpStatus.OK).build();
  }

  @FunctionName(CREATE_AGGREGATES_CSV_ACTIVITY)
  public void createAggregatesCsv(
      @DurableActivityTrigger(name = "onboardingString") String onboardingWorkflowString,
      final ExecutionContext context) {
    OnboardingWorkflow onboardingWorkflow =
        readOnboardingWorkflowValue(objectMapper, onboardingWorkflowString);
    telemetryService.trackFunction(
        CREATE_AGGREGATES_CSV_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            CREATE_AGGREGATES_CSV_ACTIVITY,
            onboardingWorkflowString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboardingWorkflow.getOnboarding().getId(),
            "productId", onboardingWorkflow.getOnboarding().getProductId()));
    contractService.uploadAggregatesCsv(onboardingWorkflow);
  }

  @FunctionName(RETRIEVE_AGGREGATES_ACTIVITY)
  public String retrieveAggregates(
      @DurableActivityTrigger(name = "onboardingString") String onboardingString,
      final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        RETRIEVE_AGGREGATES_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING, RETRIEVE_AGGREGATES_ACTIVITY, onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    List<DelegationResponse> delegationResponseList =
        completionService.retrieveAggregates(onboarding);
    return getDelegationResponseListString(objectMapper, delegationResponseList);
  }

  @FunctionName(UPDATE_ONBOARDING_EXPIRING_DATE_ACTIVITY)
  public void updateOnboardingExpiringDate(
          @DurableActivityTrigger(name = "onboardingString") String onboardingString,
          final ExecutionContext context) {
    Onboarding onboarding = readOnboardingValue(objectMapper, onboardingString);
    telemetryService.trackFunction(
        UPDATE_ONBOARDING_EXPIRING_DATE_ACTIVITY,
        String.format(
            FORMAT_LOGGER_ONBOARDING_STRING,
            UPDATE_ONBOARDING_EXPIRING_DATE_ACTIVITY,
            onboardingString),
        SeverityLevel.Information,
        Map.of(
            "onboardingId", onboarding.getId(),
            "productId", onboarding.getProductId()));
    service.updateOnboardingExpiringDate(onboarding);
  }
}
