package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import org.junit.jupiter.api.Test;

class HttpFunctionGuardTest {

  private final HttpJwtValidator httpJwtValidator = mock(HttpJwtValidator.class);
  private final FunctionInvocationLogger functionInvocationLogger =
      mock(FunctionInvocationLogger.class);
  private final HttpFunctionGuard guard =
      new HttpFunctionGuard(httpJwtValidator, functionInvocationLogger);

  private final HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
  private final ExecutionContext context = mock(ExecutionContext.class);

  @Test
  void authorizeReturnsRejectionWithoutTrackingInvocation() {
    // given
    HttpResponseMessage unauthorized = mock(HttpResponseMessage.class);
    when(httpJwtValidator.authenticate(request, context))
        .thenReturn(HttpAuthorization.rejected(unauthorized));

    // when
    HttpAuthorization authorization = guard.authorize("TestFunction", request, context);

    // then
    assertTrue(authorization.isRejected());
    assertSame(unauthorized, authorization.rejection());
    verifyNoInteractions(functionInvocationLogger);
  }

  @Test
  void authorizeTracksInvocationWithVerifiedCaller() {
    // given
    CallerIdentity caller = new CallerIdentity("token-uid", "SPID", "apim", null, null, null, null);
    when(httpJwtValidator.authenticate(request, context))
        .thenReturn(HttpAuthorization.authorized(caller));

    // when
    HttpAuthorization authorization = guard.authorize("TestFunction", request, context);

    // then
    assertFalse(authorization.isRejected());
    assertSame(caller, authorization.caller());
    verify(functionInvocationLogger).logInvocation("TestFunction", caller, request);
  }
}
