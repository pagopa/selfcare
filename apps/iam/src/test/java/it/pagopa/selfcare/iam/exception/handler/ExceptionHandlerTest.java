package it.pagopa.selfcare.iam.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.iam.controller.response.Problem;
import it.pagopa.selfcare.iam.exception.ConflictException;
import it.pagopa.selfcare.iam.exception.ForbiddenException;
import it.pagopa.selfcare.iam.exception.InternalException;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.exception.UnimplementedException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ExceptionHandlerTest {

  private final ExceptionHandler handler = new ExceptionHandler();

  @Test
  void mapsBadRequestToProblemResponse() {
    Response response = handler.toResponse(new BadRequestException("Conflicting tenant context"));

    assertProblem(response, 400, "Conflicting tenant context");
  }

  @Test
  void mapsDomainExceptionsToExpectedStatuses() {
    assertProblem(handler.toResponse(new InvalidRequestException("invalid")), 400, "invalid");
    assertProblem(handler.toResponse(new ResourceNotFoundException("missing")), 404, "missing");
    assertProblem(handler.toResponse(new ForbiddenException("denied")), 403, "denied");
    assertProblem(handler.toResponse(new ConflictException("conflict")), 409, "conflict");
    assertProblem(handler.toResponse(new InternalException("internal")), 500, "internal");
    assertProblem(handler.toResponse(new UnimplementedException("todo")), 501, "Not implemented");
    assertProblem(handler.toResponse(new NotAllowedException("GET")), 405, "Method not allowed");
    assertProblem(
        handler.toResponse(new RuntimeException("boom")),
        500,
        ExceptionHandler.SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER);
  }

  private static void assertProblem(Response response, int status, String detail) {
    Problem problem = (Problem) response.getEntity();
    assertEquals(status, response.getStatus());
    assertEquals("application/problem+json", response.getMediaType().toString());
    assertEquals(status, problem.getStatus());
    assertEquals(detail, problem.getDetail());
  }
}
