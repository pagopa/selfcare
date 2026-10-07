package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpringReferenceTest {

  private HttpServer server;
  private Process process;
  private SpringReference reference;
  private volatile int healthStatus;

  @BeforeEach
  void start() throws Exception {
    process = mock(Process.class);
    when(process.isAlive()).thenReturn(true);
    when(process.waitFor(15, TimeUnit.SECONDS)).thenReturn(true);
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/actuator/health",
        exchange -> {
          exchange.sendResponseHeaders(healthStatus, -1);
          exchange.close();
        });
    server.start();
    reference = new SpringReference(process, server.getAddress().getPort());
  }

  @AfterEach
  void stop() {
    reference.close();
    server.stop(0);
  }

  @Test
  void aHealthyReferenceIsReadyWithoutStoppingTheProcess() {
    healthStatus = 200;

    assertDoesNotThrow(() -> reference.awaitReady(Duration.ofSeconds(2)));

    verify(process, never()).destroy();
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 503})
  void anErrorResponseDoesNotCountAsReadiness(int status) {
    healthStatus = status;

    assertThrows(IllegalStateException.class, () -> reference.awaitReady(Duration.ofMillis(50)));

    verify(process).destroy();
  }

  @Test
  void anExitedReferenceReportsTheFailureAndCleansUp() {
    when(process.isAlive()).thenReturn(false);
    when(process.exitValue()).thenReturn(7);

    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> reference.awaitReady(Duration.ofSeconds(2)));

    assertTrue(error.getMessage().contains("exited with 7"));
    verify(process).destroy();
  }

  @Test
  void interruptedStartupStillStopsTheReference() {
    healthStatus = 200;
    Thread.currentThread().interrupt();
    try {
      assertThrows(InterruptedException.class, () -> reference.awaitReady(Duration.ofSeconds(2)));
    } finally {
      Thread.interrupted();
    }

    verify(process).destroy();
  }
}
