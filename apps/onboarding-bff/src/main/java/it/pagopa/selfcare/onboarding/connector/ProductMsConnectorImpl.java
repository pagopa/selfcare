package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.api.ProductApi;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
public class ProductMsConnectorImpl implements ProductMsConnector {

    // CONNECTOR
    private final ProductApi productApi;

    // MAPPER
    private final ProductMapper productMapper;

    public ProductMsConnectorImpl(@RestClient ProductApi productApi, ProductMapper productMapper) {
        this.productApi = productApi;
        this.productMapper = productMapper;
    }

    @Override
    public OriginResult getOrigins(String productId) {
        log.trace("getOrigins start");
        ProductOriginResponse origins = productApi.getProductOriginsById(productId).await().indefinitely();
        OriginResult entryList = productMapper.toOriginResult(origins);
        log.debug("getOrigins size = {}", entryList.getOrigins().isEmpty());
        log.trace("getOrigins end");
        return entryList;
    }
}
