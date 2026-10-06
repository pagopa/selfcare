package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;

import java.util.List;

public interface ProductMsConnector {

    OriginResult getOrigins(String tenantId, String productId);

    List<RequiredDocumentModel> getRequiredDocuments(String tenantId, String productId, String institutionType, String origin);

    boolean isRequiredDocumentsEnabled(String tenantId, String productId, String institutionType, String origin);

    Product getProduct(String productId);

    Product getValidProduct(String productId);

    List<Product> getProducts(boolean rootOnly);

    boolean isProductEnabled(String productId);

    boolean isAllowedByInstitutionTaxCode(String productId, String institutionTaxCode);

}
