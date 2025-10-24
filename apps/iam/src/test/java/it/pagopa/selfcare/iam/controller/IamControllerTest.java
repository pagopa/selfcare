package it.pagopa.selfcare.iam.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.iam.service.IamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestHTTPEndpoint(IamController.class)
@TestSecurity(user = "userJwt")
public class IamControllerTest {

  @InjectMock
  IamServiceImpl iamService;

  @BeforeEach
  void setup() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @Test
  void shouldReturn200WhenUserIsCreated() {
    SaveUserRequest request = new SaveUserRequest();
    request.setName("john");
    request.setEmail("john@example.com");

    UserClaims response = new UserClaims();
    response.setName("john");
    response.setEmail("john@example.com");

    Mockito.when(iamService.saveUser(Mockito.any(SaveUserRequest.class), Mockito.anyString()))
      .thenReturn(Uni.createFrom().item(response));
//
    given()
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .patch("/users")
      .then()
      .statusCode(200);
  }

  @Test
  void shouldReturn400WhenServiceFails() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail(null);

    Mockito.when(iamService.saveUser(Mockito.any(SaveUserRequest.class), Mockito.nullable(String.class)))
      .thenReturn(Uni.createFrom().failure(new InvalidRequestException("Email cannot be null")));

    given()
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .patch("/users")
      .then()
      .log().all()
      .statusCode(400)
      .body(containsString("Email cannot be null"));
  }

  @Test
  void shouldReturn404WhenUserIsNotFound() {
    String userId = "non-existing-user";
    String productId = "product-1";

    OngoingStubbing<Uni<UserClaims>> userNotFound = Mockito.when(iamService.getUser(userId, productId))
      .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("User not found")));

    given()
      .when()
      .get("/users/{uid}?productId={productid}", userId, productId)
      .then()
      .log().all()
      .statusCode(404);
  }

  @Test
  void shouldReturn200WhenUserIsFound() {
    String userId = "existing-user";
    String productId = "product-1";

    UserClaims userClaims = new UserClaims();
    userClaims.setUid(userId);
    ProductRoles productRoles = new ProductRoles();
    productRoles.setProductId(productId);
    productRoles.setRoles(List.of("role1"));
    userClaims.setProductRoles(List.of(productRoles));

    Mockito.when(iamService.getUser(userId, productId))
      .thenReturn(Uni.createFrom().item(userClaims));

    given()
      .when()
      .get("/users/{uid}?productId={productid}", userId, productId)
      .then()
      .log().all()
      .statusCode(200);
  }

  @Test
  void hasPermission_shouldReturn200_true() {
    String uid = "user-1";
    String permission = "read:users";
    String productId = "productA";
    String institutionId = "inst-1";

    Mockito.when(iamService.hasPermission(uid, permission, productId, institutionId))
      .thenReturn(Uni.createFrom().item(true));

    given()
      .accept(ContentType.JSON)
      .when()
      .get("/users/{uid}/permissions/{permission}?productId={productId}&institutionId={institutionId}",
        uid, permission, productId, institutionId)
      .then()
      .statusCode(200)
      .body(equalTo("true"));
  }

  @Test
  void hasPermission_shouldReturn200_false() {
    Mockito.when(iamService.hasPermission("user-2", "write:users", "productB", "inst-2"))
      .thenReturn(Uni.createFrom().item(false));

    given()
      .accept(ContentType.JSON)
      .when()
      .get("/users/{uid}/permissions/{permission}?productId={productId}&institutionId={institutionId}",
        "user-2", "write:users", "productB", "inst-2")
      .then()
      .statusCode(200)
      .body(equalTo("false"));
  }

  @Test
  void hasPermission_shouldReturn400_invalidRequest() {
    Mockito.when(iamService.hasPermission(Mockito.eq("user-3"), Mockito.eq("bad:perm"),
        Mockito.eq("productC"), Mockito.eq("inst-3")))
      .thenReturn(Uni.createFrom().failure(new InvalidRequestException("Invalid permission")));

    given()
      .accept(ContentType.JSON)
      .when()
      .get("/users/{uid}/permissions/{permission}?productId={productId}&institutionId={institutionId}",
        "user-3", "bad:perm", "productC", "inst-3")
      .then()
      .statusCode(400)
      .body(containsString("Invalid permission"));
  }

  @Test
  void hasPermission_shouldReturn404_userNotFound() {
    Mockito.when(iamService.hasPermission("missing-user", "read:users", "productD", "inst-4"))
      .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("User not found")));

    given()
      .accept(ContentType.JSON)
      .when()
      .get("/users/{uid}/permissions/{permission}?productId={productId}&institutionId={institutionId}",
        "missing-user", "read:users", "productD", "inst-4")
      .then()
      .statusCode(404)
      .body(containsString("User not found"));
  }

  @Test
  void hasPermission_shouldHandleNullOptionalQueryParams() {
    Mockito.when(iamService.hasPermission("user-null", "read:users", null, null))
      .thenReturn(Uni.createFrom().item(true));

    given()
      .accept(ContentType.JSON)
      .when()
      .get("/users/{uid}/permissions/{permission}", "user-null", "read:users")
      .then()
      .statusCode(200)
      .body(equalTo("true"));
  }
}
