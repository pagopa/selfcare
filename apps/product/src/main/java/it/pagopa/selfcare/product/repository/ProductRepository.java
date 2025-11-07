package it.pagopa.selfcare.product.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import it.pagopa.selfcare.product.model.Product;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements ReactivePanacheMongoRepositoryBase<Product, String> {
}