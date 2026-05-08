package it.pagopa.selfcare.dashboard.handler;

import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.dashboard.exception.BadGatewayException;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.exception.SupportException;
import it.pagopa.selfcare.dashboard.exception.handler.DashboardExceptionsHandler;
import it.pagopa.selfcare.dashboard.exception.FileValidationException;
import it.pagopa.selfcare.dashboard.exception.InvalidProductRoleException;
import it.pagopa.selfcare.dashboard.exception.InvalidUserGroupException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import jakarta.validation.ValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.*;

class DashboardExceptionsHandlerTest {

    private static final String DETAIL_MESSAGE = "detail message";

    private final DashboardExceptionsHandler handler;


    public DashboardExceptionsHandlerTest() {
        this.handler = new DashboardExceptionsHandler();
    }


    @ParameterizedTest
    @ValueSource(classes = {
            FileValidationException.class,
            InvalidProductRoleException.class,
            ValidationException.class
    })
    void handleBadRequestException(Class<?> clazz) {
        // given
        Exception exceptionMock = (Exception) Mockito.mock(clazz);
        Mockito.when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        // when
        ResponseEntity<Problem> responseEntity = handler.handleBadRequestException(exceptionMock);
        // then
        assertNotNull(responseEntity);
        assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(BAD_REQUEST.value(), responseEntity.getBody().getStatus());
    }

    @ParameterizedTest
    @ValueSource(classes = {
            ResourceNotFoundException.class,
            InvalidUserGroupException.class
    })
    void handleNotFoundException(Class<?> clazz) {
        // given
        Exception exceptionMock = (Exception) Mockito.mock(clazz);
        Mockito.when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        // when
        ResponseEntity<Problem> responseEntity = handler.handleNotFoundException(exceptionMock);
        // then
        assertNotNull(responseEntity);
        assertEquals(NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(NOT_FOUND.value(), responseEntity.getBody().getStatus());
    }

    @ParameterizedTest
    @ValueSource(classes = {
            BadGatewayException.class,
    })
    void handleBadGatewayException(Class<?> clazz) {
        // given
        Exception exceptionMock = (Exception) Mockito.mock(clazz);
        Mockito.when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        // when
        ResponseEntity<Problem> responseEntity = handler.handleBadGatewayException(exceptionMock);
        // then
        assertNotNull(responseEntity);
        assertEquals(BAD_GATEWAY, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(BAD_GATEWAY.value(), responseEntity.getBody().getStatus());
    }

    @ParameterizedTest
    @ValueSource(classes = {
            SupportException.class
    })
    void handleSupportException(Class<?> clazz) {
        // given
        Exception exceptionMock = (Exception) Mockito.mock(clazz);
        Mockito.when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        // when
        ResponseEntity<Problem> responseEntity = handler.handleSupportException(exceptionMock);
        // then
        assertNotNull(responseEntity);
        assertEquals(INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(INTERNAL_SERVER_ERROR.value(), responseEntity.getBody().getStatus());
    }

    @ParameterizedTest
    @ValueSource(classes = {
            AccessDeniedException.class
    })
    void handleAccessDeniedException(Class<?> clazz) {
        // given
        AccessDeniedException exceptionMock = (AccessDeniedException) Mockito.mock(clazz);
        Mockito.when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        // when
        ResponseEntity<Problem> responseEntity = handler.handleAccessDeniedException(exceptionMock);
        // then
        assertNotNull(responseEntity);
        assertEquals(FORBIDDEN, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(FORBIDDEN.value(), responseEntity.getBody().getStatus());
    }

}