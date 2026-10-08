package it.pagopa.selfcare.onboarding.exception.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.vertx.core.http.HttpClosedException;
import it.pagopa.selfcare.onboarding.exception.AccessDeniedException;
import it.pagopa.selfcare.onboarding.exception.CustomVerifyException;
import it.pagopa.selfcare.onboarding.exception.DownstreamServiceException;
import it.pagopa.selfcare.onboarding.exception.InternalGatewayErrorException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.exception.ManagerNotFoundException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.exception.UnauthorizedUserException;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.model.error.Problem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.beans.Introspector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Translates every failure into the {@code application/problem+json} contract of the Spring BFF.
 * Errors raised by downstream REST clients follow the former {@code FeignErrorDecoder} rules.
 */
@Slf4j
@ApplicationScoped
public class OnboardingExceptionMapper {

    static final String DOWNSTREAM_ERROR_DETAIL = "An error occurred during a downstream service request";
    private static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    private static final int INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    @ServerExceptionMapper
    public Response handleThrowable(Throwable e, UriInfo uriInfo) {
        Throwable cause = unwrapAsyncFailure(e);
        if (isTransportFailure(cause)) {
            return downstreamProblem(cause, -1, uriInfo);
        }
        log.error("unhandled exception: ", e);
        return ProblemResponses.problem(INTERNAL_SERVER_ERROR, e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleInvalidRequestException(InvalidRequestException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(BAD_REQUEST, e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleResourceNotFoundException(ResourceNotFoundException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(Response.Status.NOT_FOUND.getStatusCode(), e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleResourceConflictException(ResourceConflictException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(Response.Status.CONFLICT.getStatusCode(), e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleProductHasNoRelationshipException(ManagerNotFoundException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(INTERNAL_SERVER_ERROR, e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleUpdateNotAllowedException(UpdateNotAllowedException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(Response.Status.CONFLICT.getStatusCode(), e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleInvalidUserFieldsException(InvalidUserFieldsException e, UriInfo uriInfo) {
        log.warn(e.toString());
        List<Problem.InvalidParam> invalidParams = e.getInvalidFields() == null ? null
                : e.getInvalidFields().stream()
                .map(invalidField -> new Problem.InvalidParam(invalidField.getName(), invalidField.getReason()))
                .collect(Collectors.toList());
        return ProblemResponses.problem(Response.Status.CONFLICT.getStatusCode(), e.getMessage(), uriInfo, invalidParams);
    }

    @ServerExceptionMapper
    public Response handleOnboardingNotAllowedException(OnboardingNotAllowedException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(Response.Status.FORBIDDEN.getStatusCode(), e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleInternalGatewayErrorException(InternalGatewayErrorException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(Response.Status.BAD_GATEWAY.getStatusCode(), null, uriInfo);
    }

    @ServerExceptionMapper
    public Response handleUnauthorizedUserException(UnauthorizedUserException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(Response.Status.FORBIDDEN.getStatusCode(), e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleAccessDeniedException(AccessDeniedException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(Response.Status.FORBIDDEN.getStatusCode(), e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handlePropagatedFrontendException(CustomVerifyException ex) {
        return verbatim(ex.getStatus(), ex.getBody());
    }

    @ServerExceptionMapper
    public Response handleDownstreamServiceException(DownstreamServiceException e, UriInfo uriInfo) {
        return downstreamProblem(e, e.getStatus(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleProcessingException(ProcessingException e, UriInfo uriInfo) {
        return downstreamProblem(e, -1, uriInfo);
    }

    @ServerExceptionMapper
    public Response handleIOException(IOException e, UriInfo uriInfo) {
        return downstreamProblem(e, -1, uriInfo);
    }

    @ServerExceptionMapper
    public Response handleWebApplicationException(WebApplicationException e, ContainerRequestContext requestContext) {
        UriInfo uriInfo = requestContext == null ? null : requestContext.getUriInfo();
        return serverError(e, requestContext, uriInfo);
    }

    @ServerExceptionMapper
    public Response handleConstraintViolationException(ConstraintViolationException e, UriInfo uriInfo) {
        log.warn(e.toString());
        List<Problem.InvalidParam> invalidParams = requestBodyViolations(e);
        if (invalidParams != null) {
            return ProblemResponses.problem(BAD_REQUEST, "Validation failed", uriInfo, invalidParams);
        }
        return ProblemResponses.problem(BAD_REQUEST, e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleValidationException(ValidationException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(BAD_REQUEST, e.getMessage(), uriInfo);
    }

    @ServerExceptionMapper
    public Response handleJsonProcessingException(JsonProcessingException e, UriInfo uriInfo) {
        log.warn(e.toString());
        return ProblemResponses.problem(BAD_REQUEST, "JSON parse error: " + e.getOriginalMessage(), uriInfo);
    }

    // Overrides the quarkus-rest-jackson built-in mapper, which answers with a bare, non problem+json body
    @ServerExceptionMapper
    public Response handleMismatchedInputException(MismatchedInputException e, UriInfo uriInfo) {
        return handleJsonProcessingException(e, uriInfo);
    }

    private Response downstreamProblem(Throwable e, int downstreamStatus, UriInfo uriInfo) {
        int status = ProblemResponses.downstreamErrorStatus(downstreamStatus);
        if (status >= 500) {
            log.error("unhandled exception: ", e);
        } else {
            log.warn(e.toString());
        }
        return ProblemResponses.problem(status, DOWNSTREAM_ERROR_DETAIL, uriInfo);
    }

    private Response serverError(WebApplicationException e, ContainerRequestContext requestContext, UriInfo uriInfo) {
        log.warn(e.toString());
        int status = e.getResponse() == null ? INTERNAL_SERVER_ERROR : e.getResponse().getStatus();
        if (e.getCause() instanceof JsonProcessingException jsonException) {
            return ProblemResponses.problem(BAD_REQUEST, "JSON parse error: " + jsonException.getOriginalMessage(), uriInfo);
        }
        if (e instanceof NotFoundException) {
            String path = ProblemResponses.instance(uriInfo);
            String resource = path == null ? "" : path.replaceFirst("^/", "");
            return ProblemResponses.problem(BAD_REQUEST, "No static resource " + resource + ".", uriInfo);
        }
        if (e instanceof NotAllowedException) {
            String method = requestContext == null ? null : requestContext.getMethod();
            return ProblemResponses.problem(Response.Status.METHOD_NOT_ALLOWED.getStatusCode(),
                    "Request method '" + method + "' is not supported", uriInfo);
        }
        if (e instanceof NotAcceptableException) {
            return ProblemResponses.problem(Response.Status.NOT_ACCEPTABLE.getStatusCode(), "No acceptable representation", uriInfo);
        }
        if (e instanceof NotSupportedException) {
            String contentType = requestContext == null ? null : requestContext.getHeaderString("Content-Type");
            return ProblemResponses.problem(BAD_REQUEST, "Content-Type '" + contentType + "' is not supported", uriInfo);
        }
        if (status < 400 || ProblemResponses.reasonPhrase(status) == null) {
            return ProblemResponses.problem(INTERNAL_SERVER_ERROR, e.getMessage(), uriInfo);
        }
        return ProblemResponses.problem(status, e.getMessage(), uriInfo);
    }

    private static Response verbatim(int status, String body) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    // Mutiny wraps checked failures (IO errors, timeouts) of the blocking client calls
    private static Throwable unwrapAsyncFailure(Throwable e) {
        Throwable current = e;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isTransportFailure(Throwable e) {
        return e instanceof ProcessingException || e instanceof IOException || e instanceof TimeoutException
                || e instanceof HttpClosedException;
    }

    /**
     * Violations raised on a request body are reported as {@code <bodyName>.<field path>} (the Spring
     * {@code MethodArgumentNotValidException} naming); null when any violation is not a body field violation.
     */
    private static List<Problem.InvalidParam> requestBodyViolations(ConstraintViolationException e) {
        List<Problem.InvalidParam> invalidParams = new ArrayList<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            List<Path.Node> nodes = new ArrayList<>();
            violation.getPropertyPath().forEach(nodes::add);
            if (nodes.size() < 3 || nodes.get(1).getKind() != ElementKind.PARAMETER
                    || nodes.get(2).getKind() == ElementKind.PARAMETER) {
                return null;
            }
            Path.ParameterNode parameter = nodes.get(1).as(Path.ParameterNode.class);
            Object[] arguments = violation.getExecutableParameters();
            Object body = arguments != null && parameter.getParameterIndex() < arguments.length
                    ? arguments[parameter.getParameterIndex()] : null;
            if (body == null) {
                return null;
            }
            invalidParams.add(new Problem.InvalidParam(
                    Introspector.decapitalize(body.getClass().getSimpleName()) + "." + fieldPath(nodes.subList(2, nodes.size())),
                    violation.getMessage()));
        }
        return invalidParams.isEmpty() ? null : invalidParams;
    }

    private static String fieldPath(List<Path.Node> nodes) {
        StringBuilder path = new StringBuilder();
        for (Path.Node node : nodes) {
            if (node.isInIterable() && !path.isEmpty()) {
                path.append('[');
                if (node.getIndex() != null) {
                    path.append(node.getIndex());
                } else if (node.getKey() != null) {
                    path.append(node.getKey());
                }
                path.append(']');
            }
            if (node.getKind() != ElementKind.PROPERTY || node.getName() == null) {
                continue;
            }
            if (!path.isEmpty()) {
                path.append('.');
            }
            path.append(node.getName());
        }
        return path.toString();
    }
}
