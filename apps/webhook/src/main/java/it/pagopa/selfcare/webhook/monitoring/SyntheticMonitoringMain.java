package it.pagopa.selfcare.webhook.monitoring;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class SyntheticMonitoringMain {

  private static final AttributeKey<String> CHECK = AttributeKey.stringKey("check");
  private static final AttributeKey<String> LOCATION = AttributeKey.stringKey("location");

  private SyntheticMonitoringMain() {}

  public static void main(String[] args) {
    String storageAccount = requiredEnv("STORAGE_ACCOUNT_NAME");
    String tableName = requiredEnv("STORAGE_ACCOUNT_TABLE_NAME");
    String managedIdentityClientId = requiredEnv("AZURE_CLIENT_ID");

    TableClient tableClient =
        new TableClientBuilder()
            .endpoint("https://" + storageAccount + ".table.core.windows.net")
            .tableName(tableName)
            .credential(
                new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(managedIdentityClientId)
                    .build())
            .buildClient();

    Meter meter = GlobalOpenTelemetry.getMeter("it.pagopa.selfcare.webhook.synthetic-monitoring");
    DoubleHistogram health =
        meter
            .histogramBuilder("webhook.synthetic.health")
            .setDescription("Health of webhook synthetic checks: 1 for success, 0 for failure")
            .build();
    DoubleHistogram duration =
        meter
            .histogramBuilder("webhook.synthetic.duration")
            .setUnit("ms")
            .setDescription("Duration of webhook synthetic checks")
            .build();

    boolean allSuccessful = true;
    int checkCount = 0;
    HttpClient httpClient = HttpClient.newHttpClient();
    for (TableEntity check : tableClient.listEntities()) {
      checkCount++;
      CheckResult result = execute(httpClient, check);
      Attributes attributes =
          Attributes.of(
              CHECK,
              check.getPartitionKey(),
              LOCATION,
              check.getRowKey());
      health.record(result.success() ? 1 : 0, attributes);
      duration.record(result.durationMs(), attributes);
      allSuccessful &= result.success();
      if (!result.success()) {
        System.err.println(
            "Synthetic check "
                + check.getPartitionKey()
                + " failed: "
                + result.errorMessage());
      }
    }

    if (checkCount == 0) {
      throw new IllegalStateException("No synthetic monitoring checks were configured");
    }

    waitForMetricExport();

    if (!allSuccessful) {
      throw new IllegalStateException("One or more synthetic monitoring checks failed");
    }
  }

  static CheckResult execute(HttpClient httpClient, TableEntity check) {
    Map<String, Object> properties = check.getProperties();
    String url = requiredProperty(properties, "url");
    String method = String.valueOf(properties.getOrDefault("method", "GET"));
    long timeoutMs = Long.parseLong(String.valueOf(properties.getOrDefault("durationLimit", "5000")));
    List<StatusRange> expectedCodes =
        parseExpectedCodes(requiredProperty(properties, "expectedCodes"));
    long startedAt = System.nanoTime();

    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(Duration.ofMillis(timeoutMs))
              .method(method, HttpRequest.BodyPublishers.noBody())
              .build();
      HttpResponse<Void> response =
          httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      boolean success =
          expectedCodes.stream().anyMatch(range -> range.includes(response.statusCode()));
      return new CheckResult(
          success,
          elapsedMillis(startedAt),
          success ? null : "Unexpected HTTP status " + response.statusCode());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new CheckResult(false, elapsedMillis(startedAt), e.getMessage());
    } catch (Exception e) {
      return new CheckResult(false, elapsedMillis(startedAt), e.getMessage());
    }
  }

  static List<StatusRange> parseExpectedCodes(String json) {
    String content = json.trim();
    if (!content.startsWith("[") || !content.endsWith("]")) {
      throw new IllegalArgumentException("expectedCodes must be a JSON array");
    }
    String entries = content.substring(1, content.length() - 1).trim();
    if (entries.isEmpty()) {
      return List.of();
    }
    return Arrays.stream(entries.split(","))
        .map(String::trim)
        .map(value -> value.replace("\"", ""))
        .map(
            value -> {
              String[] bounds = value.split("-", 2);
              int start = Integer.parseInt(bounds[0]);
              int end = bounds.length == 2 ? Integer.parseInt(bounds[1]) : start;
              return new StatusRange(start, end);
            })
        .toList();
  }

  private static void waitForMetricExport() {
    String configuredWait = System.getenv().getOrDefault("METRIC_EXPORT_WAIT_MS", "6000");
    long waitMs = Long.parseLong(configuredWait);
    if (waitMs <= 0) {
      return;
    }
    try {
      Thread.sleep(waitMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for metric export", e);
    }
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing required environment variable: " + name);
    }
    return value;
  }

  private static String requiredProperty(Map<String, Object> properties, String name) {
    Object value = properties.get(name);
    if (value == null || value.toString().isBlank()) {
      throw new IllegalStateException("Missing required monitoring property: " + name);
    }
    return value.toString();
  }

  private static long elapsedMillis(long startedAt) {
    return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
  }

  record CheckResult(boolean success, long durationMs, String errorMessage) {}

  record StatusRange(int start, int end) {
    boolean includes(int status) {
      return status >= start && status <= end;
    }
  }
}
