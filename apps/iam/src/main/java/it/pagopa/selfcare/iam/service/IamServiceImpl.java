package it.pagopa.selfcare.iam.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class IamServiceImpl implements IamService {

  @Override
  public Uni<String> ping() {
    return Uni.createFrom().item("OK");
  }

  @Override
  public Uni<UserClaims> saveUser(SaveUserRequest saveUserRequest) {
    return Uni.createFrom().item(saveUserRequest)
      .onItem().ifNull().failWith(() -> new IllegalArgumentException("User cannot be null"))
      .onItem().transformToUni(req ->
        Uni.createFrom().optional(
            Optional.ofNullable(req.getEmail())
              .filter(email -> !email.isBlank())
              .map(email -> UserClaims.builder()
                .email(req.getEmail())
                .name(req.getName())
                .familyName(req.getFamilyName())
                .roles(req.getRoles())
                .build())
          )
          .chain(userClaims -> 
            UserClaims.findByEmail(userClaims.getEmail())
              .onItem().ifNull().continueWith(() -> {
                // Nuovo utente: genera userId
                userClaims.setUid(UUID.randomUUID().toString());
                return userClaims;
              })
              .onItem().ifNotNull().transform(existing -> {
                // Utente esistente: mantieni userId esistente
                userClaims.setUid(existing.getUid());
                return userClaims;
              })
              .chain(user -> user.persistOrUpdate().map(v -> userClaims))
          )
          .onItem().ifNull().failWith(() -> new IllegalArgumentException("Email cannot be null"))
      );
  }
}
