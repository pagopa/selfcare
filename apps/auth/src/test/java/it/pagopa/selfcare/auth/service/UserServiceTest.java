package it.pagopa.selfcare.auth.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.client.ExternalInternalUserApi;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.model.UserClaims;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.internal_json.model.UserInfoResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.NameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
public class UserServiceTest {

  @Inject UserService userService;

  @RestClient @InjectMock UserApi userRegistryApi;

  @RestClient @InjectMock ExternalInternalUserApi internalUserApi;

  @Test
  void patchUserWithValidInputs() {
    UUID userId = UUID.randomUUID();
    UserClaims expected =
        UserClaims.builder()
            .uid(userId.toString())
            .fiscalCode("fiscalCode")
            .name("name")
            .familyName("family")
            .sameIdp(true)
            .build();
    when(userRegistryApi.saveUsingPATCH(any()))
        .thenReturn(Uni.createFrom().item(UserId.builder().id(userId).build()));
    userService
        .patchUser("fiscalCode", "name", "family", true)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted()
        .assertItem(expected);
  }

  @Test
  void failure_patchUserWhenPatchingUserOnPdv() {
    String exceptionDesc = "Cannot invoke patch on PDV";
    when(userRegistryApi.saveUsingPATCH(any()))
        .thenReturn(Uni.createFrom().failure(new Exception(exceptionDesc)));
    userService
        .patchUser("fiscalCode", "name", "family", true)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(Exception.class, exceptionDesc);
  }

  @Test
  void getUserInfoEmailWithValidInputs() {
    UUID userId = UUID.randomUUID();
    org.openapi.quarkus.internal_json.model.UserResource userResource =
        org.openapi.quarkus.internal_json.model.UserResource.builder()
            .lastActiveOnboardingUserEmail("test@test.email")
            .build();
    UserInfoResource userInfoResource = UserInfoResource.builder().user(userResource).build();
    UserClaims claims =
        UserClaims.builder()
            .uid(userId.toString())
            .fiscalCode("fiscalCode")
            .name("name")
            .familyName("family")
            .sameIdp(true)
            .build();
    when(internalUserApi.v2getUserInfoUsingGET(any()))
        .thenReturn(Uni.createFrom().item(userInfoResource));
    userService
        .getUserInfoEmail(claims)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted()
        .assertItem("test@test.email");
  }

  @Test
  void failure_getUserInfoEmailWhenUserNotFound() {
    UUID userId = UUID.randomUUID();
    UserClaims claims =
        UserClaims.builder()
            .uid(userId.toString())
            .fiscalCode("fiscalCode")
            .name("name")
            .familyName("family")
            .sameIdp(true)
            .build();
    when(internalUserApi.v2getUserInfoUsingGET(any()))
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
    userService
        .getUserInfoEmail(claims)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(ResourceNotFoundException.class);
  }

  @Test
  void failure_getUserInfoEmailWhenInternalApisError() {
    UUID userId = UUID.randomUUID();
    UserClaims claims =
        UserClaims.builder()
            .uid(userId.toString())
            .fiscalCode("fiscalCode")
            .name("name")
            .familyName("family")
            .sameIdp(true)
            .build();
    when(internalUserApi.v2getUserInfoUsingGET(any()))
        .thenReturn(Uni.createFrom().failure(new WebApplicationException(500)));
    userService
        .getUserInfoEmail(claims)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(InternalException.class);
  }

  @Test
  void failure_getUserClaimsFromPdvWhenInternalError() {
    String exceptionDesc = "Cannot invoke getUser on PDV";
    when(userRegistryApi.findByIdUsingGET(anyString(), anyString()))
        .thenReturn(Uni.createFrom().failure(new Exception(exceptionDesc)));
    userService
        .getUserClaimsFromPdv("userId")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertFailed()
        .assertFailedWith(Exception.class, exceptionDesc);
  }

  @Test
  void getUserClaimsFromPdvWithValidInputs() {
    UserClaims expectedUserClaims =
        UserClaims.builder()
            .familyName("familyName")
            .name("name")
            .uid("userId")
            .fiscalCode("fiscalCode")
            .build();
    UserResource pdvUserResource =
        UserResource.builder()
            .fiscalCode("fiscalCode")
            .name(NameCertifiableSchema.builder().value("name").build())
            .familyName(FamilyNameCertifiableSchema.builder().value("familyName").build())
            .build();
    when(userRegistryApi.findByIdUsingGET(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(pdvUserResource));
    userService
        .getUserClaimsFromPdv("userId")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted()
        .assertItem(expectedUserClaims);
  }
}
