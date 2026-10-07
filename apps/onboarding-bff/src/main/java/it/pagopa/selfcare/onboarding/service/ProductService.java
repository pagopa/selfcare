package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.common.InstitutionType;

import java.util.List;

public interface ProductService {

    OriginResult getOrigins(String tenantId, String productId);

    List<RequiredDocumentModel> getRequiredDocuments(String tenantId, String productId, String institutionType, String origin);

    boolean isRequiredDocumentsEnabled(String tenantId, String productId, String institutionType, String origin);

    Product getProduct(String id, InstitutionType institutionType);

    Product getProductValid(String id);

    List<Product> getProducts(boolean rootOnly);

    boolean isProductEnabled(String productId);

    boolean verifyAllowedByInstitutionTaxCode(String productId, String institutionTaxCode);

}
