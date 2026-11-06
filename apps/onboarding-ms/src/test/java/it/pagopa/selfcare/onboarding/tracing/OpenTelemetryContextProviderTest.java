package it.pagopa.selfcare.onboarding.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import java.util.Map;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.junit.jupiter.api.Test;

class OpenTelemetryContextProviderTest {

  private static final ContextKey<String> KEY = ContextKey.named("operation");

  private final OpenTelemetryContextProvider provider = new OpenTelemetryContextProvider();

  @Test
  void currentContextIsRestoredInSnapshotAndPreviousContextAfterEnd() {
    ThreadContextSnapshot snapshot;
    try (Scope ignored = Context.root().with(KEY, "request-A").makeCurrent()) {
      snapshot = provider.currentContext(Map.of());
    }

    try (Scope ignored = Context.root().with(KEY, "request-B").makeCurrent()) {
      ThreadContextController controller = snapshot.begin();
      assertEquals("request-A", Context.current().get(KEY));
      controller.endContext();
      assertEquals("request-B", Context.current().get(KEY));
    }
  }

  @Test
  void clearedContextRunsWithRootContext() {
    try (Scope ignored = Context.root().with(KEY, "request-A").makeCurrent()) {
      ThreadContextController controller = provider.clearedContext(Map.of()).begin();
      assertNull(Context.current().get(KEY));
      controller.endContext();
      assertEquals("request-A", Context.current().get(KEY));
    }
  }
}
