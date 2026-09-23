package it.pagopa.selfcare.product.health;

import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractMongoReadinessCheck;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@Singleton
@Unremovable
public class ProductMongoReadinessCheck extends AbstractMongoReadinessCheck {

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
              .map(AbstractMongoReadinessCheck::hostFromConnectionString)
              .orElse(HOST_NOT_AVAILABLE);
    }
  }

  @Override
  protected String checkName() {
    return "mongodb-product";
  }

  @Override
  protected String databaseName() {
    return databaseName;
  }

  @Override
  protected String host() {
    return host;
  }

  @Override
  protected Uni<?> probe() {
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
}
