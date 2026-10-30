package it.pagopa.selfcare.onboarding.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.exception.NonRetryableRestClientException;
import it.pagopa.selfcare.onboarding.exception.RestClientException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class RestClientErrorMapperTest {

  private final RestClientErrorMapper mapper = new RestClientErrorMapper();

  @ParameterizedTest
  @ValueSource(ints = {400, 422})
  void toThrowable_shouldBeNonRetryableOnDefinitiveClientErrors(int status) {
    Response response = response(status, "{\"detail\":\"institution.description is required\"}");

    WebApplicationException ex = mapper.toThrowable(response);

    assertInstanceOf(NonRetryableRestClientException.class, ex);
    assertEquals("HTTP " + status + " - {\"detail\":\"institution.description is required\"}", ex.getMessage());
    assertSame(response, ex.getResponse());
    verify(response).bufferEntity();
  }

  @ParameterizedTest
  @ValueSource(ints = {404, 409, 429, 500, 503})
  void toThrowable_shouldStayRetryableOnOtherErrors(int status) {
    Response response = response(status, "boom");

    WebApplicationException ex = mapper.toThrowable(response);

    assertInstanceOf(RestClientException.class, ex);
    assertEquals(status, ex.getResponse().getStatus());
    assertEquals("HTTP " + status + " - boom", ex.getMessage());
  }

  @Test
  void toThrowable_shouldTruncateLongBodyAndTolerateUnreadableBody() {
    Response longBody = response(500, "x".repeat(RestClientErrorMapper.MAX_BODY_LENGTH * 2));
    assertTrue(mapper.toThrowable(longBody).getMessage().length() < RestClientErrorMapper.MAX_BODY_LENGTH + 20);

    Response unreadable = response(500, null);
    when(unreadable.readEntity(String.class)).thenThrow(new IllegalStateException("closed"));
    assertEquals("HTTP 500 - <body not readable>", mapper.toThrowable(unreadable).getMessage());
  }

  private static Response response(int status, String body) {
    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(status);
    when(response.hasEntity()).thenReturn(true);
    when(response.readEntity(String.class)).thenReturn(body);
    return response;
  }
}
