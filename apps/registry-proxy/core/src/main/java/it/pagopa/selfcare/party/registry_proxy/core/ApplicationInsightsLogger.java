package it.pagopa.selfcare.party.registry_proxy.core;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.InvokeBindingRequest;
import io.dapr.utils.TypeRef;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ApplicationInsightsLogger {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationInsightsLogger.class);
  public static final String PERFORMANCE_INDEX_METRIC = "PerformanceIndexMetric";
  public static final String EVENT_PROCESSING_ERROR = "IndexInstitutionProcessingError";
  public static final String EVENT_PROCESSING_SUCCESS = "IndexInstitutionProcessingSuccess";
  public static final String EVENT_PROCESSING_WARNING = "IndexInstitutionProcessingWarning";

  private static final String AI_CUSTOM_EVENTS_TYPE = "Microsoft.ApplicationInsights.Event";
  private static final String AI_EXCEPTIONS_TYPE = "Microsoft.ApplicationInsights.Exception";
  private static final String AI_METRICS_TYPE = "Microsoft.ApplicationInsights.Metric";

  public static final String OPERATION = "operation";
  public static final String SUCCESS = "success";
  public static final String SEVERITY_LEVEL = "severityLevel";
  public static final String TIMESTAMP = "timestamp";
  public static final String EVENT_TYPE = "eventType";
  public static final String APPLICATION_NAME = "applicationName";
  public static final String CORRELATION_ID = "correlationId";
  public static final String VERSION = "version";
  public static final String SEVERITY_LEVEL_INFORMATION = "Information";
  public static final String EVENT_ID = "eventId";
  public static final String INSTITUTION_ID = "institutionId";
  public static final String PROCESSING_TIME_MS = "processingTimeMs";
  public static final String SEVERITY_LEVEL_WARNING = "Warning";
  public static final String WARNING = "warning";
  public static final String REASON = "reason";
  public static final String SEVERITY_LEVEL_ERROR = "Error";
  public static final String MESSAGE = "message";
  public static final String ERROR = "error";
  public static final String DETAILS = "details";
  public static final String NAME = "name";
  public static final String PROPERTIES = "properties";
  public static final String MEASUREMENTS = "measurements";
  public static final String BASE_TYPE = "baseType";
  public static final String EVENT_DATA = "EventData";
  public static final String BASE_DATA = "baseData";
  public static final String ID = "id";
  public static final String OUTER_ID = "outerId";
  public static final String TYPE_NAME = "typeName";
  public static final String HAS_FULL_STACK = "hasFullStack";
  public static final String STACK = "stack";
  public static final String EXCEPTIONS = "exceptions";
  public static final String EXCEPTION_DATA = "ExceptionData";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String APPLICATION_JSON = "application/json";
  public static final String OPERATION_CREATE = "create";
  public static final String TIME = "time";
  public static final String I_KEY = "iKey";
  public static final String DATA = "data";

  private final DaprClient daprClient;
  private final ObjectMapper objectMapper;

  @Value("${dapr.appinsights.binding.name:appinsights-binding}")
  private String appInsightsBindingName;

  @Value("${spring.application.name:registry-proxy}")
  private String applicationName;

  @Value("${logging.appinsights.enabled:true}")
  private boolean appInsightsLoggingEnabled;

  @Value("${appinsights.connection.string}")
  private String connectionString;

  public ApplicationInsightsLogger() {
    this.daprClient = new DaprClientBuilder().build();
    this.objectMapper = new ObjectMapper();
  }

  public void logEventProcessingError(String eventType, String eventId, String institutionId, String error, String details, Throwable exception) {
    if (!appInsightsLoggingEnabled) {
      return;
    }

    try {
      if (exception != null) {
        sendExceptionTelemetry(exception, Map.of(
          EVENT_ID, eventId != null ? eventId : "",
          INSTITUTION_ID, institutionId != null ? institutionId : "",
          ERROR, error != null ? error : "",
          DETAILS, details != null ? details : "",
          EVENT_TYPE, eventType
        ));
      } else {
        Map<String, String> properties = new HashMap<>();
        properties.put(EVENT_ID, eventId != null ? eventId : "");
        properties.put(INSTITUTION_ID, institutionId != null ? institutionId : "");
        properties.put(ERROR, error != null ? error : "");
        properties.put(DETAILS, details != null ? details : "");
        properties.put(SEVERITY_LEVEL, SEVERITY_LEVEL_ERROR);
        sendCustomEvent(eventType, properties, null);
      }
    } catch (Exception e) {
      logger.error("Failed to send error log to Application Insights: {}", e.getMessage(), e);
    }
  }

  private void sendCustomEvent(String eventName, Map<String, String> properties, Map<String, Double> measurements) {
    Map<String, Object> baseData = new HashMap<>();
    baseData.put(NAME, eventName);
    baseData.put(VERSION, 1);

    if (properties != null && !properties.isEmpty()) {
      baseData.put(PROPERTIES, properties);
    }

    if (measurements != null && !measurements.isEmpty()) {
      baseData.put(MEASUREMENTS, measurements);
    }

    Map<String, Object> data = new HashMap<>();
    data.put(BASE_TYPE, EVENT_DATA);
    data.put(BASE_DATA, baseData);

    Map<String, Object> telemetryItem = createBaseTelemetryItem(AI_CUSTOM_EVENTS_TYPE, data);
    sendTelemetryAsync(List.of(telemetryItem));
  }

  private void sendExceptionTelemetry(Throwable exception, Map<String, String> properties) {
    Map<String, Object> exceptionDetails = new HashMap<>();
    exceptionDetails.put(ID, 1);
    exceptionDetails.put(OUTER_ID, 0);
    exceptionDetails.put(TYPE_NAME, exception.getClass().getName());
    exceptionDetails.put(MESSAGE, exception.getMessage() != null ? exception.getMessage() : "");
    exceptionDetails.put(HAS_FULL_STACK, true);
    exceptionDetails.put(STACK, getStackTrace(exception));

    Map<String, Object> baseData = new HashMap<>();
    baseData.put(VERSION, 1);
    baseData.put(EXCEPTIONS, List.of(exceptionDetails));
    baseData.put(SEVERITY_LEVEL, SEVERITY_LEVEL_ERROR);

    if (properties != null && !properties.isEmpty()) {
      baseData.put(PROPERTIES, properties);
    }

    Map<String, Object> data = new HashMap<>();
    data.put(BASE_TYPE, EXCEPTION_DATA);
    data.put(BASE_DATA, baseData);

    Map<String, Object> telemetryItem = createBaseTelemetryItem(AI_EXCEPTIONS_TYPE, data);

    sendTelemetryAsync(List.of(telemetryItem));
  }

  private void sendTelemetryAsync(List<Map<String, Object>> telemetryItems) {
    Mono.fromCallable(() -> {
        try {
          String jsonPayload = objectMapper.writeValueAsString(telemetryItems);
          logger.debug("Sending telemetry payload: {}", jsonPayload);

          InvokeBindingRequest request = new InvokeBindingRequest(appInsightsBindingName, OPERATION_CREATE)
            .setData(jsonPayload.getBytes())
            .setMetadata(Map.of(
              CONTENT_TYPE, APPLICATION_JSON
            ));

          daprClient.invokeBinding(request, TypeRef.get(Void.class)).block();
          logger.debug("Telemetry sent to Application Insights successfully");
          return null;

        } catch (Exception e) {
          logger.error("Error sending telemetry to Application Insights: {}", e.getMessage(), e);
          throw new RuntimeException("Failed to send telemetry", e);
        }
      })
      .onErrorResume(throwable -> {
        logger.warn("Application Insights logging failed, continuing without blocking: {}", throwable.getMessage());
        return Mono.empty();
      })
      .subscribe();
  }

  private Map<String, Object> createBaseTelemetryItem(String name, Map<String, Object> data) {
    Map<String, Object> telemetryItem = new HashMap<>();
    telemetryItem.put(NAME, name);
    telemetryItem.put(TIME, Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
    telemetryItem.put(I_KEY, extractInstrumentationKey());
    telemetryItem.put(DATA, data);

    // Tags standard
    Map<String, String> tags = new HashMap<>();
    tags.put("ai.session.id", UUID.randomUUID().toString());
    tags.put("ai.operation.id", UUID.randomUUID().toString());
    tags.put("ai.application.ver", "1.0.0");
    tags.put("ai.cloud.role", applicationName);
    tags.put("ai.internal.sdkVersion", "java:dapr:1.0.0");

    telemetryItem.put("tags", tags);

    return telemetryItem;
  }

  public void logEventProcessingSuccess(String eventType, String eventId, String institutionId, long processingTimeMs) {
    if (!appInsightsLoggingEnabled) {
      return;
    }

    try {
      Map<String, String> logData = new HashMap<>(); //createBaseLogEntry(EVENT_PROCESSING_SUCCESS)
      logData.put(EVENT_ID, eventId);
      logData.put(INSTITUTION_ID, institutionId);
      logData.put(SEVERITY_LEVEL, SEVERITY_LEVEL_INFORMATION);

      Map<String, Double> measurements = new HashMap<>();
      measurements.put(PROCESSING_TIME_MS, (double) processingTimeMs);

      sendCustomEvent(eventType, logData, measurements);
    } catch (Exception e) {
      logger.error("Failed to send success log to Application Insights: {}", e.getMessage(), e);
    }
  }

  /**
   * Log di warning per eventi che richiedono attenzione
   */
  public void logEventProcessingWarning(String eventId, String institutionId, String warning, String reason) {
    if (!appInsightsLoggingEnabled) {
      return;
    }

    try {
      Map<String, String> properties = new HashMap<>();
      properties.put(EVENT_ID, eventId);
      properties.put(INSTITUTION_ID, institutionId);
      properties.put(WARNING, warning);
      properties.put(REASON, reason);
      properties.put(SEVERITY_LEVEL, SEVERITY_LEVEL_WARNING);

      sendCustomEvent(EVENT_PROCESSING_WARNING, properties, null);

    } catch (Exception e) {
      logger.error("Failed to send warning log to Application Insights: {}", e.getMessage(), e);
    }
  }

  /**
   * Log custom con metriche personalizzate
   */
  public void logCustomEvent(String eventName, Map<String, Object> properties, Map<String, Double> measurements) {
    if (!appInsightsLoggingEnabled) {
      return;
    }

    try {
      Map<String, String> stringProperties = new HashMap<>();
      if (properties != null) {
        properties.forEach((key, value) ->
          stringProperties.put(key, value != null ? value.toString() : "")
        );
      }

      sendCustomEvent(eventName, stringProperties, measurements);

    } catch (Exception e) {
      logger.error("Failed to send custom event to Application Insights: {}", e.getMessage(), e);
    }
  }

  /**
   * Log per tracking delle performance
   */
  public void logPerformanceMetric(String operation, long durationMs, boolean success, Map<String, String> additionalProperties) {
    if (!appInsightsLoggingEnabled) {
      return;
    }

    try {
      Map<String, String> properties = new HashMap<>();
      properties.put(OPERATION, operation);
      properties.put(SUCCESS, String.valueOf(success));
      properties.put(APPLICATION_NAME, applicationName);
      properties.put(SEVERITY_LEVEL, SEVERITY_LEVEL_INFORMATION);

      if (additionalProperties != null) {
        properties.putAll(additionalProperties);
      }
      Map<String, Double> measurements = new HashMap<>();
      measurements.put("durationMs", (double) durationMs);

      sendCustomEvent(PERFORMANCE_INDEX_METRIC, properties, measurements);
    } catch (Exception e) {
      logger.error("Failed to send performance metric to Application Insights: {}", e.getMessage(), e);
    }
  }

  /**
   * Builder per creare entry di log base
   */
  private LogDataBuilder createBaseLogEntry(String eventType) {
    return new LogDataBuilder()
      .put(TIMESTAMP, Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT))
      .put(EVENT_TYPE, eventType)
      .put(APPLICATION_NAME, applicationName)
      .put(CORRELATION_ID, UUID.randomUUID().toString())
      .put("iKey", extractInstrumentationKey())
      .put("name", "Microsoft.ApplicationInsights." + extractInstrumentationKey())
      .put(VERSION, "1.0");
  }

  /**
   * Invia il log in modo asincrono tramite Dapr binding
   */
  private void sendLogAsync(Map<String, Object> logData) {
    Mono.fromCallable(() -> {
        try {
          String jsonPayload = objectMapper.writeValueAsString(logData);

          InvokeBindingRequest request = new InvokeBindingRequest(appInsightsBindingName, "create")
            .setData(jsonPayload.getBytes())
            .setMetadata(Map.of(
              "operation", "track"
            ));

          daprClient.invokeBinding(request, TypeRef.get(Void.class)).block();
          logger.debug("Log sent to Application Insights successfully");
          return null;

        } catch (Exception e) {
          logger.error("Error sending log to Application Insights: {}", e.getMessage(), e);
          throw new RuntimeException("Failed to send log", e);
        }
      })
      .onErrorResume(throwable -> {
        logger.warn("Application Insights logging failed, continuing without blocking: {}", throwable.getMessage());
        return Mono.empty();
      })
      .subscribe();
  }

  /**
   * Utility per ottenere stack trace come stringa
   */
  private String getStackTrace(Throwable throwable) {
    if (throwable == null) return null;

    StringBuilder sb = new StringBuilder();
    sb.append(throwable.getMessage()).append("\n");

    for (StackTraceElement element : throwable.getStackTrace()) {
      sb.append("\tat ").append(element.toString()).append("\n");
    }

    if (throwable.getCause() != null) {
      sb.append("Caused by: ").append(getStackTrace(throwable.getCause()));
    }

    return sb.toString();
  }

  /**
   * Builder helper class
   */
  private static class LogDataBuilder {
    private final Map<String, Object> data = new HashMap<>();

    public LogDataBuilder put(String key, Object value) {
      if (value != null) {
        data.put(key, value);
      }
      return this;
    }

    public Map<String, Object> build() {
      return new HashMap<>(data);
    }
  }

  private String extractInstrumentationKey() {
    if (connectionString != null && connectionString.contains("InstrumentationKey=")) {
      String[] parts = connectionString.split(";");
      for (String part : parts) {
        if (part.startsWith("InstrumentationKey=")) {
          return part.substring("InstrumentationKey=".length());
        }
      }
    }
    return "default-key";
  }
}
