package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;

public interface ProductService {
    Uni<String> ping();
}
