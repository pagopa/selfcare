package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    private final ProductApi productApi;
    private final ProductMapper productMapper;

    public ProductServiceImpl(@RestClient ProductApi productApi, ProductMapper productMapper) {
        this.productApi = productApi;
        this.productMapper = productMapper;
    }

    @Override
    public OriginResult getOrigins(String productId) {
        log.trace("getOrigins start");
        String productIdSanitized = Encode.forJava(productId);
        ProductOriginResponse origins = productApi.getProductOriginsById(productIdSanitized).await().indefinitely();
        OriginResult originResult = productMapper.toOriginResult(origins);
        log.debug("getOrigins size = {}", originResult.getOrigins().size());
        log.trace("getOrigins end");
        return originResult;
    }
}
