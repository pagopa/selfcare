package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.client.model.Product;
import it.pagopa.selfcare.onboarding.client.model.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.common.InstitutionType;

import java.util.List;

public interface ProductService {

    Uni<OriginResult> getOrigins(String tenantId, String productId);

    Uni<List<RequiredDocumentModel>> getRequiredDocuments(String tenantId, String productId, String institutionType, String origin);

    Uni<Boolean> isRequiredDocumentsEnabled(String tenantId, String productId, String institutionType, String origin);

    Uni<Product> getProduct(String id, InstitutionType institutionType);

    Uni<Product> getProductValid(String id);

    Uni<List<Product>> getProducts(boolean rootOnly);

    Uni<Boolean> isProductEnabled(String productId);

    Uni<Boolean> verifyAllowedByInstitutionTaxCode(String productId, String institutionTaxCode);

}
