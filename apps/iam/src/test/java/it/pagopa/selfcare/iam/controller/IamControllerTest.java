package it.pagopa.selfcare.iam.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.entity.UserClaims;
import it.pagopa.selfcare.iam.service.IamServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class IamControllerTest {

  @InjectMock
  IamServiceImpl iamService;

  @Test
  void shouldReturn200WhenUserIsCreated() {
    UserClaims response = new UserClaims();
    response.setName("john");
    response.setEmail("john@example.com");

    Mockito.when(iamService.saveUser(Mockito.any(SaveUserRequest.class), Mockito.anyString()))
      .thenReturn(Uni.createFrom().item(response));

    given()
      .contentType(ContentType.JSON)
      .body(response)
      .when()
      .patch("/iam/users")
      .then()
      .statusCode(200)
      .body("name", equalTo("john"))
      .body("email", equalTo("john@example.com"));
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
