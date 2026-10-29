package it.pagopa.selfcare.contractscrm.exception.handler;

import it.pagopa.selfcare.contractscrm.exception.InvalidRequestException;
import it.pagopa.selfcare.contractscrm.exception.ResourceNotFoundException;
import it.pagopa.selfcare.contractscrm.model.dto.response.Problem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExceptionHandler {

  public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER =
      "Something has gone wrong in the server";
  private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";
  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

  @ServerExceptionMapper
  public Response toResponse(InvalidRequestException exception) {
    LOGGER.warn("Invalid request: {}", exception.getMessage());
    return problem(Response.Status.BAD_REQUEST, exception.getMessage());
  }

  @ServerExceptionMapper
  public Response toResponse(ResourceNotFoundException exception) {
    LOGGER.warn("Resource not found: {}", exception.getMessage());
    return problem(Response.Status.NOT_FOUND, exception.getMessage());
  }

  @ServerExceptionMapper
  public Response toResponse(Exception exception) {
    LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage(), exception);
    return problem(Response.Status.INTERNAL_SERVER_ERROR, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER);
  }

  private static Response problem(Response.Status status, String detail) {
    Problem problem =
        Problem.builder()
            .title(status.getReasonPhrase())
            .status(status.getStatusCode())
            .detail(detail)
            .build();
    return Response.status(status).type(PROBLEM_MEDIA_TYPE).entity(problem).build();
  }
}
