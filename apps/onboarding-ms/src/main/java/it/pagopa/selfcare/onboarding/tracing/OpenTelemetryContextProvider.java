package it.pagopa.selfcare.onboarding.tracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Map;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

/**
 * Propagates the OpenTelemetry context (owned by the Application Insights Java agent) through
 * SmallRye Context Propagation, so Mutiny callbacks run with the context of the request that
 * assembled them.
 *
 * <p>The agent keeps the context in a ThreadLocal and re-attaches it on Netty/Vert.x callbacks:
 * with concurrent requests on the same event loop, continuations of the reactive REST client and
 * Mongo driver could resume with another request's context (or none), so outgoing dependencies got
 * the wrong {@code operation_Id} (SELC-9675).
 */
public class OpenTelemetryContextProvider implements ThreadContextProvider {

  public static final String CONTEXT_TYPE = "OpenTelemetry";

  @Override
  public ThreadContextSnapshot currentContext(Map<String, String> props) {
    return snapshot(Context.current());
  }

  @Override
  public ThreadContextSnapshot clearedContext(Map<String, String> props) {
    return snapshot(Context.root());
  }

  @Override
  public String getThreadContextType() {
    return CONTEXT_TYPE;
  }

  private static ThreadContextSnapshot snapshot(Context captured) {
    return () -> {
      Scope scope = captured.makeCurrent();
      return scope::close;
    };
  }
}
