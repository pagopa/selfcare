package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.ProductId;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.OnboardingDecisionRequest;
import org.openapi.quarkus.product_json.model.OnboardingDecisionResponse;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RequiredDocumentResponse;

import java.util.List;

public interface ProductService {

  /**
   * Asks product-ms whether an institution can onboard a product through a channel. product-ms is the only
   * owner of the admission rules: a denied onboarding is returned as an item with {@code allowed=false}.
   *
   * @throws it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException (as failure) when the product,
   *         or its parent, does not exist or is not valid
   */
  Uni<OnboardingDecisionResponse> getOnboardingDecision(String productId, OnboardingDecisionRequest request,
                                                        String tenantId);

  Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId, InstitutionType institutionType, Origin origin);

  default Uni<List<RequiredDocumentResponse>> getRequiredDocuments(ProductId productId,
                                                                    InstitutionType institutionType,
                                                                    Origin origin, String tenantId) {
    return getRequiredDocuments(productId, institutionType, origin);
  }

  Uni<ProductResponse> getValidProduct(String productId);

  Uni<ProductResponse> getValidProduct(String productId, String tenantId);

  Uni<Integer> getProductExpirationDays(String productId);

  default Uni<Integer> getProductExpirationDays(String productId, String tenantId) {
    return getProductExpirationDays(productId);
  }
}
