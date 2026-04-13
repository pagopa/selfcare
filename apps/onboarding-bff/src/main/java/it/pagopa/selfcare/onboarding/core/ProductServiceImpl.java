package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

@Slf4j
@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    // CONNECTOR
    private final ProductMsConnector productMsConnector;

    public ProductServiceImpl(ProductMsConnector productMsConnector) {
        this.productMsConnector = productMsConnector;
    }

    @Override
    public OriginResult getOrigins(String productId) {
        log.trace("getOrigins start");
        String productIdSanitized = Encode.forJava(productId);
        OriginResult originResult = productMsConnector.getOrigins(productIdSanitized);
        log.debug("getOrigins size = {}", originResult.getOrigins().size());
        log.trace("getOrigins end");
        return originResult;
    }
}
