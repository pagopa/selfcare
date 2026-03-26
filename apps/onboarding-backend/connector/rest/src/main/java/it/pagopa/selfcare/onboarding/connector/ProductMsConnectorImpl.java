package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.rest.client.MsProductApiClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.ProductMapper;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductOriginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProductMsConnectorImpl implements ProductMsConnector {

    // CONNECTOR
    private final MsProductApiClient msProductApiClient;

    // MAPPER
    private final ProductMapper productMapper;

    public ProductMsConnectorImpl(MsProductApiClient msProductApiClient, ProductMapper productMapper) {
        this.msProductApiClient = msProductApiClient;
        this.productMapper = productMapper;
    }

    @Override
    public OriginResult getOrigins(String productId) {
        log.trace("getOrigins start");
        ResponseEntity<ProductOriginResponse> origins = msProductApiClient._getProductOriginsById(productId);
        OriginResult entryList = productMapper.toOriginResult(origins.getBody());
        log.debug("getOrigins size = {}", entryList.getOrigins().isEmpty());
        log.trace("getOrigins end");
        return entryList;
    }
}
