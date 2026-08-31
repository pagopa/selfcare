package it.pagopa.selfcare.iam.exception.handler;

import it.pagopa.selfcare.iam.controller.response.Problem;
import it.pagopa.selfcare.iam.exception.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExceptionHandler {

  public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER =
      "Something has gone wrong in the server";
  public static final String FORBIDDEN = "Forbidden";
  public static final String CONFLICT = "Conflict";
  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

  @ServerExceptionMapper
  public Response toResponse(InvalidRequestException exception) {
    logHandledException(Response.Status.BAD_REQUEST, exception);
    return problem(Response.Status.BAD_REQUEST, exception.getMessage());
  }

  @ServerExceptionMapper
  public Response toResponse(Exception exception) {
    LOGGER.error(
        "event=request_failed status=500 exception={}",
        exception.getClass().getSimpleName(),
        exception);
    return problem(Response.Status.INTERNAL_SERVER_ERROR, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER);
  }

  @ServerExceptionMapper
  public Response toResponse(ResourceNotFoundException exception) {
    logHandledException(Response.Status.NOT_FOUND, exception);
    return problem(Response.Status.NOT_FOUND, exception.getMessage());
  }

  @ServerExceptionMapper
  public Response toResponse(ForbiddenException exception) {
    logHandledException(Response.Status.FORBIDDEN, exception);
    return problem(Response.Status.FORBIDDEN, exception.getMessage());
  }

  @ServerExceptionMapper
  public Response toResponse(InternalException exception) {
    logHandledException(Response.Status.INTERNAL_SERVER_ERROR, exception);
    return problem(Response.Status.INTERNAL_SERVER_ERROR, exception.getMessage());
  }

  @ServerExceptionMapper
  public Response toResponse(ConflictException exception) {
    logHandledException(Response.Status.CONFLICT, exception);
    return problem(Response.Status.CONFLICT, exception.getMessage());
  }

  @ServerExceptionMapper
  public Response toResponse(UnimplementedException exception) {
    logHandledException(Response.Status.NOT_IMPLEMENTED, exception);
    return problem(Response.Status.NOT_IMPLEMENTED, "Not implemented");
  }

  @ServerExceptionMapper
  public Response toResponse(NotAllowedException exception) {
    logHandledException(Response.Status.METHOD_NOT_ALLOWED, exception);
    return problem(Response.Status.METHOD_NOT_ALLOWED, "Method not allowed");
  }

  private void logHandledException(Response.Status status, Exception exception) {
    LOGGER.warn(
        "event=request_rejected status={} exception={}",
        status.getStatusCode(),
        exception.getClass().getSimpleName());
  }

  private Response problem(Response.Status status, String detail) {
    Problem problem =
        new Problem(detail, null, status.getStatusCode(), status.getReasonPhrase(), null);
    return Response.status(status).type("application/problem+json").entity(problem).build();
  }
}
