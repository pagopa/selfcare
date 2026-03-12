package it.pagopa.selfcare.product.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.Product;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements ReactivePanacheMongoRepositoryBase<Product, String> {

  public Uni<Product> findProductById(String productId) {
    return find("productId = ?1", Sort.descending("version"), productId).firstResult();
  }
}
