package it.pagopa.selfcare.auth.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.controller.response.MachineTokenResponse;
import it.pagopa.selfcare.auth.exception.MachineAuthenticationException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.service.MachineTokenService;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MachineTokenControllerTest {
  @InjectMock MachineTokenService service;
  private static final String BODY =
      "{\"tenantId\":\"AR\",\"audience\":\"onboarding-functions\",\"scope\":\"onboarding-functions.invoke\"}";

  @Test
  void exchangesWithoutUserSessionAndDisablesResponseCaching() {
    when(service.issue(anyString(), any()))
        .thenReturn(
            new MachineTokenResponse("signed-token", "Bearer", 300, "onboarding-functions.invoke"));
    given()
        .header("Authorization", "Bearer entra-credential")
        .contentType("application/json")
        .body(BODY)
        .post("/token/service")
        .then()
        .statusCode(200)
        .header("Cache-Control", equalTo("no-store"))
        .body("expires_in", equalTo(300))
        .body("access_token", equalTo("signed-token"));
  }

  @Test
  void invalidMachineCredentialReturnsUnauthorizedNotInternalServerError() {
    when(service.issue(any(), any())).thenThrow(new MachineAuthenticationException());
    given()
        .contentType("application/json")
        .body(BODY)
        .post("/token/service")
        .then()
        .statusCode(401)
        .header("WWW-Authenticate", equalTo("Bearer"));
  }

  @Test
  void disabledIssuerReturnsNotFound() {
    when(service.issue(any(), any()))
        .thenThrow(new ResourceNotFoundException("Resource not found"));
    given()
        .contentType("application/json")
        .body(BODY)
        .post("/token/service")
        .then()
        .statusCode(404);
  }

  @Test
  void rejectsMissingFields() {
    given()
        .contentType("application/json")
        .body("{}")
        .post("/token/service")
        .then()
        .statusCode(400);
  }
}
