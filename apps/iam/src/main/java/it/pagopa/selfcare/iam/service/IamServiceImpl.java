package it.pagopa.selfcare.iam.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.model.ProductRolePermissionsList;
import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.iam.repository.UserPermissionsRepository;
import it.pagopa.selfcare.iam.util.DataEncryptionConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class IamServiceImpl implements IamService {

  @Inject
  private final UserPermissionsRepository userPermissionsRepository;

  @Override
  public Uni<String> ping() {
    return Uni.createFrom().item("OK");
  }

  /**
   * Saves or updates a user with their product-specific roles.
   * If the user doesn't exist, creates a new user with a generated UID.
   * If the user exists, updates their information and merges product roles.
   * 
   * @param saveUserRequest the request containing user details and product roles
   * @param productId optional product ID to filter roles for a specific product
   * @return a Uni containing the saved or updated UserClaims
   * @throws InvalidRequestException if the request or email is null/blank
   */
  @Override
  public Uni<UserClaims> saveUser(SaveUserRequest saveUserRequest, final String productId) {
    return Uni.createFrom().item(saveUserRequest)
      .onItem().ifNull().failWith(() -> new InvalidRequestException("User cannot be null"))
      .onItem().transformToUni(req ->
      Uni.createFrom().optional(
        Optional.ofNullable(req.getEmail())
          .filter(email -> !email.isBlank())
          .filter(email -> email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
          .map(email -> UserClaims.builder()
            .email(DataEncryptionConfig.encrypt(req.getEmail()))
            .name(DataEncryptionConfig.encrypt(req.getName()))
            .familyName(DataEncryptionConfig.encrypt(req.getFamilyName()))
            .productRoles(setFilteredProductRoles(req.getProductRoles(), productId))
            .build())
        )
        .onItem().ifNull().failWith(() -> new InvalidRequestException("Invalid email format"))
        .onItem().transformToUni(userClaims ->
        UserClaims.findByEmail(userClaims.getEmail())
          .onItem().ifNull().continueWith(() -> {
          userClaims.setUid(UUID.randomUUID().toString());
          return userClaims;
          })
          .onItem().ifNotNull().transform(existing -> {
          userClaims.setUid(existing.getUid());
          Optional.ofNullable(userClaims.getName()).ifPresent(existing::setName);
          Optional.ofNullable(userClaims.getFamilyName()).ifPresent(existing::setFamilyName);

          Optional.ofNullable(productId).ifPresentOrElse(
            pid -> Optional.ofNullable(userClaims.getProductRoles())
              .flatMap(prs -> prs.stream().filter(pr -> pr.getProductId().equals(pid)).findFirst())
              .ifPresent(newProductRole -> {
              List<ProductRoles> existingRoles = Optional.ofNullable(existing.getProductRoles()).orElse(List.of());
              List<ProductRoles> mutableRoles = new ArrayList<>(existingRoles);
              mutableRoles.stream()
                .filter(epr -> epr.getProductId().equals(pid))
                .findFirst()
                .ifPresentOrElse(
                existingProductRole -> existingProductRole.setRoles(newProductRole.getRoles()),
                () -> mutableRoles.add(newProductRole)
                );
              existing.setProductRoles(mutableRoles);
              }),
            () -> Optional.ofNullable(userClaims.getProductRoles()).ifPresent(existing::setProductRoles)
            );
          return existing;
          })
          .chain(user -> user.persistOrUpdate()
            .map(v -> decryptUser(userClaims)))
        )
        .onItem().ifNull().failWith(() -> new InvalidRequestException("Email cannot be null"))
      );
  }

  private UserClaims decryptUser(UserClaims userClaims) {
    return UserClaims.builder()
      .uid(Optional.ofNullable(userClaims.getUid()).orElse(""))
      .email(DataEncryptionConfig.decrypt(userClaims.getEmail()))
      .name(DataEncryptionConfig.decrypt(userClaims.getName()))
      .familyName(DataEncryptionConfig.decrypt(userClaims.getFamilyName()))
      .productRoles(userClaims.getProductRoles())
      .test(userClaims.isTest())
      .build();
  }

  /**
   * Retrieves a user by their ID and product ID.
   *
   * @param userId the ID of the user
   * @param productId the ID of the product
   * @return a Uni containing the UserClaims if found
   * @throws ResourceNotFoundException if the user is not found
   */
  @Override
  public Uni<UserClaims> getUser(String userId, String productId) {
    return UserClaims.findByUidAndProductId(userId, productId)
      .onItem().ifNotNull().transform(userClaims -> {
        userClaims.setProductRoles(setFilteredProductRoles(userClaims.getProductRoles(), productId));
        return decryptUser(userClaims);
      })
      .onItem().ifNull().failWith(() -> new ResourceNotFoundException("User not found"));
  }

  /**
   * Retrieves a list of product, role and permissions by user ID and product ID.
   *
   * @param userId the ID of the user
   * @param productId the ID of the product
   * @return a Uni containing a ProductRolePermissionsList if found
   */
  @Override
  public Uni<ProductRolePermissionsList> getProductRolePermissionsList(String userId, String productId) {
    return userPermissionsRepository.getUserProductRolePermissionsList(userId, productId)
            .map(ProductRolePermissionsList::new);
  }

  /**
   * Filters the product roles for a specific product ID.
   * @param productRoles the list of product roles
   * @param productId the ID of the product
   * @return a list of filtered ProductRoles
   */
  public List<ProductRoles> setFilteredProductRoles(List<ProductRoles> productRoles, String productId) {
    return Optional.ofNullable(productRoles)
            .map(roles -> Optional.ofNullable(productId)
                    .map(pid -> {
                      List<ProductRoles> exact = roles.stream()
                              .filter(pr -> pr.getProductId().equals(pid))
                              .toList();
                      return exact.isEmpty()
                              ? roles.stream().filter(pr -> "ALL".equals(pr.getProductId())).toList()
                              : exact;
                    })
                    .orElse(roles)
            )
            .orElse(List.of());
  }

  /**
   * Checks if a user has a specific permission for a product.
   *
   * @param userId the ID of the user
   * @param permission the permission to check
   * @param productId the ID of the product
   * @param institutionId the ID of the institution
   * @return a Uni containing true if the user has the permission, false otherwise
   */
  @Override
  public Uni<Boolean> hasPermission(String userId, String permission, String productId, String institutionId) {
    return userPermissionsRepository.getUserPermissions(userId, permission, productId)
      .onItem().transform(userPermissions -> userPermissions.getPermissions().contains(permission))
      .onFailure(ResourceNotFoundException.class)
      .recoverWithItem(ex -> false);
  }
}
