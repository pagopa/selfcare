package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
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

    @Override
    public List<RequiredDocumentModel> getRequiredDocuments(String productId, String institutionType, String origin) {
        log.trace("getRequiredDocuments start");
        List<RequiredDocumentModel> result = productMsConnector.getRequiredDocuments(
                Encode.forJava(productId),
                Encode.forJava(institutionType),
                Encode.forJava(origin)
        );
        log.debug("getRequiredDocuments size = {}", result.size());
        log.trace("getRequiredDocuments end");
        return result;
    }

    @Override
    public boolean isRequiredDocumentsEnabled(String productId, String institutionType, String origin) {
        log.trace("isRequiredDocumentsEnabled start");
        boolean result = productMsConnector.isRequiredDocumentsEnabled(
                Encode.forJava(productId),
                Encode.forJava(institutionType),
                Encode.forJava(origin)
        );
        log.debug("isRequiredDocumentsEnabled result = {}", result);
        log.trace("isRequiredDocumentsEnabled end");
        return result;
    }
}
