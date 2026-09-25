package it.pagopa.selfcare.product.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.model.dto.request.ProductCreateRequest;
import it.pagopa.selfcare.product.model.dto.request.ProductPatchRequest;
import it.pagopa.selfcare.product.model.dto.response.ProductBaseResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductExpirationResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductOriginResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductResponse;
import it.pagopa.selfcare.product.model.dto.response.ProductRoleResponse;
import it.pagopa.selfcare.product.model.dto.response.RequiredDocumentResponse;
import it.pagopa.selfcare.product.model.dto.response.WorkflowTypeResponse;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.UserRole;
import java.util.List;

public interface ProductService {

  Uni<String> ping();

  Uni<ProductBaseResponse> createProduct(ProductCreateRequest product, String createdBy);

  Uni<ProductResponse> getProduct(String tenantId, String productId);

  Uni<ProductBaseResponse> deleteProduct(String tenantId, String productId);

  Uni<ProductResponse> patchProductById(
      String tenantId, String productId, String createdBy, ProductPatchRequest productPatchRequest);

  Uni<ProductOriginResponse> getProductOrigins(String tenantId, String productId);

  Uni<WorkflowTypeResponse> getWorkflowType(
      String tenantId, String productId, InstitutionType institutionType, Origin origin);

  Uni<Boolean> isRequiredDocumentsEnabled(
      String tenantId, String productId, InstitutionType institutionType, Origin origin);

  Uni<List<RequiredDocumentResponse>> getRequiredDocuments(
      String tenantId, String productId, InstitutionType institutionType, Origin origin);

  /**
   * Returns the product only if it is "valid" for onboarding purposes, i.e. neither the product nor
   * its parent (when present) is in a not-valid status ({@code INACTIVE}, {@code DELETED} or {@code
   * PHASE_OUT}).
   *
   * @param productId the product identifier
   * @return the {@link ProductResponse} when valid; fails with {@link
   *     jakarta.ws.rs.NotFoundException} when the product/parent does not exist or is not valid
   */
  Uni<ProductResponse> getValidProduct(String tenantId, String productId);

  /**
   * Returns the expiration days associated with the given product. If the product exists and is
   * valid, its {@code features.expirationDays} is returned; otherwise the default value (30) is
   * returned.
   *
   * @param productId the product identifier
   * @return the {@link ProductExpirationResponse} holding the expiration days
   */
  Uni<ProductExpirationResponse> getProductExpirationDays(String tenantId, String productId);

  /**
   * Returns the list of products (one entry per {@code productId}, latest version).
   *
   * @param rootOnly when {@code true} only products without a parent are returned
   * @param valid when {@code true} products in a not-valid status ({@code INACTIVE}, {@code
   *     PHASE_OUT} or {@code DELETED}) are excluded
   * @return the list of {@link ProductResponse}
   */
  Uni<List<ProductResponse>> getProducts(String tenantId, boolean rootOnly, boolean valid);

  /**
   * Validates that the given {@code productRole} (product-specific role code) exists for the given
   * {@code role} ({@link UserRole}) within the product configuration and returns it.
   *
   * @param productId the product identifier
   * @param role the Selfcare user role (party role) acting as key on the role mappings
   * @param productRole the product-specific role code to validate
   * @return the matching {@link ProductRoleResponse}; fails with {@link
   *     jakarta.ws.rs.NotFoundException} when product, role or productRole is not found
   */
  Uni<ProductRoleResponse> validateProductRole(
      String tenantId, String productId, UserRole role, String productRole);
}
