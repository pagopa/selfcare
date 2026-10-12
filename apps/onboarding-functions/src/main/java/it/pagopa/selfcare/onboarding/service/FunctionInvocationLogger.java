package it.pagopa.selfcare.onboarding.service;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.azure.functions.HttpRequestMessage;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class FunctionInvocationLogger {

  public static final String USER_ID_HEADER = "x-selfcare-uid";
  public static final String USER_ID_PROPERTY = "userId";
  public static final String UNKNOWN_USER_ID = "unknown";

  private final TelemetryService telemetryService;

  public FunctionInvocationLogger(TelemetryService telemetryService) {
    this.telemetryService = telemetryService;
  }

  public String logInvocation(String functionName, HttpRequestMessage<?> request) {
    String userId = resolveUserId(request);
    boolean missingUserId = isMissingUserId(userId);
    telemetryService.trackFunction(
        functionName,
        missingUserId
            ? functionName + " HTTP trigger received a request without a userId"
            : functionName + " HTTP trigger processed a request",
        missingUserId ? SeverityLevel.Warning : SeverityLevel.Information,
        Map.of(
            "functionName", functionName,
            USER_ID_PROPERTY, userId,
            "missingUserId", Boolean.toString(missingUserId)));
    return userId;
  }

  public static boolean isMissingUserId(String userId) {
    return Objects.isNull(userId) || userId.isBlank() || UNKNOWN_USER_ID.equals(userId);
  }

  public void logOrchestrationStarted(String functionName, String userId, String instanceId) {
    telemetryService.trackFunction(
        functionName,
        functionName + " created orchestration instance " + instanceId,
        SeverityLevel.Information,
        Map.of(
            "functionName", functionName,
            USER_ID_PROPERTY, userId,
            "instanceId", instanceId));
  }

  String resolveUserId(HttpRequestMessage<?> request) {
    if (Objects.isNull(request) || Objects.isNull(request.getHeaders())) {
      return UNKNOWN_USER_ID;
    }

    return request.getHeaders().entrySet().stream()
        .filter(entry -> USER_ID_HEADER.equalsIgnoreCase(entry.getKey()))
        .map(Map.Entry::getValue)
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .findFirst()
        .orElse(UNKNOWN_USER_ID);
  }
}
