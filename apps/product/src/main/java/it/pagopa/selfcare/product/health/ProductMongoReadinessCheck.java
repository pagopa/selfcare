package it.pagopa.selfcare.product.health;

import io.quarkus.arc.Unremovable;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.bson.Document;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@Singleton
@Unremovable
public class ProductMongoReadinessCheck implements AsyncHealthCheck {

  private static final String HOST_NOT_AVAILABLE = "n/a";
  private final TenantMongoClientProducer tenantMongoClientProducer;
  private final TenantRegistry tenantRegistry;
  private final String databaseName;
  private final String host;

  @Inject
  public ProductMongoReadinessCheck(
      TenantRegistry tenantRegistry, TenantMongoClientProducer tenantMongoClientProducer) {
    this.tenantRegistry = tenantRegistry;
    this.tenantMongoClientProducer = tenantMongoClientProducer;

    String firstTenant = tenantRegistry.supportedTenantIds().stream().findFirst().orElse(null);
    if (firstTenant == null) {
      this.databaseName = HOST_NOT_AVAILABLE;
      this.host = HOST_NOT_AVAILABLE;
    } else {
      this.databaseName = tenantRegistry.resolve(firstTenant).mongo().database();
      this.host =
          tenantRegistry
              .connectionString(firstTenant)
              .map(ProductMongoReadinessCheck::hostFromConnectionString)
              .orElse(HOST_NOT_AVAILABLE);
    }
  }

  private String checkName() {
    return "mongodb-product";
  }

  private Uni<?> probe() {
    return io.smallrye.mutiny.Multi.createFrom()
        .iterable(tenantRegistry.supportedTenantIds())
        .onItem()
        .transformToUniAndConcatenate(
            tenantId -> {
              String tenantDatabase = tenantRegistry.resolve(tenantId).mongo().database();
              return tenantMongoClientProducer
                  .clientForTenant(tenantId)
                  .getDatabase(tenantDatabase)
                  .runCommand(new Document("ping", 1));
            })
        .collect()
        .asList()
        .replaceWithVoid();
  }

  @Override
  public Uni<HealthCheckResponse> call() {
    long startNanos = System.nanoTime();
    Duration timeout = Duration.ofSeconds(5);
    return probe()
        .ifNoItem()
        .after(timeout)
        .failWith(() -> new TimeoutException("Readiness probe timed out after " + timeout))
        .onItemOrFailure()
        .transform(
            (item, failure) -> {
              HealthCheckResponseBuilder builder =
                  HealthCheckResponse.named(checkName())
                      .withData("latencyMs", String.valueOf(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)))
                      .withData("component", "mongodb")
                      .withData("database", databaseName)
                      .withData("host", host);
              if (failure == null) {
                return builder.up().build();
              }
              return builder
                  .down()
                  .withData(
                      "error",
                      failure.getClass().getSimpleName()
                          + ": "
                          + (failure.getMessage() == null ? "" : failure.getMessage()))
                  .build();
            });
  }

  private static String hostFromConnectionString(String connectionString) {
    if (connectionString == null || connectionString.isBlank()) {
      return HOST_NOT_AVAILABLE;
    }
    try {
      return String.join(",", new com.mongodb.ConnectionString(connectionString).getHosts());
    } catch (RuntimeException e) {
      return HOST_NOT_AVAILABLE;
    }
  }
}
