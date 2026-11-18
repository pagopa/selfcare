package it.pagopa.selfcare.product.health;

import io.quarkus.arc.Unremovable;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Set;
import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@Singleton
@Unremovable
public class ProductCdcMongoReadinessCheck implements AsyncHealthCheck {

  private final TenantRegistry tenantRegistry;
  private final TenantMongoClientProducer mongoClientProducer;

  @Inject
  public ProductCdcMongoReadinessCheck(
      TenantRegistry tenantRegistry, TenantMongoClientProducer mongoClientProducer) {
    this.tenantRegistry = tenantRegistry;
    this.mongoClientProducer = mongoClientProducer;
  }

  private Uni<Void> probe() {
    Set<String> tenantIds = tenantRegistry.supportedTenantIds();
    if (tenantIds.isEmpty()) {
      return Uni.createFrom().failure(new IllegalStateException("No configured tenants"));
    }
    return Multi.createFrom()
        .iterable(tenantIds)
        .onItem()
        .transformToUniAndConcatenate(
            tenantId ->
                mongoClientProducer
                    .clientForTenant(tenantId)
                    .getDatabase(tenantRegistry.resolve(tenantId).mongo().database())
                    .runCommand(new Document("ping", 1)))
        .collect()
        .asList()
        .replaceWithVoid();
  }

  @Override
  public Uni<HealthCheckResponse> call() {
    return Uni.createFrom()
        .deferred(this::probe)
        .ifNoItem()
        .after(Duration.ofSeconds(5))
        .fail()
        .onItemOrFailure()
        .transform(
            (ignored, failure) -> {
              HealthCheckResponseBuilder response =
                  HealthCheckResponse.named("mongodb-product-cdc").withData("component", "mongodb");
              return failure == null
                  ? response.up().build()
                  : response.down().withData("error", failure.getClass().getSimpleName()).build();
            });
  }
}
