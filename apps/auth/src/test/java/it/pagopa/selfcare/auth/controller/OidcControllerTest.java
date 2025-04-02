package it.pagopa.selfcare.auth.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeResponse;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.service.OidcService;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(OidcController.class)
class OidcControllerTest {
  @InjectMock private OidcService oidcService;

  @Test
  void exchangeWithAuthCode() {
    JsonObject request =
        Json.createObjectBuilder().add("code", "code").add("redirectUri", "redirect").build();
    when(oidcService.exchange(anyString(), anyString()))
        .thenReturn(
            Uni.createFrom().item(OidcExchangeResponse.builder().sessionToken("token").build()));
    given()
        .body(request.toString())
        .when()
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(200)
        .body("sessionToken", equalTo("token"));
  }

  @Test
  void badRequestWithMalformedInput() {
    JsonObject jsonObject = Json.createObjectBuilder().add("code", "code").build();

    when(oidcService.exchange(anyString(), anyString()))
        .thenReturn(
            Uni.createFrom().item(OidcExchangeResponse.builder().sessionToken("token").build()));
    given()
        .body(jsonObject.toString())
        .when()
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(400);
  }

  @Test
  void forbiddenWithForbiddenException() {
    JsonObject request =
        Json.createObjectBuilder()
            .add("code", "invalidCode")
            .add("redirectUri", "redirect")
            .build();

    when(oidcService.exchange(anyString(), anyString()))
        .thenReturn(Uni.createFrom().failure(new ForbiddenException("Forbidden")));
    given()
        .body(request.toString())
        .when()
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(403);
  }

  @Test
  void notFoundWithNotFoundException() {
    JsonObject request =
        Json.createObjectBuilder()
            .add("code", "invalidCode")
            .add("redirectUri", "redirect")
            .build();

    when(oidcService.exchange(anyString(), anyString()))
        .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Not Found")));
    given()
        .body(request.toString())
        .when()
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(404);
  }

  @Test
  void internalServerErrorWithInternalException() {
    JsonObject request =
        Json.createObjectBuilder()
            .add("code", "invalidCode")
            .add("redirectUri", "redirect")
            .build();

    when(oidcService.exchange(anyString(), anyString()))
        .thenReturn(Uni.createFrom().failure(new InternalException("Internal error")));
    given()
        .body(request.toString())
        .when()
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(500);
  }
}
