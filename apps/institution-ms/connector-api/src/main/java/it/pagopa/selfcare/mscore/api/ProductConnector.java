package it.pagopa.selfcare.mscore.api;


import it.pagopa.selfcare.product.entity.Product;

public interface ProductConnector {
    Product getProductById(String productId);

    Product getProductValidById(String productId);
}
