package it.pagopa.selfcare.document.exception.handler;

import it.pagopa.selfcare.document.exception.*;
import it.pagopa.selfcare.document.model.dto.response.Problem;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ExceptionHandler {

  public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER =
      "Something has gone wrong in the server";
  public static final String FORBIDDEN = "Forbidden";
  public static final String CONFLICT = "Conflict";
  public static final String PREFIX_LOGGER = "{}: {}";
  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

  @PostConstruct
  void init() {
    LOGGER.info("ExceptionHandler initialized - Exception mappers are active");
  }

  @ServerExceptionMapper
  public Response toResponse(InvalidRequestException exception) {
    LOGGER.warn(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(), null, HttpStatus.SC_BAD_REQUEST, exception.getCode(), null);
    return Response.status(Response.Status.BAD_REQUEST).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(Exception exception) {
    LOGGER.error(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
    Problem problem =
        new Problem(
            SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER,
            null,
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER,
            null);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(ResourceNotFoundException exception) {
    LOGGER.warn(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(), null, HttpStatus.SC_NOT_FOUND, exception.getMessage(), null);
    return Response.status(Response.Status.NOT_FOUND).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(ForbiddenException exception) {
    LOGGER.warn(PREFIX_LOGGER, FORBIDDEN, exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(), null, HttpStatus.SC_FORBIDDEN, exception.getMessage(), null);
    return Response.status(Response.Status.FORBIDDEN).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(InternalException exception) {
    LOGGER.error(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(),
            null,
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            exception.getMessage(),
            null);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(ConflictException exception) {
    LOGGER.error(PREFIX_LOGGER, CONFLICT, exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(), null, HttpStatus.SC_CONFLICT, exception.getMessage(), null);
    return Response.status(Response.Status.CONFLICT).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(UnimplementedException exception) {
    LOGGER.error(PREFIX_LOGGER, "Unimplemented endpoint", exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(),
            null,
            HttpStatus.SC_NOT_IMPLEMENTED,
            exception.getMessage(),
            null);
    return Response.status(Response.Status.NOT_IMPLEMENTED).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(NotAllowedException exception) {
    LOGGER.error(PREFIX_LOGGER, "Unimplemented endpoint", exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(),
            null,
            HttpStatus.SC_METHOD_NOT_ALLOWED,
            exception.getMessage(),
            null);
    return Response.status(Response.Status.METHOD_NOT_ALLOWED).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(UpdateNotAllowedException exception) {
    LOGGER.error(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(), null, HttpStatus.SC_CONFLICT, exception.getCode(), null);
    return Response.status(Response.Status.CONFLICT).entity(problem).build();
  }

  @ServerExceptionMapper
  public Response toResponse(PdfBuilderException exception) {
    LOGGER.error(PREFIX_LOGGER, "PDF generation failed", exception.getMessage());
    Problem problem =
        new Problem(
            exception.getMessage(),
            null,
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            exception.getCode(),
            null);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(problem).build();
  }
}
