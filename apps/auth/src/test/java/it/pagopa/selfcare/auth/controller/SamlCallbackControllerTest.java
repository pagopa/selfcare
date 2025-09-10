package it.pagopa.selfcare.auth.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import io.restassured.RestAssured;
import it.pagopa.selfcare.auth.service.SAMLService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(SamlCallbackController.class)
class SamlCallbackControllerTest {
  @InjectMock
  private SAMLService samlService;

  private static final String VALID_SAML_RESPONSE = "a-valid-saml-response-string";
  private static final String INVALID_SAML_RESPONSE = "an-invalid-saml-response-string";

  @BeforeEach
  void setUp() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  /**
   * Test case for a successful SAML validation.
   * The controller should return a 200 OK response with the SAML response in the body.
   */
  @Test
  public void testHandleSamlResponse_Success() {
    // Arrange: Configure the mock service to return 'true' for validation.
    when(samlService.validate(VALID_SAML_RESPONSE))
      .thenReturn(Uni.createFrom().item(true));

    // Act & Assert
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .formParam("SAMLResponse", VALID_SAML_RESPONSE)
      .when()
      .post("/acs")
      .then()
      .statusCode(200)
      .body(is("samlResponse: " + VALID_SAML_RESPONSE));
  }

  /**
   * Test case for a failed SAML validation.
   * The controller should return a 400 Bad Request response with an error message.
   */
  @Test
  public void testHandleSamlResponse_Invalid() {
    // Arrange: Configure the mock service to return 'false' for validation.
    when(samlService.validate(anyString())).thenReturn(Uni.createFrom().item(false));

    // Act & Assert
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .formParam("SAMLResponse", INVALID_SAML_RESPONSE)
      .when()
      .post("/acs")
      .then()
      .statusCode(400)
      .body(is("SAMLResponse not valid"));
  }

  /**
   * Test case for a missing SAMLResponse parameter.
   * The controller should return a 400 Bad Request response with a specific error message.
   */
  @Test
  public void testHandleSamlResponse_Null() {
    // Arrange: No mock setup is needed as the service is not called.

    // Act & Assert
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      // Note: We are not sending the "SAMLResponse" form parameter.
      .when()
      .post("/acs")
      .then()
      .statusCode(400)
      .body(is("SAMLResponse is required."));
  }

  @Test
  void handleSamlResponse_NullSamlResponse_ShouldReturnBadRequest() {
    // When & Then
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .when()
      .post("/acs")
      .then()
      .statusCode(400)
      .body(equalTo("SAMLResponse is required."));

    // Verify that the service is not called when SAMLResponse is null
    verify(samlService, never()).validate(anyString());
  }

  @Test
  void handleSamlResponse_EmptySamlResponse_ShouldReturnBadRequest() {
    // When & Then
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .formParam("SAMLResponse", "")
      .when()
      .post("/acs")
      .then()
      .statusCode(400)
      .body(equalTo("SAMLResponse is required."));

    // Verify that the service is not called when SAMLResponse is empty
    verify(samlService, never()).validate(anyString());
  }

  @Test
  void handleSamlResponse_ServiceValidationFailure_ShouldReturnBadRequest() {
    // Given
    when(samlService.validate(anyString()))
      .thenReturn(Uni.createFrom().failure(new RuntimeException("Validation failed")));

    // When & Then
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .formParam("SAMLResponse", VALID_SAML_RESPONSE)
      .when()
      .post("/acs")
      .then()
      .statusCode(500); // Quarkus restituisce 500 per eccezioni non gestite
  }

  @Test
  void handleSamlResponse_PostWithoutBody_ShouldReturnBadRequest() {
    // When & Then
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .when()
      .post("/acs")
      .then()
      .statusCode(400)
      .body(equalTo("SAMLResponse is required."));
  }

  @Test
  void handleSamlResponse_LongSamlResponse_ShouldHandleCorrectly() {
    // Given
    StringBuilder longSamlResponse = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longSamlResponse.append(VALID_SAML_RESPONSE);
    }
    String longResponse = longSamlResponse.toString();

    when(samlService.validate(longResponse))
      .thenReturn(Uni.createFrom().item(true));

    // When & Then
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .formParam("SAMLResponse", longResponse)
      .when()
      .post("/acs")
      .then()
      .statusCode(413);
  }

  @Test
  void handleSamlResponse_SpecialCharactersInSamlResponse_ShouldHandleCorrectly() {
    // Given
    String specialCharsSamlResponse = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHNhbWxwOlJlc3BvbnNlICZsdDsgJmd0OyAmYW1wOyAmcXVvdDs+PC9zYW1scDpSZXNwb25zZT4K";

    when(samlService.validate(specialCharsSamlResponse))
      .thenReturn(Uni.createFrom().item(true));

    // When & Then
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .formParam("SAMLResponse", specialCharsSamlResponse)
      .when()
      .post("/acs")
      .then()
      .statusCode(200)
      .contentType(MediaType.TEXT_PLAIN)
      .body(containsString("samlResponse: " + specialCharsSamlResponse));
  }
}
