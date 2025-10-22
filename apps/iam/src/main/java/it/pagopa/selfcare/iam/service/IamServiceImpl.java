package it.pagopa.selfcare.iam.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.iam.repository.UserPermissionsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
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
          .email(req.getEmail())
          .name(req.getName())
          .familyName(req.getFamilyName())
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
          .chain(user -> user.persistOrUpdate().map(v -> userClaims))
        )
        .onItem().ifNull().failWith(() -> new InvalidRequestException("Email cannot be null"))
      );
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
        return userClaims;
      })
      .onItem().ifNull().failWith(() -> new ResourceNotFoundException("User not found"));
  }

  /**
   * Filters the product roles for a specific product ID.
   * @param productRoles the list of product roles
   * @param productId the ID of the product
   * @return a list of filtered ProductRoles
   */
  public List<ProductRoles> setFilteredProductRoles(List<ProductRoles> productRoles, String productId) {
    return Optional.ofNullable(productId).map(pid ->
      Optional.ofNullable(productRoles)
        .map(prs -> prs.stream()
          .filter(pr -> pr.getProductId().equals(pid))
          .toList())
        .orElse(List.of())
    ).orElse(productRoles);
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
