package it.pagopa.selfcare.iam.controller;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.service.IamServiceImpl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestHTTPEndpoint(IamController.class)
public class IamControllerTest {

  @InjectMock
  IamServiceImpl iamService;

  @BeforeAll
  static void setup() {
    RestAssured.defaultParser = Parser.JSON;  // Fallback parser
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
    request.setName(null);

    Mockito.when(iamService.saveUser(Mockito.any(SaveUserRequest.class), Mockito.anyString()))
      .thenReturn(Uni.createFrom().failure(new IllegalArgumentException("Invalid user")));

    given()
      .contentType(ContentType.JSON)
      .body(request)
      .when()
      .patch("/iam/users")
      .then()
      .statusCode(400);
  }
}
