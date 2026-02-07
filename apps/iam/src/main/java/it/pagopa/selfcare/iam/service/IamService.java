package it.pagopa.selfcare.iam.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.model.ProductRolePermissionsList;
import java.util.List;

public interface IamService {
  Uni<String> ping();

  Uni<UserClaims> saveUser(SaveUserRequest saveUserRequest, String productId);

  Uni<UserClaims> getUser(String userId, String productId);

  Uni<List<UserClaims>> getUsers(String productId);

  Uni<ProductRolePermissionsList> getProductRolePermissionsList(String userId, String productId);

  Uni<Boolean> hasPermission(
      String userId, String permission, String productId, String institutionId);
}
