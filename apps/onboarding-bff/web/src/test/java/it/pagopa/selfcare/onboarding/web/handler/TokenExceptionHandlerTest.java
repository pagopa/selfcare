package it.pagopa.selfcare.onboarding.web.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.onboarding.connector.exceptions.UnauthorizedUserException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TokenExceptionHandlerTest {

  private static TokenExceptionHandler tokenExceptionHandler;

  @BeforeAll
  static void setup() {
    tokenExceptionHandler = new TokenExceptionHandler();
  }

  @Test
  void handleUserNotAllowedExceptionTest() {
    // given
    UnauthorizedUserException e = mock(UnauthorizedUserException.class);

    // when
    ResponseEntity<Problem> response = tokenExceptionHandler.handleUserNotAllowedException(e);

    // then
    assertNotNull(response);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(HttpStatus.FORBIDDEN.value(), response.getBody().getStatus());
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
