package it.pagopa.selfcare.party.registry_proxy.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.Topic;
import io.swagger.annotations.Api;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ServiceUnavailableException;
import it.pagopa.selfcare.party.registry_proxy.core.ApplicationInsightsLogger;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static it.pagopa.selfcare.party.registry_proxy.core.ApplicationInsightsLogger.EVENT_PROCESSING_ERROR;

@Slf4j
@RestController
@RequestMapping(value = "/dapr")
@Api(tags = "dapr")
public class EventController {
  public static final String RESPONSE_FAILED = "FAILED";
  public static final String RESPONSE_SUCCESS = "SUCCESS";
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SearchService searchService;
  private final ApplicationInsightsLogger appInsightsLogger;

  @Autowired
  public EventController(SearchService searchService, ApplicationInsightsLogger appInsightsLogger) {
    this.searchService = searchService;
    this.appInsightsLogger = appInsightsLogger;
  }

  @PostMapping("/subscribe")
  public List<Map<String, String>> subscribe() {
    return searchService.subscribe();
  }

  @Topic(name = "SC-Contracts", pubsubName = "selc-eventhub-pubsub")
  @PostMapping("/events")
  public ResponseEntity<Map<String, Object>> handleEvent(@RequestBody Map<String, Object> event) {
    long startTime = System.currentTimeMillis();
    String institutionId = event.getOrDefault("institutionId", "").toString();
    String eventId = event.getOrDefault("id", "").toString();

    try {
      String eventJson = objectMapper.writeValueAsString(event);
      log.info("Received event: {}", sanitizeForLog(eventJson));

      if (institutionId == null || institutionId.trim().isEmpty()) {
        String error = "Institution ID is missing or empty in event data";
        log.error(error);
        appInsightsLogger.logEventProcessingError(EVENT_PROCESSING_ERROR, eventId, institutionId, error, "The event must contain 'institutionId' field", null);
        return createErrorResponse("Missing institution id", "The event must contain 'institutionId' field");
      }

      searchService.indexInstitution(institutionId);
      long processingTime = System.currentTimeMillis() - startTime;

      appInsightsLogger.logPerformanceMetric("InstitutionIndexProcessing", processingTime, true, Map.of("institutionId", institutionId, "eventId", eventId));
      return createSuccessResponse(eventId, institutionId, "Event processed and institution indexed successfully");

    } catch (ResourceNotFoundException e) {
      String error = "Indexing failed - Missing institution ID";
      logIndexError(e, error, institutionId, eventId);
      appInsightsLogger.logEventProcessingError(EVENT_PROCESSING_ERROR, eventId, institutionId, error, e.getMessage(), e);

      return createErrorResponse(error, e.getMessage());
    } catch (ServiceUnavailableException e) {
      String error = "Indexing failed - Service unavailable";
      logIndexError(e, error, institutionId, eventId);
      appInsightsLogger.logEventProcessingError(EVENT_PROCESSING_ERROR, eventId, institutionId, error, e.getMessage(), e);

      return createServerErrorResponse("Error processing event", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    catch (Exception e) {
      String error = "Indexing failed - Failed to parse event payload";
      logIndexError(e, error, institutionId, eventId);
      appInsightsLogger.logEventProcessingError(EVENT_PROCESSING_ERROR, eventId, institutionId, error, e.getMessage(), e);

      return createErrorResponse("Invalid JSON payload", e.getMessage());
    }

  }

  private static void logIndexError(Exception e, String error, String institutionId, String eventId) {
    log.error("{} for institution {} in event {}: {}", error, institutionId, eventId, e.getMessage());
  }

  @NotNull
  private static ResponseEntity<Map<String, Object>> createServerErrorResponse(String error_processing_event, String server_error, HttpStatus internalServerError) {
    Map<String, Object> response = new HashMap<>();
    response.put("status", RESPONSE_FAILED);
    response.put("error", error_processing_event);
    response.put("details", server_error);
    response.put("timestamp", System.currentTimeMillis());
    return ResponseEntity.status(internalServerError).body(response);
  }

  private ResponseEntity<Map<String, Object>> createSuccessResponse(String eventId, String institutionId, String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("status", RESPONSE_SUCCESS);
    response.put("message", message);
    response.put("eventId", eventId);
    response.put("institutionId", institutionId);
    response.put("timestamp", System.currentTimeMillis());

    return ResponseEntity.ok(response);
  }
  private ResponseEntity<Map<String, Object>> createErrorResponse(String error, String details) {
    return createServerErrorResponse(error, details, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "healthy");
    response.put("service", "registry-proxy-events");
    response.put("timestamp", String.valueOf(System.currentTimeMillis()));

    return ResponseEntity.ok(response);
  }

  /**
   * Sanitize user-provided input to prevent log injection.
   * Removes CR, LF, TAB, VT, FF, Unicode LS, PS and replaces them with a space.
   */
  private static String sanitizeForLog(String input) {
    if (input == null) {
      return null;
    }
    // Replace CR, LF, TAB, VT, FF, Unicode line/paragraph separators with space
    return input.replaceAll("[\\r\\n\\t\\u000B\\f\\u2028\\u2029]", " ");
  }
}
