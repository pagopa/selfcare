package it.pagopa.selfcare.onboarding.web.handler;

import static org.springframework.http.HttpStatus.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.onboarding.connector.exceptions.CustomVerifyException;
import it.pagopa.selfcare.onboarding.connector.exceptions.InternalGatewayErrorException;
import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import it.pagopa.selfcare.onboarding.connector.exceptions.ManagerNotFoundException;
import it.pagopa.selfcare.onboarding.connector.exceptions.ResourceConflictException;
import it.pagopa.selfcare.onboarding.connector.exceptions.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.connector.exceptions.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.core.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.core.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.core.exception.UpdateNotAllowedException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.springframework.http.HttpStatus;

@Slf4j
@ApplicationScoped
public class OnboardingExceptionMapper {

    @ServerExceptionMapper
    public Response handleInvalidRequestException(InvalidRequestException e) {
        log.warn(e.toString());
        return problemResponse(BAD_REQUEST, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn(e.toString());
        return problemResponse(NOT_FOUND, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleResourceConflictException(ResourceConflictException e) {
        log.warn(e.toString());
        return problemResponse(CONFLICT, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleProductHasNoRelationshipException(ManagerNotFoundException e) {
        log.warn(e.toString());
        return problemResponse(INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleUpdateNotAllowedException(UpdateNotAllowedException e) {
        log.warn(e.toString());
        return problemResponse(CONFLICT, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleInvalidUserFieldsException(InvalidUserFieldsException e) {
        log.warn(e.toString());
        final Problem problem = new Problem(HttpStatus.CONFLICT, e.getMessage());
        if (e.getInvalidFields() != null) {
            problem.setInvalidParams(
                    e.getInvalidFields().stream()
                            .map(invalidField -> new Problem.InvalidParam(invalidField.getName(), invalidField.getReason()))
                            .collect(Collectors.toList()));
        }
        return Response.status(CONFLICT.value())
                .type(MediaType.APPLICATION_JSON)
                .entity(problem)
                .build();
    }

    @ServerExceptionMapper
    public Response handleOnboardingNotAllowedException(OnboardingNotAllowedException e) {
        log.warn(e.toString());
        return problemResponse(FORBIDDEN, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleInternalGatewayErrorException(InternalGatewayErrorException e) {
        log.warn(e.toString());
        return problemResponse(BAD_GATEWAY, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handleUnauthorizedUserException(UnauthorizedUserException e) {
        log.warn(e.toString());
        return problemResponse(FORBIDDEN, e.getMessage());
    }

    @ServerExceptionMapper
    public Response handlePropagatedFrontendException(CustomVerifyException ex) {
        return Response.status(ex.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(ex.getBody())
                .build();
    }

    private Response problemResponse(HttpStatus status, String message) {
        return Response.status(status.value())
                .type(MediaType.APPLICATION_JSON)
                .entity(new Problem(status, message))
                .build();
    }
}
