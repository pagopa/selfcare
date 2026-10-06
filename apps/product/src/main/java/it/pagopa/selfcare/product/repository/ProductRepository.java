package it.pagopa.selfcare.product.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductRepository implements ReactivePanacheMongoRepositoryBase<Product, String> {

  @Inject TenantContext tenantContext;

  public Uni<Product> findProductById(String productId) {
    return find(
            "tenantId = ?1 and productId = ?2",
            Sort.descending("version"),
            tenantContext.requiredTenantId(),
            productId)
        .firstResult();
  }

  /** Returns the latest version of each product (one entry per {@code productId}). */
  public Uni<List<Product>> findLatestVersionForEachProduct() {
    return list("tenantId", tenantContext.requiredTenantId())
        .map(
            products ->
                products.stream()
                    .collect(Collectors.groupingBy(Product::getProductId))
                    .values()
                    .stream()
                    .map(
                        versions ->
                            versions.stream()
                                .max(Comparator.comparingInt(Product::getVersion))
                                .orElseThrow())
                    .toList());
  }
}
