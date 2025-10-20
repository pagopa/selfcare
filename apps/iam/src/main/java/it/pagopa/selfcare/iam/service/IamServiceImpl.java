package it.pagopa.selfcare.iam.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.model.ProductRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class IamServiceImpl implements IamService {

  @Override
  public Uni<String> ping() {
    return Uni.createFrom().item("OK");
  }

  @Override
  public Uni<UserClaims> saveUser(SaveUserRequest saveUserRequest, final String productId) {
    return Uni.createFrom().item(saveUserRequest)
      .onItem().ifNull().failWith(() -> new InvalidRequestException("User cannot be null"))
      .onItem().transformToUni(req ->
        Uni.createFrom().optional(
            Optional.ofNullable(req.getEmail())
              .filter(email -> !email.isBlank())
              .map(email -> UserClaims.builder()
                .email(req.getEmail())
                .name(req.getName())
                .familyName(req.getFamilyName())
                .productRoles(setFilteredProductRoles(req.getProductRoles(), productId))
                .build())
          )
          .chain(userClaims ->
            UserClaims.findByEmail(userClaims.getEmail())
              .onItem().ifNull().continueWith(() -> {
                userClaims.setUid(UUID.randomUUID().toString());
                return userClaims;
              })
              .onItem().ifNotNull().transform(existing -> {
                Optional.ofNullable(userClaims.getName()).ifPresent(existing::setName);
                Optional.ofNullable(userClaims.getFamilyName()).ifPresent(existing::setFamilyName);
                Optional.ofNullable(userClaims.getProductRoles()).ifPresent(prodRoles ->
                  prodRoles.forEach((key, value) -> existing.getProductRoles().put(key, value)));
                return existing;
              })
              .chain(user -> user.persistOrUpdate().map(v -> userClaims))
          )
          .onItem().ifNull().failWith(() -> new InvalidRequestException("Email cannot be null"))
      );
  }

  @Override
  public Uni<UserClaims> getUser(String userId, String productId) {
    return UserClaims.findByUidAndProductId(userId, productId)
      .onItem().ifNotNull().transform(userClaims -> {
        userClaims.setProductRoles(setFilteredProductRoles(userClaims.getProductRoles(), productId));
        return userClaims;
      })
      .onItem().ifNull().failWith(() -> new ResourceNotFoundException("User not found"));
  }

  public Map<String, ProductRoles> setFilteredProductRoles(Map<String, ProductRoles> productRoles, String productId) {
    return Optional.ofNullable(productId)
      .map(pid -> Optional.ofNullable(productRoles)
        .filter(prs -> prs.containsKey(pid))
        .map(prs -> Map.of(pid, prs.get(pid)))
        .orElse(Map.of()))
      .orElse(productRoles);
  }


}
