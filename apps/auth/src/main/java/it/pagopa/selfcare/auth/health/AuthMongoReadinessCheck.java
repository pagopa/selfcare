package it.pagopa.selfcare.auth.health;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractMongoReadinessCheck;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.bson.Document;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class AuthMongoReadinessCheck extends AbstractMongoReadinessCheck {

  private final TenantRegistry tenantRegistry;
  private final TenantMongoClientProducer tenantMongoClientProducer;
  private final List<String> supportedTenantIds;
  private final String databaseName;
  private final String host;

  @Inject
  public AuthMongoReadinessCheck(
      TenantRegistry tenantRegistry, TenantMongoClientProducer tenantMongoClientProducer) {
    this.tenantRegistry = tenantRegistry;
    this.tenantMongoClientProducer = tenantMongoClientProducer;
    supportedTenantIds = tenantRegistry.supportedTenantIds().stream().sorted().toList();

    if (supportedTenantIds.isEmpty()) {
      databaseName = HOST_NOT_AVAILABLE;
      host = HOST_NOT_AVAILABLE;
    } else {
      String firstTenant = supportedTenantIds.get(0);
      databaseName = tenantRegistry.resolve(firstTenant).mongo().database();
      host =
          tenantRegistry
              .connectionString(firstTenant)
              .map(AuthMongoReadinessCheck::hostFromConnectionString)
              .orElse(HOST_NOT_AVAILABLE);
    }
  }

  @Override
  protected String checkName() {
    return "mongodb-auth";
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
    if (supportedTenantIds.isEmpty()) {
      return Uni.createFrom()
          .failure(new IllegalStateException("No supported Mongo tenants are configured"));
    }

    return Multi.createFrom()
        .iterable(supportedTenantIds)
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
