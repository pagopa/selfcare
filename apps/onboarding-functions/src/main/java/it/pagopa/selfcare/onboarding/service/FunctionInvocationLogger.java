package it.pagopa.selfcare.onboarding.service;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.azure.functions.HttpRequestMessage;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class FunctionInvocationLogger {

  /**
   * Header historically used to declare the caller. It is not trusted: the caller is taken from the
   * verified JWT and the header value is only tracked as {@link #DECLARED_USER_ID_PROPERTY}.
   */
  public static final String USER_ID_HEADER = "x-selfcare-uid";
  public static final String USER_ID_PROPERTY = "userId";
  static final String DECLARED_USER_ID_PROPERTY = "declaredUserId";

  private final TelemetryService telemetryService;

  public FunctionInvocationLogger(TelemetryService telemetryService) {
    this.telemetryService = telemetryService;
  }

  public void logInvocation(
      String functionName, CallerIdentity caller, HttpRequestMessage<?> request) {
    Map<String, String> properties = new HashMap<>();
    properties.put("functionName", functionName);
    properties.put(USER_ID_PROPERTY, caller.userId());
    putIfPresent(properties, "tenant", caller.tenant());
    putIfPresent(properties, "tenantSource", caller.tenantSource());
    putIfPresent(properties, "issuer", caller.issuer());
    putIfPresent(properties, "clientId", caller.clientId());
    putIfPresent(properties, "subscriptionId", caller.subscriptionId());
    putIfPresent(properties, "productId", caller.productId());
    String declaredUserId = resolveDeclaredUserId(request);
    if (declaredUserId != null) {
      properties.put(DECLARED_USER_ID_PROPERTY, declaredUserId);
      properties.put(
          "declaredUserIdMismatch", Boolean.toString(!declaredUserId.equals(caller.userId())));
    }
    telemetryService.trackFunction(
        functionName,
        functionName + " HTTP trigger processed a request",
        SeverityLevel.Information,
        properties);
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

  private static void putIfPresent(Map<String, String> properties, String key, String value) {
    if (value != null) {
      properties.put(key, value);
    }
  }

  private static String resolveDeclaredUserId(HttpRequestMessage<?> request) {
    if (Objects.isNull(request) || Objects.isNull(request.getHeaders())) {
      return null;
    }
    return request.getHeaders().entrySet().stream()
        .filter(entry -> USER_ID_HEADER.equalsIgnoreCase(entry.getKey()))
        .map(Map.Entry::getValue)
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .findFirst()
        .orElse(null);
  }
}
