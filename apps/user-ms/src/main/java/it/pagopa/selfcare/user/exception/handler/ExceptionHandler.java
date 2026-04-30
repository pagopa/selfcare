package it.pagopa.selfcare.user.exception.handler;

import it.pagopa.selfcare.user.exception.ConflictException;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.openapi.quarkus.user_registry_json.model.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
    public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER = "Something has gone wrong in the server";
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
        Problem problem = new Problem(exception.getMessage(), null, null, HttpStatus.SC_NOT_FOUND, exception.getMessage(), null);
        return Response.status(Response.Status.NOT_FOUND).entity(problem).build();
    }

    @ServerExceptionMapper
    public Response toResponse(ConflictException exception) {
        LOGGER.warn(PREFIX_LOGGER, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        Problem problem = new Problem(exception.getMessage(), null, null, HttpStatus.SC_CONFLICT, exception.getMessage(), null);
        return Response.status(Response.Status.CONFLICT).entity(problem).build();
    }
}
