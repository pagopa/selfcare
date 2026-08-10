package it.pagopa.selfcare.webhook.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.data.tables.models.TableEntity;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SyntheticMonitoringMainTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void parseExpectedCodes_shouldSupportExactValuesAndRanges() {
    List<SyntheticMonitoringMain.StatusRange> ranges =
        SyntheticMonitoringMain.parseExpectedCodes("[\"200\",\"300-303\"]");

    assertThat(ranges).anyMatch(range -> range.includes(200));
    assertThat(ranges).anyMatch(range -> range.includes(302));
    assertThat(ranges).noneMatch(range -> range.includes(500));
  }

  @Test
  void execute_shouldReturnSuccessForExpectedStatus() throws Exception {
    TableEntity check = checkForStatus(204, "[\"200-299\"]");

    SyntheticMonitoringMain.CheckResult result =
        SyntheticMonitoringMain.execute(HttpClient.newHttpClient(), check);

    assertThat(result.success()).isTrue();
    assertThat(result.errorMessage()).isNull();
  }

  @Test
  void execute_shouldReturnFailureForUnexpectedStatus() throws Exception {
    TableEntity check = checkForStatus(503, "[\"200\"]");

    SyntheticMonitoringMain.CheckResult result =
        SyntheticMonitoringMain.execute(HttpClient.newHttpClient(), check);

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("Unexpected HTTP status 503");
  }

  private TableEntity checkForStatus(int status, String expectedCodes) throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/diagnostics",
        exchange -> {
          exchange.sendResponseHeaders(status, -1);
          exchange.close();
        });
    server.start();

    return new TableEntity("webhook-diagnostics", "private")
        .setProperties(
            Map.of(
                "url",
                "http://localhost:" + server.getAddress().getPort() + "/diagnostics",
                "method",
                "GET",
                "expectedCodes",
                expectedCodes,
                "durationLimit",
                "1000"));
  }
}
