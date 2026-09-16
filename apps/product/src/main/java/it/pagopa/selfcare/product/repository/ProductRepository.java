package it.pagopa.selfcare.product.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.Product;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductRepository implements ReactivePanacheMongoRepositoryBase<Product, String> {

  public Uni<Product> findProductById(String productId) {
    return find("productId = ?1", Sort.descending("version"), productId).firstResult();
  }

  /**
   * Returns the latest version of each product (one entry per {@code productId}).
   */
  public Uni<List<Product>> findLatestVersionForEachProduct() {
    return listAll()
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
                    .collect(Collectors.toList()));
  }
}
