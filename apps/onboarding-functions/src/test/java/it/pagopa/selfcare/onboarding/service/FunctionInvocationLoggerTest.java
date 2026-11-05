package it.pagopa.selfcare.onboarding.service;

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
  void logInvocationTracksUserIdFromVerifiedToken() {
    // given
    HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(Map.of());
    CallerIdentity caller = new CallerIdentity("token-uid", "SPID", null, null, null);

    // when
    functionInvocationLogger.logInvocation("TestFunction", caller, request);

    // then
    verify(telemetryService)
        .trackFunction(
            "TestFunction",
            "TestFunction HTTP trigger processed a request",
            SeverityLevel.Information,
            Map.of("functionName", "TestFunction", "userId", "token-uid", "issuer", "SPID"));
  }

  @Test
  void logInvocationIgnoresDeclaredHeaderAsIdentityAndFlagsMismatch() {
    // given
    HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(Map.of("X-SelfCare-Uid", " pippo "));
    CallerIdentity caller =
        new CallerIdentity("token-uid", "SPID", "apim", "subscription-id", "product-id");

    // when
    functionInvocationLogger.logInvocation("TestFunction", caller, request);

    // then
    verify(telemetryService)
        .trackFunction(
            "TestFunction",
            "TestFunction HTTP trigger processed a request",
            SeverityLevel.Information,
            Map.of(
                "functionName", "TestFunction",
                "userId", "token-uid",
                "issuer", "SPID",
                "clientId", "apim",
                "subscriptionId", "subscription-id",
                "productId", "product-id",
                "declaredUserId", "pippo",
                "declaredUserIdMismatch", "true"));
  }

  @Test
  void logInvocationTracksMatchingDeclaredHeader() {
    // given
    HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(Map.of("x-selfcare-uid", "token-uid"));
    CallerIdentity caller = new CallerIdentity("token-uid", null, null, null, null);

    // when
    functionInvocationLogger.logInvocation("TestFunction", caller, request);

    // then
    verify(telemetryService)
        .trackFunction(
            "TestFunction",
            "TestFunction HTTP trigger processed a request",
            SeverityLevel.Information,
            Map.of(
                "functionName", "TestFunction",
                "userId", "token-uid",
                "declaredUserId", "token-uid",
                "declaredUserIdMismatch", "false"));
  }

  @Test
  void logInvocationHandlesMissingHeaders() {
    // given
    HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(null);
    CallerIdentity caller = new CallerIdentity("token-uid", null, null, null, null);

    // when
    functionInvocationLogger.logInvocation("TestFunction", caller, request);

    // then
    verify(telemetryService)
        .trackFunction(
            "TestFunction",
            "TestFunction HTTP trigger processed a request",
            SeverityLevel.Information,
            Map.of("functionName", "TestFunction", "userId", "token-uid"));
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
