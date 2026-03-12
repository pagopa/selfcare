package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableActivityTrigger;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import com.microsoft.durabletask.azurefunctions.DurableOrchestrationTrigger;
import it.pagopa.selfcare.onboarding.dto.NotificationCountResult;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.service.NotificationEventResenderService;
import it.pagopa.selfcare.onboarding.service.NotificationEventService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.ws.rs.core.MediaType;
import org.apache.http.HttpHeaders;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import static it.pagopa.selfcare.onboarding.functions.CommonFunctions.FORMAT_LOGGER_ONBOARDING_STRING;
import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.*;
import static it.pagopa.selfcare.onboarding.utils.Utils.*;

public class NotificationFunctions {

  private static final String CREATED_NEW_RESEND_NOTIFICATIONS_ORCHESTRATION_WITH_INSTANCE_ID_MSG = "Created new Resend Notifications orchestration with instance ID = ";
  private final NotificationEventService notificationEventService;
  private final OnboardingService onboardingService;
  private final NotificationEventResenderService notificationEventResenderService;
  private final ObjectMapper objectMapper;


  public NotificationFunctions(ObjectMapper objectMapper,
                               NotificationEventService notificationEventService,
                               OnboardingService onboardingService,
                               NotificationEventResenderService notificationEventResenderService) {
    this.objectMapper = objectMapper;
    this.notificationEventService = notificationEventService;
    this.onboardingService = onboardingService;
    this.notificationEventResenderService = notificationEventResenderService;
  }

  /**
   * This HTTP-triggered function sends messages through event hub.
   * It gets invoked by module onboarding-cdc when status is COMPLETED or DELETED
   */
  @FunctionName("Notification")
  public HttpResponseMessage sendNotification(
    @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
    final ExecutionContext context) {
    context.getLogger().info("sendNotifications trigger processed a request");

    String onboardingString = request.getBody().orElseThrow(() -> new IllegalArgumentException("Request body cannot be empty."));
    final String queueEventString = request.getQueryParameters().get("queueEvent");
    final QueueEvent queueEvent = Objects.isNull(queueEventString) ? null : QueueEvent.valueOf(queueEventString);
    final Onboarding onboarding;

    try {
      onboarding = readOnboardingValue(objectMapper, onboardingString);
      context.getLogger().info(() -> String.format(FORMAT_LOGGER_ONBOARDING_STRING, SEND_ONBOARDING_NOTIFICATION, onboardingString));
    } catch (Exception ex) {
      context.getLogger().warning(() -> "Error during sendNotifications execution, msg: " + ex.getMessage());
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
        .body("Malformed object onboarding in input.")
        .build();
    }

    notificationEventService.send(context, onboarding, queueEvent);
    return request.createResponseBuilder(HttpStatus.OK).build();
  }

  /**
   * This HTTP-triggered function retrieves onboarding given its identifier
   * After that, It sends a message on topics through the event bus
   */
  @FunctionName("ResendNotification")
  public HttpResponseMessage resendNotification(
    @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
    final ExecutionContext context) {
    context.getLogger().info("sendNotifications trigger processed a request");

    final String onboardingId = request.getQueryParameters().get("onboardingId");
    if (Objects.isNull(onboardingId)) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
        .body("onboardingId cannot be empty.")
        .build();
    }

    final String queueEventString = request.getQueryParameters().get("queueEvent");
    final QueueEvent queueEvent = Objects.isNull(queueEventString) ? null : QueueEvent.valueOf(queueEventString);


