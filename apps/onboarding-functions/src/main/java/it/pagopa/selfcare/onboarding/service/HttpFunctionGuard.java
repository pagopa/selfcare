package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Single entry point that every HTTP-triggered function must call first: it authenticates the
 * request and tracks the invocation with the caller identity taken from the verified JWT.
 */
@ApplicationScoped
public class HttpFunctionGuard {

  private final HttpJwtValidator httpJwtValidator;
  private final FunctionInvocationLogger functionInvocationLogger;

  public HttpFunctionGuard(
      HttpJwtValidator httpJwtValidator, FunctionInvocationLogger functionInvocationLogger) {
    this.httpJwtValidator = httpJwtValidator;
    this.functionInvocationLogger = functionInvocationLogger;
  }

  /**
   * Returns the rejection to send back when the request is not authenticated; otherwise tracks the
   * invocation and returns the verified caller.
   */
  public HttpAuthorization authorize(
      String functionName, HttpRequestMessage<?> request, ExecutionContext context) {
    HttpAuthorization authorization = httpJwtValidator.authenticate(request, context);
    if (!authorization.isRejected()) {
      functionInvocationLogger.logInvocation(functionName, authorization.caller(), request);
    }
    return authorization;
  }
}
