package it.pagopa.selfcare.auth.exception.handler;

import it.pagopa.selfcare.auth.controller.response.Problem;
import it.pagopa.selfcare.auth.exception.*;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
    public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER = "Something has gone wrong in the server";
    public static final String FORBIDDEN = "Forbidden";
    public static final String PREFIX_LOGGER = "{}: {}";

    @ServerExceptionMapper
    public RestResponse<String> toResponse(InvalidRequestException exception) {
        LOGGER.warn(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST, exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> toResponse(Exception exception) {
        LOGGER.error(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER);
    }

    @ServerExceptionMapper
    public Response toResponse(ResourceNotFoundException exception) {
        LOGGER.warn(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        Problem problem = new Problem(exception.getMessage(), null, HttpStatus.SC_NOT_FOUND, exception.getMessage(), null);
        return Response.status(Response.Status.NOT_FOUND).entity(problem).build();
    }

    @ServerExceptionMapper
    public Response toResponse(ForbiddenException exception) {
        LOGGER.warn(PREFIX_LOGGER, FORBIDDEN, exception.getMessage());
        Problem problem = new Problem(exception.getMessage(), null,  HttpStatus.SC_FORBIDDEN, exception.getMessage(), null);
        return Response.status(Response.Status.FORBIDDEN).entity(problem).build();
    }

    @ServerExceptionMapper
    public Response toResponse(InternalException exception) {
        LOGGER.error(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        Problem problem = new Problem(exception.getMessage(), null,  HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.getMessage(), null);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(problem).build();
    }

    @ServerExceptionMapper
    public Response toResponse(UnimplementedException exception) {
        LOGGER.error(PREFIX_LOGGER, "Unimplemented endpoint", exception.getMessage());
        Problem problem = new Problem(exception.getMessage(), null,  HttpStatus.SC_NOT_IMPLEMENTED, exception.getMessage(), null);
        return Response.status(Response.Status.NOT_IMPLEMENTED).entity(problem).build();
    }
}
