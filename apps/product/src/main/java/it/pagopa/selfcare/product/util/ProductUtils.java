package it.pagopa.selfcare.product.util;

import it.pagopa.selfcare.product.model.ProductMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ProductUtils {

    public ProductMetadata buildProductMetadata(String createdBy) {
        return ProductMetadata.builder().createdAt(Instant.now()).createdBy(createdBy).build();
    }
}
