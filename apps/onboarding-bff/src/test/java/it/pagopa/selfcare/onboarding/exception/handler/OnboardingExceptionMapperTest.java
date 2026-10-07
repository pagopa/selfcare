package it.pagopa.selfcare.onboarding.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
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
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingExceptionMapperTest {

    private static final String DETAIL_MESSAGE = "detail message";
    private static final String DOWNSTREAM_DETAIL = "An error occurred during a downstream service request";

    private final OnboardingExceptionMapper mapper = new OnboardingExceptionMapper();
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() {
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/v1/institutions/abc?productId=prod-io"));
    }

    @Test
    void invalidRequestIs400WithProblemBody() {
        Response response = mapper.handleInvalidRequestException(new InvalidRequestException(DETAIL_MESSAGE), uriInfo);

        assertProblem(response, 400, "Bad Request", DETAIL_MESSAGE);
    }

    @Test
    void resourceNotFoundIs404() {
        assertProblem(mapper.handleResourceNotFoundException(new ResourceNotFoundException(DETAIL_MESSAGE), uriInfo),
                404, "Not Found", DETAIL_MESSAGE);
    }

    @Test
    void resourceConflictIs409() {
        assertProblem(mapper.handleResourceConflictException(new ResourceConflictException(DETAIL_MESSAGE), uriInfo),
                409, "Conflict", DETAIL_MESSAGE);
    }

    @Test
    void updateNotAllowedIs409() {
        assertProblem(mapper.handleUpdateNotAllowedException(new UpdateNotAllowedException(DETAIL_MESSAGE), uriInfo),
                409, "Conflict", DETAIL_MESSAGE);
    }

    @Test
    void managerNotFoundIs500() {
        assertProblem(mapper.handleProductHasNoRelationshipException(new ManagerNotFoundException(DETAIL_MESSAGE), uriInfo),
                500, "Internal Server Error", DETAIL_MESSAGE);
    }

    @Test
    void onboardingNotAllowedIs403() {
        assertProblem(mapper.handleOnboardingNotAllowedException(new OnboardingNotAllowedException(DETAIL_MESSAGE), uriInfo),
                403, "Forbidden", DETAIL_MESSAGE);
    }

    @Test
    void unauthorizedUserIs403() {
        assertProblem(mapper.handleUnauthorizedUserException(new UnauthorizedUserException(DETAIL_MESSAGE), uriInfo),
                403, "Forbidden", DETAIL_MESSAGE);
    }

    @Test
    void accessDeniedIs403() {
        assertProblem(mapper.handleAccessDeniedException(new AccessDeniedException("Access Denied"), uriInfo),
                403, "Forbidden", "Access Denied");
    }

    @Test
    void internalGatewayErrorIs502WithoutDetail() {
        Response response = mapper.handleInternalGatewayErrorException(new InternalGatewayErrorException("raw body"), uriInfo);

        Problem body = assertProblem(response, 502, "Bad Gateway", null);
        assertNull(body.getDetail());
    }

    @Test
    void invalidUserFieldsIs409WithInvalidParams() {
        InvalidUserFieldsException exception = new InvalidUserFieldsException(
                List.of(new InvalidUserFieldsException.InvalidField("name", "reason")));

        Response response = mapper.handleInvalidUserFieldsException(exception, uriInfo);

        Problem body = assertProblem(response, 409, "Conflict", exception.getMessage());
        assertEquals(1, body.getInvalidParams().size());
        assertEquals("name", body.getInvalidParams().get(0).getName());
        assertEquals("reason", body.getInvalidParams().get(0).getReason());
    }

    @Test
    void customVerifyExceptionBodyIsForwardedVerbatim() {
        String body = "{\"errors\":[{\"code\":\"X\"}]}";

        Response response = mapper.handlePropagatedFrontendException(new CustomVerifyException(422, body));

        assertEquals(422, response.getStatus());
        assertEquals(body, response.getEntity());
        assertEquals("application/json", response.getMediaType().toString());
    }

    @Test
    void transportFailuresAre500WithGenericDownstreamDetail() {
        assertProblem(mapper.handleProcessingException(new ProcessingException(new ConnectException("refused")), uriInfo),
                500, "Internal Server Error", DOWNSTREAM_DETAIL);
        assertProblem(mapper.handleIOException(new IOException("closed"), uriInfo),
                500, "Internal Server Error", DOWNSTREAM_DETAIL);
    }

    @Test
    void unexpectedFailureIs500WithMessage() {
        assertProblem(mapper.handleThrowable(new IllegalStateException("boom"), uriInfo),
                500, "Internal Server Error", "boom");
    }

    @Test
    void downstreamAuthAndThrottlingErrorsKeepStatusWithGenericDetail() {
        for (int status : new int[]{401, 403, 422, 429}) {
            Response response = mapper.handleDownstreamServiceException(new DownstreamServiceException(status, "ignored"), uriInfo);

            assertEquals(status, response.getStatus());
            Problem body = (Problem) response.getEntity();
            assertEquals(DOWNSTREAM_DETAIL, body.getDetail());
            assertEquals(status, body.getStatus());
            assertEquals("/v1/institutions/abc", body.getInstance());
            assertEquals("application/problem+json", response.getMediaType().toString());
        }
    }

    @Test
    void unmappableDownstreamStatusFallsBackTo500() {
        Response response = mapper.handleDownstreamServiceException(new DownstreamServiceException(499, "x"), uriInfo);

        assertProblem(response, 500, "Internal Server Error", DOWNSTREAM_DETAIL);
    }

    @Test
    void checkedTransportFailuresWrappedByMutinyAre500WithGenericDetail() {
        assertProblem(mapper.handleThrowable(new CompletionException(new IOException("Connection was closed")), uriInfo),
                500, "Internal Server Error", DOWNSTREAM_DETAIL);
        assertProblem(mapper.handleThrowable(new CompletionException(new TimeoutException("read timed out")), uriInfo),
                500, "Internal Server Error", DOWNSTREAM_DETAIL);
        assertProblem(mapper.handleThrowable(new ExecutionException(new CompletionException(new IOException("x"))), uriInfo),
                500, "Internal Server Error", DOWNSTREAM_DETAIL);
    }

    @Test
    void wrappedUnexpectedFailureKeepsTheWrapperMessage() {
        Response response = mapper.handleThrowable(new CompletionException(new IllegalStateException("boom")), uriInfo);

        assertProblem(response, 500, "Internal Server Error", "java.lang.IllegalStateException: boom");
    }

    @Test
    void unacceptableRepresentationIs406() {
        Response response = mapper.handleWebApplicationException(new NotAcceptableException(), requestContext());

        assertProblem(response, 406, "Not Acceptable", "No acceptable representation");
    }

    @Test
    void invalidJsonValueIs400ProblemInsteadOfTheBuiltinBareBody() throws Exception {
        MismatchedInputException failure = org.junit.jupiter.api.Assertions.assertThrows(MismatchedInputException.class,
                () -> new ObjectMapper().readValue("{\"userId\":\"not-a-uuid\"}",
                        new TypeReference<java.util.Map<String, java.util.UUID>>() { }));

        Response response = mapper.handleMismatchedInputException(failure, uriInfo);

        assertEquals(400, response.getStatus());
        assertEquals("application/problem+json", response.getMediaType().toString());
        Problem body = (Problem) response.getEntity();
        assertEquals("Bad Request", body.getTitle());
        assertTrue(body.getDetail().startsWith("JSON parse error: Cannot deserialize value of type `java.util.UUID`"));
        assertEquals("/v1/institutions/abc", body.getInstance());
    }

    @Test
    void unknownRouteIs400NoStaticResource() {
        Response response = mapper.handleWebApplicationException(new NotFoundException(), requestContext());

        assertEquals(400, response.getStatus());
        Problem body = (Problem) response.getEntity();
        assertEquals("No static resource v1/institutions/abc.", body.getDetail());
    }

    @Test
    void methodNotAllowedIs405() {
        ContainerRequestContext requestContext = requestContext();
        when(requestContext.getMethod()).thenReturn("PATCH");

        Response response = mapper.handleWebApplicationException(new NotAllowedException("GET"), requestContext);

        assertEquals(405, response.getStatus());
        assertEquals("Request method 'PATCH' is not supported", ((Problem) response.getEntity()).getDetail());
    }

    @Test
    void nestedValidationPathsPutIndexesAndMapKeysBeforeTheNestedProperty() throws Exception {
        UsersBody request = new UsersBody(
                List.of(new UserInput("")),
                Map.of("primary", new UserInput("")));

        Problem body = assertProblem(invalidBody(request), 400, "Bad Request", "Validation failed");

        assertEquals(
                Set.of("usersBody.users[0].name", "usersBody.contacts[primary].name"),
                body.getInvalidParams().stream().map(Problem.InvalidParam::getName).collect(Collectors.toSet()));
    }

    @Test
    void containerElementValidationUsesTheElementIndexWithoutItsSyntheticName() throws Exception {
        Problem body = assertProblem(
                invalidBody(new TagsBody(List.of("valid", ""))), 400, "Bad Request", "Validation failed");

        assertEquals(List.of("tagsBody.tags[1]"),
                body.getInvalidParams().stream().map(Problem.InvalidParam::getName).toList());
    }

    @Test
    void bodyNamesPreserveLeadingAcronymsLikeSpringPropertyNames() throws Exception {
        Problem body = assertProblem(invalidBody(new URLRequest("")), 400, "Bad Request", "Validation failed");

        assertEquals("URLRequest.value", body.getInvalidParams().get(0).getName());
    }

    private Response invalidBody(Object request) throws Exception {
        try (ValidatorFactory factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory()) {
            var violations = factory.getValidator().forExecutables().validateParameters(
                    new ValidationEndpoint(),
                    ValidationEndpoint.class.getMethod("submit", Object.class),
                    new Object[]{request});
            assertTrue(!violations.isEmpty());
            return mapper.handleConstraintViolationException(new ConstraintViolationException(violations), uriInfo);
        }
    }

    public static class ValidationEndpoint {
        public void submit(@Valid Object request) {
        }
    }

    record UserInput(@NotBlank String name) {
    }

    record UsersBody(@Valid List<UserInput> users, @Valid Map<String, UserInput> contacts) {
    }

    record TagsBody(List<@NotBlank String> tags) {
    }

    record URLRequest(@NotBlank String value) {
    }

    private Problem assertProblem(Response response, int status, String title, String detail) {
        assertNotNull(response);
        assertEquals(status, response.getStatus());
        assertEquals("application/problem+json", response.getMediaType().toString());
        Problem body = (Problem) response.getEntity();
        assertNotNull(body);
        assertEquals(status, body.getStatus());
        assertEquals(title, body.getTitle());
        assertEquals(detail, body.getDetail());
        assertEquals("/v1/institutions/abc", body.getInstance());
        assertTrue(body.getInvalidParams() == null || !body.getInvalidParams().isEmpty());
        return body;
    }

    private ContainerRequestContext requestContext() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        return requestContext;
    }

}
