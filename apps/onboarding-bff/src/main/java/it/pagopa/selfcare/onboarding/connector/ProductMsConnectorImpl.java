package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsProductApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.product_json.model.ProductOriginResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
public class ProductMsConnectorImpl implements ProductMsConnector {

    // CONNECTOR
    private final MsProductApiClient msProductApiClient;

    // MAPPER
    private final ProductMapper productMapper;

    public ProductMsConnectorImpl(@RestClient MsProductApiClient msProductApiClient, ProductMapper productMapper) {
        this.msProductApiClient = msProductApiClient;
        this.productMapper = productMapper;
    }

    @Override
    public OriginResult getOrigins(String productId) {
        log.trace("getOrigins start");
        ProductOriginResponse origins = msProductApiClient._getProductOriginsById(productId);
        OriginResult entryList = productMapper.toOriginResult(origins);
        log.debug("getOrigins size = {}", entryList.getOrigins().isEmpty());
        log.trace("getOrigins end");
        return entryList;
    }
}
