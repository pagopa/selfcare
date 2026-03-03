package it.pagopa.selfcare.onboarding.exception.handler;

import it.pagopa.selfcare.onboarding.exception.*;
import it.pagopa.selfcare.onboarding.exception.model.Problem;
import it.pagopa.selfcare.onboarding.exception.model.ProblemError;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.List;

@Slf4j
public class ExceptionHandler {

    public static final String SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER = "Something has gone wrong in the server";
    public static final String LOG_ERROR_SYNTAX = "{}: {}";

    @ServerExceptionMapper
    public Response toResponse(InvalidRequestException exception) {
        log.error(LOG_ERROR_SYNTAX, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        Problem problem = problem(exception.getMessage(), Response.Status.BAD_REQUEST.getStatusCode(), exception.getCode());
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(problem)
                .build();
    }
    @ServerExceptionMapper
    public RestResponse<String> toResponse(OnboardingNotAllowedException exception) {
        log.error(LOG_ERROR_SYNTAX, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.BAD_REQUEST, exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<String> toResponse(Exception exception) {
        log.error(LOG_ERROR_SYNTAX, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER);
    }
    @ServerExceptionMapper
    public RestResponse<String> toResponse(ResourceNotFoundException exception) {
        log.error(LOG_ERROR_SYNTAX, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.NOT_FOUND, exception.getMessage());
    }
    @ServerExceptionMapper
    public RestResponse<String> toResponse(ResourceConflictException exception) {
        log.error(LOG_ERROR_SYNTAX, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        return RestResponse.status(Response.Status.CONFLICT, exception.getMessage());
    }
    @ServerExceptionMapper
    public Response toResponse(UpdateNotAllowedException exception) {
        log.error(LOG_ERROR_SYNTAX, SOMETHING_HAS_GONE_WRONG_IN_THE_SERVER, exception.getMessage());
        Problem problem = problem(exception.getMessage(), Response.Status.CONFLICT.getStatusCode(), exception.getCode());
        return Response
                .status(Response.Status.CONFLICT)
                .entity(problem)
                .build();
    }

    private Problem problem(String errorMessage, Integer status, String code) {
        Problem problem = new Problem();
        problem.setStatus(status);
        problem.setErrors(List.of(ProblemError.builder()
                .code(code)
                .detail(errorMessage)
                .build()));
        return problem;
    }
}
