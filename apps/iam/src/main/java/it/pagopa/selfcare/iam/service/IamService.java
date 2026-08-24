package it.pagopa.selfcare.iam.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.model.ProductRole;
import it.pagopa.selfcare.iam.model.ProductRolePermissionsList;
import java.util.List;

public interface IamService {
  Uni<String> ping();

  Uni<UserClaims> saveUser(SaveUserRequest saveUserRequest, String productId, String tenantId);

  Uni<UserClaims> getUser(String userId, String productId, String tenantId);

  Uni<UserClaims> getUserByEmail(String email, String productId, String tenantId);

  Uni<List<UserClaims>> getUsers(String productId, String tenantId);

  Uni<ProductRolePermissionsList> getProductRolePermissionsList(
      String userId, String productId, String tenantId);

  Uni<List<ProductRole>> getProductRoles(String userId, String productId, String tenantId);

  Uni<Boolean> hasPermission(
      String userId, String permission, String productId, String institutionId, String tenantId);

  Uni<List<String>> getInstitutionProducts(String institutionId, String productId);
}