    final Optional<Onboarding> onboarding = onboardingService.getOnboarding(onboardingId);
    if (onboarding.isEmpty()) {
      return request.createResponseBuilder(HttpStatus.NOT_FOUND)
        .body("Onboarding with ID: " + onboardingId + " not found")
        .build();
    }
    notificationEventService.send(context, onboarding.get(), queueEvent);
    return request.createResponseBuilder(HttpStatus.OK).build();
  }

  /**
   * This HTTP-triggered function performs for every product a count of relative onboardings in COMPLETED
   * and DELETE status in a given range of dates. It is useful to the external consumer services of the queues, as it provides them
   * a comparison tool to compare the data they have with the situation on the selfcare domain
   */
  @FunctionName("CountNotifications")
  public HttpResponseMessage countNotifications(
    @HttpTrigger(name = "req", route = "onboardings/notifications/count", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
    final ExecutionContext context) {
    context.getLogger().info("count trigger processed a request");

    String from = request.getQueryParameters().get("from");
    String to = request.getQueryParameters().get("to");
    String productId = request.getQueryParameters().get("productId");

    List<NotificationCountResult> countedResult = onboardingService.countNotifications(productId, from, to, context);
    return request.createResponseBuilder(HttpStatus.OK)
      .body(countedResult)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
      .build();
  }

  /**
   * This HTTP-triggered function invokes an orchestration to resend notifications for a given range of dates and filters
   */
  @FunctionName("ResendNotifications")
  public HttpResponseMessage resendNotifications(
    @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
    @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
    final ExecutionContext context) throws JsonProcessingException {
    context.getLogger().info("resendNotifications trigger processed a request");

    ResendNotificationsFilters filters = getResendNotificationsFilters(request);
    try {
      checkResendNotificationsFilters(filters);
    } catch (IllegalArgumentException e) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
        .body(e.getMessage())
        .build();
    }

    String filtersInJson = objectMapper.writeValueAsString(filters);
    DurableTaskClient client = durableContext.getClient();
    String instanceId = client.scheduleNewOrchestrationInstance("NotificationsSender", filtersInJson);
    context.getLogger().info(() -> String.format("%s %s", CREATED_NEW_RESEND_NOTIFICATIONS_ORCHESTRATION_WITH_INSTANCE_ID_MSG, instanceId));

    return durableContext.createCheckStatusResponse(request, instanceId);
  }

  /**
   * This function is the orchestrator that manages the resend notifications process, it is responsible for invoking
   * the activity function "NotificationsSender" until there are no more notifications to resend.
   */
  @FunctionName("NotificationsSender")
  public void notificationsSenderOrchestrator(
    @DurableOrchestrationTrigger(name = "taskOrchestrationContext") TaskOrchestrationContext ctx,
    ExecutionContext functionContext) throws JsonProcessingException {
    String filtersString = getFiltersFromContextAndEnrichWithInstanceId(ctx);
    if (functionContext.getLogger().isLoggable(Level.INFO)) {
      functionContext.getLogger().info("Resend notifications orchestration started with input: " + filtersString);
    }

    do {
      filtersString = ctx.callActivity(RESEND_NOTIFICATIONS_ACTIVITY, filtersString, String.class).await();
    } while (filtersString != null);

    functionContext.getLogger().info("Resend notifications orchestration completed");
  }

  /**
   * It retrieves the filters from the orchestrator context and enriches them with the instanceId.
   * For logging purposes, the instanceId will be used as the notificationEventTraceId.
   *
   * @param ctx TaskOrchestrationContext provided by the Azure Functions runtime.
   * @return JSON string representing the filters to be applied when resending notifications.
   * @throws JsonProcessingException if there is an error parsing the filtersString into a ResendNotificationsFilters object.
   */
  private String getFiltersFromContextAndEnrichWithInstanceId(TaskOrchestrationContext ctx) throws JsonProcessingException {
    String filtersString = ctx.getInput(String.class);
    String instanceId = ctx.getInstanceId();
    ResendNotificationsFilters filters = objectMapper.readValue(filtersString, ResendNotificationsFilters.class);
    filters.setNotificationEventTraceId(instanceId);
    return objectMapper.writeValueAsString(filters);
  }

  /**
   * It is triggered by the orchestrator function "NotificationsSender", and is responsible for a resend of single page of notifications.
   *
   * @param filtersString JSON string representing the filters to be applied when resending notifications.
   * @param context       ExecutionContext provided by the Azure Functions runtime, used for logging.
   * @return JSON string representing the next set of filters to be applied in the next iteration of the activity, or null if there are no more notifications to resend.
   * @throws JsonProcessingException if there is an error parsing the filtersString into a ResendNotificationsFilters object.
   */
  @FunctionName(RESEND_NOTIFICATIONS_ACTIVITY)
  public String resendNotificationsActivity(@DurableActivityTrigger(name = "filtersString") String filtersString, final ExecutionContext context) throws JsonProcessingException {
    context.getLogger().info(() -> String.format(FORMAT_LOGGER_ONBOARDING_STRING, RESEND_NOTIFICATIONS_ACTIVITY, filtersString));
    ResendNotificationsFilters filters;
    try {
      filters = objectMapper.readValue(filtersString, ResendNotificationsFilters.class);
    } catch (JsonProcessingException e) {
      throw new NotificationException("Error occurred during json parsing of filters", e);
    }

    /*
     * At the end of the resendNotifications it is checked whether there are more onboardings to resend, if there are, the method
     * returns the same filters received as input by incrementing the page by one to fetch on next iteration the next page of onboardings.
     * Otherwise, it returns null.
     */
    ResendNotificationsFilters nextFilters = notificationEventResenderService.resendNotifications(filters, context);

    context.getLogger().info(() -> "Resend notifications activity completed, nextFilter = " + nextFilters);
    return Objects.nonNull(nextFilters) ? objectMapper.writeValueAsString(nextFilters) : null;
  }

}
