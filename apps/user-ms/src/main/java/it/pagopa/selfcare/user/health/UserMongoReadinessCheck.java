package it.pagopa.selfcare.user.health;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.commons.health.AbstractAsyncReadinessCheck;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.mongodb.TenantMongoClientProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.eclipse.microprofile.health.Readiness;
import lombok.extern.slf4j.Slf4j;

@Readiness
@ApplicationScoped
@Slf4j
public class UserMongoReadinessCheck extends AbstractAsyncReadinessCheck {
  @Inject TenantRegistry registry;
  @Inject TenantMongoClientProducer clients;

  @Override protected String checkName() { return "mongodb-user"; }
  @Override protected Duration timeout() { return Duration.ofSeconds(3); }

  @Override protected Map<String, String> data() {
    return registry.supportedTenantIds().stream().collect(Collectors.toMap(
        tenant -> tenant + ".database", tenant -> registry.resolve(tenant).mongo().database()));
  }

  @Override protected Uni<?> probe() {
    return Multi.createFrom().iterable(registry.supportedTenantIds())
        .onItem().transformToUniAndMerge(tenant -> Uni.createFrom().deferred(() ->
            clients.clientForTenant(tenant).getDatabase(registry.resolve(tenant).mongo().database())
                .runCommand(new Document("ping", 1)))
            .ifNoItem().after(Duration.ofSeconds(2)).fail()
            .onFailure().invoke(failure -> log.warn("Mongo readiness failed for tenant {} ({})", tenant, failure.getClass().getSimpleName()))
            .replaceWith("").onFailure().recoverWithItem(tenant))
        .collect().asList().invoke(results -> {
          var failures = results.stream().filter(tenant -> !tenant.isEmpty()).toList();
          if (!failures.isEmpty()) {
            throw new IllegalStateException("Mongo unavailable for tenants: " + failures);
          }
        });
  }
}
