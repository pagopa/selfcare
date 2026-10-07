package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;
import org.openapi.quarkus.product_json.model.WorkflowTypeResponse;

import java.util.List;

public interface ProductService {
  Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin, ProductId productId);

  default Uni<WorkflowTypeResponse> getWorkflowType(InstitutionType institutionType, Origin origin,
                                                    ProductId productId, String tenantId) {
    return getWorkflowType(institutionType, origin, productId);
  }

  Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin);

  default Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId,
                                                                    InstitutionType institutionType,
                                                                    Origin origin, String tenantId) {
    return getRequiredDocuments(productId, institutionType, origin);
  }

  Uni<Boolean> isRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin);

  default Uni<Boolean> isRequiredDocuments(ProductId productId, InstitutionType institutionType,
                                           Origin origin, String tenantId) {
    return isRequiredDocuments(productId, institutionType, origin);
  }

  Uni<ProductResponse> getProduct(String productId);

  default Uni<ProductResponse> getProduct(String productId, String tenantId) {
    return getProduct(productId);
  }

  Uni<ProductResponse> getValidProduct(String productId);

  Uni<ProductResponse> getValidProduct(String productId, String tenantId);

  Uni<Integer> getProductExpirationDays(String productId);

  default Uni<Integer> getProductExpirationDays(String productId, String tenantId) {
    return getProductExpirationDays(productId);
  }
}
