package it.pagopa.selfcare.user.health;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.commons.health.AbstractAsyncReadinessCheck;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.user.service.TenantProductCatalogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.microprofile.health.Readiness;
import lombok.extern.slf4j.Slf4j;

@Readiness
@ApplicationScoped
@Slf4j
public class ProductBlobStorageReadinessCheck extends AbstractAsyncReadinessCheck {
  @Inject TenantRegistry registry;
  @Inject TenantProductCatalogService catalogs;

  @Override protected String checkName() { return "blob-storage-product"; }
  @Override protected Duration timeout() { return Duration.ofSeconds(3); }

  @Override protected Map<String, String> data() {
    return registry.supportedTenantIds().stream().collect(Collectors.toMap(
        tenant -> tenant + ".container", tenant -> registry.storage(tenant, "products").container()));
  }

  @Override protected Uni<?> probe() {
    return Multi.createFrom().iterable(registry.supportedTenantIds())
        .onItem().transformToUniAndMerge(tenant -> Uni.createFrom().item(() ->
            catalogs.blobClient(tenant).getProperties(catalogs.productPath(tenant)))
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .ifNoItem().after(Duration.ofSeconds(2)).fail()
            .onFailure().invoke(failure -> log.warn("Product storage readiness failed for tenant {} ({})", tenant, failure.getClass().getSimpleName()))
            .replaceWith("").onFailure().recoverWithItem(tenant))
        .collect().asList().invoke(results -> {
          var failures = results.stream().filter(tenant -> !tenant.isEmpty()).toList();
          if (!failures.isEmpty()) {
            throw new IllegalStateException("Product storage unavailable for tenants: " + failures);
          }
        });
  }
}
