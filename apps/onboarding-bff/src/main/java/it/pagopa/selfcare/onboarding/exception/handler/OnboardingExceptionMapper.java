package it.pagopa.selfcare.onboarding.exception.handler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import it.pagopa.selfcare.onboarding.exception.CustomVerifyException;
import it.pagopa.selfcare.onboarding.exception.InternalGatewayErrorException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ManagerNotFoundException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.model.Problem;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Slf4j
@ApplicationScoped
public class OnboardingExceptionMapper {

    @ServerExceptionMapper
    public Response handleInvalidRequestException(InvalidRequestException e) {
        log.warn(e.toString());
        return problemResponse(Response.Status.BAD_REQUEST, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn(e.toString());
        return problemResponse(Response.Status.NOT_FOUND, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleResourceConflictException(ResourceConflictException e) {
        log.warn(e.toString());
        return problemResponse(Response.Status.CONFLICT, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleProductHasNoRelationshipException(ManagerNotFoundException e) {
        log.warn(e.toString());
        return problemResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleUpdateNotAllowedException(UpdateNotAllowedException e) {
        log.warn(e.toString());
        return problemResponse(Response.Status.CONFLICT, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleInvalidUserFieldsException(InvalidUserFieldsException e) {
        log.warn(e.toString());
        final Problem problem = buildProblem(Response.Status.CONFLICT, e.getMessage());
        if (e.getInvalidFields() != null) {
            problem.setInvalidParams(
                    e.getInvalidFields().stream()
                            .map(invalidField -> new Problem.InvalidParam(invalidField.getName(), invalidField.getReason()))
                            .collect(Collectors.toList()));
        }
        return Response.status(Response.Status.CONFLICT.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(problem)
                .build();
    }

    @ServerExceptionMapper
    public Response handleOnboardingNotAllowedException(OnboardingNotAllowedException e) {
        log.warn(e.toString());
        return problemResponse(Response.Status.FORBIDDEN, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleInternalGatewayErrorException(InternalGatewayErrorException e) {
        log.warn(e.toString());
        return problemResponse(Response.Status.BAD_GATEWAY, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleUnauthorizedUserException(UnauthorizedUserException e) {
        log.warn(e.toString());
        return problemResponse(Response.Status.FORBIDDEN, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handlePropagatedFrontendException(CustomVerifyException ex) {
        return Response.status(ex.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(ex.getBody())
                .build();
    }

    private Response problemResponse(Response.Status status, String message) {
        return Response.status(status.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .entity(buildProblem(status, message))
                .build();
    }

    private Problem buildProblem(Response.Status status, String message) {
        Problem problem = new Problem();
        problem.setStatus(status.getStatusCode());
        problem.setTitle(status.getReasonPhrase());
        problem.setDetail(message);
        return problem;
    }
}
