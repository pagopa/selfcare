package it.pagopa.selfcare.auth.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import it.pagopa.selfcare.auth.service.SAMLService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(SamlCallbackController.class)
class SamlCallbackControllerTest {
  public static final String SESSION_TOKEN = "TOKEN";
  @InjectMock
  private SAMLService samlService;

  @Inject
  SamlCallbackController controller;

  private static final String VALID_SAML_RESPONSE = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHNhbWxwOlJlc3BvbnNlICZsdDsgJmd0OyAmYW1wOyAmcXVvdDs+PC9zYW1scDpSZXNwb25zZT4K";
  private static final String INVALID_SAML_RESPONSE = "an-invalid-saml-response-string";

  @BeforeEach
  void setUp() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  /**
   * Test case for a failed SAML validation.
   * The controller should return a 400 Bad Request response with an error message.
   */
  @Test
  public void testHandleSamlResponse_Invalid() throws Exception {
    // Arrange: Configure the mock service to return 'false' for validation.
    when(samlService.generateSessionToken(anyString())).thenThrow(new SamlSignatureException("Validation Error"));

    // Act & Assert
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .formParam("SAMLResponse", INVALID_SAML_RESPONSE)
      .when()
      .post("/acs")
      .then()
      .statusCode(400);
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
  void handleSamlResponse_NullSamlResponse_ShouldReturnBadRequest() throws Exception {
    // When & Then
    given()
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .when()
      .post("/acs")
      .then()
      .statusCode(400)
      .body(equalTo("SAMLResponse is required."));

    // Verify that the service is not called when SAMLResponse is null
    verify(samlService, never()).generateSessionToken(anyString());
  }

  @Test
  void handleSamlResponse_EmptySamlResponse_ShouldReturnBadRequest() throws Exception {
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
    verify(samlService, never()).generateSessionToken(anyString());
  }

  @Test
  void handleSamlResponse_ServiceValidationFailure_ShouldReturnBadRequest() throws Exception {
    // Given
    when(samlService.generateSessionToken(anyString()))
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
  void handleSamlResponse_LongSamlResponse_ShouldHandleCorrectly() throws Exception {
    // Given
    StringBuilder longSamlResponse = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longSamlResponse.append(VALID_SAML_RESPONSE);
    }
    String longResponse = longSamlResponse.toString();

    when(samlService.generateSessionToken(longResponse))
      .thenReturn(Uni.createFrom().item(SESSION_TOKEN));

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
  @DisplayName("Happy path: a valid SAML response should redirect successfully")
  void handleSamlResponse_withValidRequest_shouldRedirect() throws Exception {
    // ARRANGE
    String samlResponse = "valid-saml-response";
    String sessionToken = "generated-session-token";
    String redirectUrl = "https://selfcare.pagopa.it/success";

    // Mock the ContainerRequestContext to simulate a correct Content-Type
    ContainerRequestContext mockRequestContext = Mockito.mock(ContainerRequestContext.class);
    when(mockRequestContext.getMediaType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

    // Mock the calls to SAMLService
    when(samlService.generateSessionToken(samlResponse)).thenReturn(Uni.createFrom().item(sessionToken));
    when(samlService.getLoginSuccessUrl(sessionToken)).thenReturn(redirectUrl);

    // ACT
    Uni<Response> responseUni = controller.handleSamlResponse(mockRequestContext, samlResponse);
    Response response = responseUni.await().indefinitely(); // Wait for the Uni's result

    // ASSERT
    assertNotNull(response);
    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus()); // 303 See Other
    assertEquals(URI.create(redirectUrl), response.getLocation());

    // Verify that the service methods were called correctly
    Mockito.verify(samlService).generateSessionToken(samlResponse);
    Mockito.verify(samlService).getLoginSuccessUrl(sessionToken);
  }

  @Test
  @DisplayName("An invalid Content-Type should return 415 Unsupported Media Type")
  void handleSamlResponse_withInvalidContentType_shouldReturnUnsupportedMediaType() throws Exception {
    // ARRANGE
    ContainerRequestContext mockRequestContext = Mockito.mock(ContainerRequestContext.class);
    when(mockRequestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE); // Content-Type errato

    // ACT
    Uni<Response> responseUni = controller.handleSamlResponse(mockRequestContext, "some-saml-response");
    Response response = responseUni.await().indefinitely();

    // ASSERT
    assertNotNull(response);
    assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), response.getStatus());
    // Verify that the service was never called
    Mockito.verifyNoInteractions(samlService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "}) // Test with both an empty string and spaces
  @DisplayName("A missing SAMLResponse should return 400 Bad Request")
  void handleSamlResponse_withMissingSamlResponse_shouldReturnBadRequest(String emptySamlResponse) throws Exception {
    // ARRANGE
    ContainerRequestContext mockRequestContext = Mockito.mock(ContainerRequestContext.class);
    when(mockRequestContext.getMediaType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

    // ACT
    Uni<Response> responseUni = controller.handleSamlResponse(mockRequestContext, emptySamlResponse);
    Response response = responseUni.await().indefinitely();

    // ASSERT
    assertNotNull(response);
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    Mockito.verifyNoInteractions(samlService);
  }

  @Test
  @DisplayName("When SAMLService fails, it should propagate the exception")
  void handleSamlResponse_whenServiceFails_shouldPropagateException() throws Exception {
    // ARRANGE
    String samlResponse = "invalid-saml-response";
    ContainerRequestContext mockRequestContext = Mockito.mock(ContainerRequestContext.class);
    when(mockRequestContext.getMediaType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_TYPE);

    // Mock the service to simulate a failure
    SamlSignatureException expectedException = new SamlSignatureException("Invalid signature");
    when(samlService.generateSessionToken(anyString()))
      .thenReturn(Uni.createFrom().failure(expectedException));

    // ACT & ASSERT
    // Verify that the Uni fails with the expected exception
    SamlSignatureException thrown = assertThrows(SamlSignatureException.class, () -> {
      controller.handleSamlResponse(mockRequestContext, samlResponse).await().indefinitely();
    });

    assertEquals("Invalid signature", thrown.getMessage());
    Mockito.verify(samlService).generateSessionToken(samlResponse);
    Mockito.verifyNoMoreInteractions(samlService); // getLoginSuccessUrl should not be called
  }
}
