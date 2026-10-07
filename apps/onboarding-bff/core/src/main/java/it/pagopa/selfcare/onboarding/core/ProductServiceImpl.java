package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.ProductMsConnector;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.connector.model.product.ProductStatus;
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
    public OriginResult getOrigins(String tenantId, String productId) {
        log.trace("getOrigins start");
        String productIdSanitized = Encode.forJava(productId);
        OriginResult originResult = productMsConnector.getOrigins(Encode.forJava(tenantId), productIdSanitized);
        log.debug("getOrigins size = {}", originResult.getOrigins().size());
        log.trace("getOrigins end");
        return originResult;
    }

    @Override
    public List<RequiredDocumentModel> getRequiredDocuments(String tenantId, String productId, String institutionType, String origin) {
        log.trace("getRequiredDocuments start");
        List<RequiredDocumentModel> result = productMsConnector.getRequiredDocuments(
                Encode.forJava(tenantId),
                Encode.forJava(productId),
                Encode.forJava(institutionType),
                Encode.forJava(origin)
        );
        log.debug("getRequiredDocuments size = {}", result.size());
        log.trace("getRequiredDocuments end");
        return result;
    }

    @Override
    public boolean isRequiredDocumentsEnabled(String tenantId, String productId, String institutionType, String origin) {
        log.trace("isRequiredDocumentsEnabled start");
        boolean result = productMsConnector.isRequiredDocumentsEnabled(
                Encode.forJava(tenantId),
                Encode.forJava(productId),
                Encode.forJava(institutionType),
                Encode.forJava(origin)
        );
        log.debug("isRequiredDocumentsEnabled result = {}", result);
        log.trace("isRequiredDocumentsEnabled end");
        return result;
    }

    @Override
    public Product getProduct(String productId, InstitutionType institutionType) {
        log.trace("getProduct start");
        Product result = productMsConnector.getProduct(Encode.forJava(productId));
        log.trace("getProduct end");
        return result;
    }

    @Override
    public Product getProductValid(String productId) {
        log.trace("getProductValid start");
        Product result = productMsConnector.getValidProduct(Encode.forJava(productId));
        log.trace("getProductValid end");
        return result;
    }

    @Override
    public List<Product> getProducts(boolean rootOnly) {
        log.trace("getProducts start");
        List<Product> result = productMsConnector.getProducts(rootOnly).stream()
                .filter(product -> ProductStatus.ACTIVE.equals(product.getStatus()))
                .toList();
        log.debug("getProducts size = {}", result.size());
        log.trace("getProducts end");
        return result;
    }

    @Override
    public boolean isProductEnabled(String productId) {
        return productMsConnector.isProductEnabled(Encode.forJava(productId));
    }

    @Override
    public boolean verifyAllowedByInstitutionTaxCode(String productId, String institutionTaxCode) {
        return productMsConnector.isAllowedByInstitutionTaxCode(
                Encode.forJava(productId), Encode.forJava(institutionTaxCode));
    }
}
