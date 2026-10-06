package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.common.InstitutionType;

import java.util.List;

public interface ProductService {

    OriginResult getOrigins(String tenantId, String productId);

    List<RequiredDocumentModel> getRequiredDocuments(String tenantId, String productId, String institutionType, String origin);

    boolean isRequiredDocumentsEnabled(String tenantId, String productId, String institutionType, String origin);

    Product getProduct(String productId, InstitutionType institutionType);

    Product getProductValid(String productId);

    List<Product> getProducts(boolean rootOnly);

    boolean isProductEnabled(String productId);

    boolean verifyAllowedByInstitutionTaxCode(String productId, String institutionTaxCode);

}
