package it.pagopa.selfcare.onboarding.exception.handler;

import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.model.error.Problem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TokenExceptionHandlerTest {

  private static OnboardingExceptionMapper tokenExceptionHandler;

  @BeforeAll
  static void setup() {
    tokenExceptionHandler = new OnboardingExceptionMapper();
  }

  @Test
  void handleUserNotAllowedExceptionTest() {
    // given
    UnauthorizedUserException e = mock(UnauthorizedUserException.class);

    // when
    Response response = tokenExceptionHandler.handleUnauthorizedUserException(e);

    // then
    assertNotNull(response);
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    assertNotNull(response.getEntity());
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ((Problem) response.getEntity()).getStatus());
  }

  @Test
  void UnauthorizedUserExceptionTest() {
    UnauthorizedUserException exception =
        assertThrows(
            UnauthorizedUserException.class,
            () -> {
              throw new UnauthorizedUserException("Test Message");
            });

    assertEquals("Test Message", exception.getMessage());
  }
}
