package it.pagopa.selfcare.iam.controller;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.exception.handler.ExceptionHandler;
import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.iam.service.IamServiceImpl;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(IamController.class)
public class IamControllerTest {

  @InjectMock
  IamServiceImpl iamService;

  @Inject
  IamController controller;

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

   @ParameterizedTest
   @ValueSource(strings = {"", "   "}) // Test with both an empty string and spaces
   @DisplayName("An empty user should return 400 Bad Request")
   void handleUSersResponse_withEmptyUSer_shouldReturnBadRequest(String emptySamlResponse) throws Exception {
     SaveUserRequest request = new SaveUserRequest();

     // ARRANGE
     Mockito.when(iamService.saveUser(Mockito.any(SaveUserRequest.class), Mockito.anyString()))
       .thenReturn(Uni.createFrom().failure(new InvalidRequestException("User cannot be null")));

     Uni<jakarta.ws.rs.core.Response> responseUni = controller.users(null, null);
     jakarta.ws.rs.core.Response response = responseUni.await().indefinitely();

     // ASSERT
     assertNotNull(response);
     assertEquals(jakarta.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
     Mockito.verifyNoInteractions(iamService);
   }

  @Test
  void shouldReturn400WhenServiceFails() {
    SaveUserRequest request = new SaveUserRequest();
    request.setEmail(null);

    Mockito.when(iamService.saveUser(Mockito.any(SaveUserRequest.class), Mockito.anyString()))
      .thenReturn(Uni.createFrom().failure(new InvalidRequestException("User cannot be null")));

    given()
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .patch("/users")
      .then()
      .log().all()  // Aggiungi log per vedere cosa viene restituito
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
      .get("/users/{uid}", userId)
      .then()
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
      .get("/users/{uid}", userId)
      .then()
      .statusCode(200)
      .body("id", is(userId))
      .body("productId", is(productId));
  }
}
