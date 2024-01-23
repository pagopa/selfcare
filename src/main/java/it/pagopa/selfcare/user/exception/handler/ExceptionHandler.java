package it.pagopa.selfcare.user.exception.handler;

import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.model.error.Problem;
import it.pagopa.selfcare.user.model.error.ProblemError;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
    public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER = "Something has gone wrong in the server";

    @ServerExceptionMapper
    public RestResponse<String> toResponse(InvalidRequestException exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST, exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> toResponse(Exception exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER);
    }

    @ServerExceptionMapper
    public Response toResponse(ResourceNotFoundException exception) {
        LOGGER.error("{}: {}", SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        Problem problem = createProblem(exception.getMessage(), HttpStatus.SC_NOT_FOUND, exception.getCode());
        return Response.status(Response.Status.NOT_FOUND).entity(problem).build();
    }

    private Problem createProblem(String errorMessage, Integer status, String code) {
        Problem problem = new Problem();
        problem.setStatus(status);
        problem.setErrors(createProblemError(errorMessage,code));
        return problem;
    }

    private List<ProblemError> createProblemError(String message, String code) {
        List<ProblemError> list = new ArrayList<>();
        list.add(ProblemError.builder()
                .code(code)
                .detail(message)
                .build());
        return list;
    }
}
