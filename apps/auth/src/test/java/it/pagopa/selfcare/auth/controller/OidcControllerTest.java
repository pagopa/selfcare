package it.pagopa.selfcare.auth.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeTokenResponse;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.service.OidcService;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(OidcController.class)
class OidcControllerTest {
  @InjectMock private OidcService oidcService;

  @BeforeEach
  void setUpTenantHeader() {
    RestAssured.requestSpecification =
        new RequestSpecBuilder().addHeader("X-Tenant-Id", "AR").build();
  }

  @AfterEach
  void resetRequestSpecification() {
    RestAssured.requestSpecification = null;
  }

  @Test
  void exchangeWithAuthCode() {
    JsonObject request =
        Json.createObjectBuilder().add("code", "code").add("redirectUri", "redirect").build();
    when(oidcService.exchange(anyString(), anyString()))
        .thenReturn(
            Uni.createFrom()
                .item(OidcExchangeTokenResponse.builder().sessionToken("token").build()));
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
            Uni.createFrom()
                .item(OidcExchangeTokenResponse.builder().sessionToken("token").build()));
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

  @Test
  void disabledTenantIsForbidden() {
    RestAssured.requestSpecification = null;
    JsonObject request =
        Json.createObjectBuilder().add("code", "code").add("redirectUri", "redirect").build();

    given()
        .header("X-Tenant-Id", "PNPG")
        .body(request.toString())
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(403);

    verifyNoInteractions(oidcService);
  }

  @Test
  void unknownTenantIsBadRequest() {
    RestAssured.requestSpecification = null;
    JsonObject request =
        Json.createObjectBuilder().add("code", "code").add("redirectUri", "redirect").build();

    given()
        .header("X-Tenant-Id", "OTHER")
        .body(request.toString())
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(400);

    verifyNoInteractions(oidcService);
  }

  @Test
  void missingTenantIsBadRequest() {
    RestAssured.requestSpecification = null;
    JsonObject request =
        Json.createObjectBuilder().add("code", "code").add("redirectUri", "redirect").build();

    given()
        .body(request.toString())
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(400);

    verifyNoInteractions(oidcService);
  }

  @Test
  void tenantRejectionResponseDoesNotLeakInternalDetails() {
    RestAssured.requestSpecification = null;
    JsonObject request =
        Json.createObjectBuilder().add("code", "code").add("redirectUri", "redirect").build();

    given()
        .header("X-Tenant-Id", "OTHER")
        .body(request.toString())
        .contentType(ContentType.JSON)
        .post("/exchange")
        .then()
        .statusCode(400)
        .contentType("application/problem+json")
        .body("detail", equalTo("Invalid tenant context"))
        .body("title", equalTo("Bad Request"));
  }
}
