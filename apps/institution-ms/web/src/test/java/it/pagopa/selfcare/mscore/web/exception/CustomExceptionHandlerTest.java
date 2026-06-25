package it.pagopa.selfcare.mscore.web.exception;

import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceConflictException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Constructor;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CustomExceptionHandlerTest {
    /**
     * Method under test: {@link CustomExceptionHandler#handleMissingServletRequestParameter(MissingServletRequestParameterException, HttpHeaders, HttpStatus, WebRequest)}
     */
    @Test
    void testHandleMissingServletRequestParameter() {
        CustomExceptionHandler customExceptionHandler = new CustomExceptionHandler();
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("Parameter Name",
                "Parameter Type");

        HttpHeaders httpHeaders = new HttpHeaders();
        ResponseEntity<Object> actualHandleMissingServletRequestParameterResult = customExceptionHandler
                .handleMissingServletRequestParameter(ex, httpHeaders, HttpStatus.CONTINUE,
                        new ServletWebRequest(new MockHttpServletRequest()));
        assertTrue(actualHandleMissingServletRequestParameterResult.hasBody());
        assertEquals(1, actualHandleMissingServletRequestParameterResult.getHeaders().size());
        assertEquals("application/problem+json", actualHandleMissingServletRequestParameterResult.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        assertEquals(HttpStatus.BAD_REQUEST, actualHandleMissingServletRequestParameterResult.getStatusCode());
        assertEquals(400, ((ProblemDetail) Objects.requireNonNull(actualHandleMissingServletRequestParameterResult.getBody())).getStatus());
    }

    /**
     * Method under test: {@link CustomExceptionHandler#handleMethodArgumentNotValid(MethodArgumentNotValidException, HttpHeaders, HttpStatus, WebRequest)}
     */
    @Test
    void testHandleMethodArgumentNotValid() throws NoSuchMethodException {
        CustomExceptionHandler customExceptionHandler = new CustomExceptionHandler();
        Constructor<?> constructor = CustomExceptionHandler.class.getConstructor();
        MethodParameter parameter = new MethodParameter(constructor,-1);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter,
                new BindException("Target", "Object Name"));

        HttpHeaders headers = new HttpHeaders();
        customExceptionHandler.handleMethodArgumentNotValid(ex, headers, HttpStatus.CONTINUE,
                new ServletWebRequest(new MockHttpServletRequest()));

        HttpHeaders httpHeaders = new HttpHeaders();
        ResponseEntity<Object> actualHandleMissingServletRequestParameterResult = customExceptionHandler
                .handleMethodArgumentNotValid(ex, httpHeaders, HttpStatus.CONTINUE,
                        new ServletWebRequest(new MockHttpServletRequest()));
        assertTrue(actualHandleMissingServletRequestParameterResult.hasBody());
        assertEquals(1, actualHandleMissingServletRequestParameterResult.getHeaders().size());
        assertEquals(HttpStatus.BAD_REQUEST, actualHandleMissingServletRequestParameterResult.getStatusCode());
        assertEquals(400, ((ProblemDetail) Objects.requireNonNull(actualHandleMissingServletRequestParameterResult.getBody())).getStatus());
    }

    /**
     * Method under test: {@link CustomExceptionHandler#handleResourceNotFoundException(HttpServletRequest, ResourceNotFoundException)}
     */
    @Test
    void testHandleResourceNotFoundException() {
        CustomExceptionHandler customExceptionHandler = new CustomExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<ProblemDetail> actualHandleResourceNotFoundExceptionResult = customExceptionHandler
                .handleResourceNotFoundException(request, new ResourceNotFoundException("An error occurred", "Code"));
        assertTrue(actualHandleResourceNotFoundExceptionResult.hasBody());
        assertEquals(1, actualHandleResourceNotFoundExceptionResult.getHeaders().size());
        assertEquals(HttpStatus.NOT_FOUND, actualHandleResourceNotFoundExceptionResult.getStatusCode());
        ProblemDetail body = actualHandleResourceNotFoundExceptionResult.getBody();
        assertEquals(404, Objects.requireNonNull(body).getStatus());
    }

    /**
     * Method under test: {@link CustomExceptionHandler#handleResourceConflictException(HttpServletRequest, ResourceConflictException)}
     */
    @Test
    void testHandleResourceConflictException() {
        CustomExceptionHandler customExceptionHandler = new CustomExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<ProblemDetail> actualHandleResourceConflictExceptionResult = customExceptionHandler
                .handleResourceConflictException(request, new ResourceConflictException("An error occurred", "Code"));
        assertTrue(actualHandleResourceConflictExceptionResult.hasBody());
        assertEquals(1, actualHandleResourceConflictExceptionResult.getHeaders().size());
        assertEquals(HttpStatus.CONFLICT, actualHandleResourceConflictExceptionResult.getStatusCode());
        ProblemDetail body = actualHandleResourceConflictExceptionResult.getBody();
        assertEquals(409, Objects.requireNonNull(body).getStatus());
    }

    /**
     * Method under test: {@link CustomExceptionHandler#handleInvalidRequestException(HttpServletRequest, InvalidRequestException)}
     */
    @Test
    void testHandleInvalidRequestException() {
        CustomExceptionHandler customExceptionHandler = new CustomExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<ProblemDetail> actualHandleInvalidRequestExceptionResult = customExceptionHandler
                .handleInvalidRequestException(request, new InvalidRequestException("An error occurred", "Code"));
        assertTrue(actualHandleInvalidRequestExceptionResult.hasBody());
        assertEquals(1, actualHandleInvalidRequestExceptionResult.getHeaders().size());
        assertEquals(HttpStatus.BAD_REQUEST, actualHandleInvalidRequestExceptionResult.getStatusCode());
        ProblemDetail body = actualHandleInvalidRequestExceptionResult.getBody();
        assertEquals(400, Objects.requireNonNull(body).getStatus());
    }

    /**
     * Method under test: {@link CustomExceptionHandler#handleException(HttpServletRequest, Exception)}
     */
    @Test
    void testHandleException() {
        CustomExceptionHandler customExceptionHandler = new CustomExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<ProblemDetail> actualHandleExceptionResult = customExceptionHandler.handleException(request,
                new Exception());
        assertTrue(actualHandleExceptionResult.hasBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, actualHandleExceptionResult.getStatusCode());
        ProblemDetail body = actualHandleExceptionResult.getBody();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
    }

    @Test
    void handleMsCoreException() {
        CustomExceptionHandler customExceptionHandler = new CustomExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<ProblemDetail> actualHandleInvalidRequestExceptionResult = customExceptionHandler
                .handleMsCoreException(request, new MsCoreException("An error occurred", "Code"));
        assertTrue(actualHandleInvalidRequestExceptionResult.hasBody());
        assertEquals(1, actualHandleInvalidRequestExceptionResult.getHeaders().size());
    }

    /**
     * Method under test: {@link CustomExceptionHandler#handleValidationException(HttpServletRequest, ValidationException)}
     */
    @Test
    void testHandleValidationException() {
        CustomExceptionHandler customExceptionHandler = new CustomExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<ProblemDetail> actualHandleInvalidRequestExceptionResult = customExceptionHandler
                .handleValidationException(request, new ValidationException("An error occurred"));
        assertTrue(actualHandleInvalidRequestExceptionResult.hasBody());
        assertEquals(1, actualHandleInvalidRequestExceptionResult.getHeaders().size());
        assertEquals(HttpStatus.BAD_REQUEST, actualHandleInvalidRequestExceptionResult.getStatusCode());
        ProblemDetail body = actualHandleInvalidRequestExceptionResult.getBody();
        assertEquals(400, Objects.requireNonNull(body).getStatus());
    }


}

