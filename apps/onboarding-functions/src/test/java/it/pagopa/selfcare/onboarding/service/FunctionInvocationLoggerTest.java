package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.azure.functions.HttpRequestMessage;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FunctionInvocationLoggerTest {

  private final TelemetryService telemetryService = mock(TelemetryService.class);
  private final FunctionInvocationLogger functionInvocationLogger =
      new FunctionInvocationLogger(telemetryService);

  @Test
  void logInvocationTracksRequesterUserId() {
    // given
    HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(Map.of("X-SelfCare-Uid", " requester-id "));

    // when
    String userId = functionInvocationLogger.logInvocation("TestFunction", request);

    // then
    assertEquals("requester-id", userId);
    verify(telemetryService)
        .trackFunction(
            "TestFunction",
            "TestFunction HTTP trigger processed a request",
            SeverityLevel.Information,
            Map.of(
                "functionName", "TestFunction",
                "userId", "requester-id",
                "missingUserId", "false"));
  }

  @Test
  void logInvocationTracksUnknownWhenRequesterHeaderIsBlank() {
    // given
    HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(Map.of("x-selfcare-uid", " "));

    // when
    String userId = functionInvocationLogger.logInvocation("TestFunction", request);

    // then
    assertEquals(FunctionInvocationLogger.UNKNOWN_USER_ID, userId);
    verify(telemetryService)
        .trackFunction(
            "TestFunction",
            "TestFunction HTTP trigger received a request without a userId",
            SeverityLevel.Warning,
            Map.of(
                "functionName", "TestFunction",
                "userId", "unknown",
                "missingUserId", "true"));
  }

  @Test
  void logInvocationTracksUnknownWhenHeadersAreMissing() {
    // given
    HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(null);

    // when
    String userId = functionInvocationLogger.logInvocation("TestFunction", request);

    // then
    assertEquals(FunctionInvocationLogger.UNKNOWN_USER_ID, userId);
    verify(telemetryService)
        .trackFunction(
            "TestFunction",
            "TestFunction HTTP trigger received a request without a userId",
            SeverityLevel.Warning,
            Map.of(
                "functionName", "TestFunction",
                "userId", "unknown",
                "missingUserId", "true"));
  }

  @Test
  void logOrchestrationStartedTracksUserAndInstance() {
    // given
    String functionName = "TestFunction";
    String userId = "requester-id";
    String instanceId = "instance-id";

    // when
    functionInvocationLogger.logOrchestrationStarted(functionName, userId, instanceId);

    // then
    verify(telemetryService)
        .trackFunction(
            functionName,
            "TestFunction created orchestration instance instance-id",
            SeverityLevel.Information,
            Map.of(
                "functionName", functionName,
                "userId", userId,
                "instanceId", instanceId));
  }
}
