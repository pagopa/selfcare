package it.pagopa.selfcare.product.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.pagopa.selfcare.product.exception.*;
import it.pagopa.selfcare.product.model.dto.response.Problem;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

class ExceptionHandlerTest {

  private final ExceptionHandler handler = new ExceptionHandler();

  @Test
  void mapsInvalidRequestAndUnexpectedException() {
    RestResponse<String> invalid = handler.toResponse(new InvalidRequestException("invalid"));
    assertEquals(400, invalid.getStatus());
    assertEquals("invalid", invalid.getEntity());

    RestResponse<String> unexpected = handler.toResponse(new RuntimeException("boom"));
    assertEquals(500, unexpected.getStatus());
    assertEquals(ExceptionHandler.SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, unexpected.getEntity());
  }

  @Test
  void mapsNotFoundParameterToProblemResponse() {
    Response response = handler.toResponse(new NotFoundException());
    Problem problem = (Problem) response.getEntity();

    assertEquals(400, response.getStatus());
    assertEquals("application/problem+json", response.getMediaType().toString());
    assertEquals("Bad Request", problem.getTitle());
    assertEquals(400, problem.getStatus());
  }

  @Test
  void mapsDomainExceptionsToExpectedStatuses() {
    assertProblem(handler.toResponse(new ResourceNotFoundException("missing", "404")), 404);
    assertProblem(handler.toResponse(new ForbiddenException("forbidden")), 403);
    assertProblem(handler.toResponse(new InternalException("internal")), 500);
    assertProblem(handler.toResponse(new ConflictException("conflict")), 409);
    assertProblem(handler.toResponse(new UnimplementedException("todo")), 501);
    assertProblem(handler.toResponse(new NotAllowedException("not allowed")), 405);
  }

  private static void assertProblem(Response response, int status) {
    assertEquals(status, response.getStatus());
    assertEquals(Problem.class, response.getEntity().getClass());
    assertEquals(status, ((Problem) response.getEntity()).getStatus());
  }
}
