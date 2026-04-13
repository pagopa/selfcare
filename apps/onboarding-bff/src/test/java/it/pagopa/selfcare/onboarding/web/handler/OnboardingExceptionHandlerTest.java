package it.pagopa.selfcare.onboarding.web.handler;

import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.onboarding.connector.exceptions.*;
import it.pagopa.selfcare.onboarding.core.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.core.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.core.exception.UpdateNotAllowedException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnboardingExceptionHandlerTest {

    private static final String DETAIL_MESSAGE = "detail message";

    private final OnboardingExceptionMapper handler;

    public OnboardingExceptionHandlerTest() {
        this.handler = new OnboardingExceptionMapper();
    }


    @Test
    void handleInvalidRequestException() {
        //given
        InvalidRequestException exceptionMock = mock(InvalidRequestException.class);
        when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        Response responseEntity = handler.handleInvalidRequestException(exceptionMock);
        //then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), responseEntity.getStatus());
        Problem body = (Problem) responseEntity.getEntity();
        assertNotNull(body);
        assertEquals(DETAIL_MESSAGE, body.getDetail());
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), body.getStatus());
    }

    @Test
    void handleInternalGatewayErrorException() {
        //given
        InternalGatewayErrorException exceptionMock = mock(InternalGatewayErrorException.class);
        when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        Response responseEntity = handler.handleInternalGatewayErrorException(exceptionMock);
        //then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), responseEntity.getStatus());
        Problem body = (Problem) responseEntity.getEntity();
        assertNotNull(body);
        assertEquals(DETAIL_MESSAGE, body.getDetail());
        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), body.getStatus());
    }


    @Test
    void handleResourceNotFoundException() {
        //given
        ResourceNotFoundException exceptionMock = mock(ResourceNotFoundException.class);
        when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        Response responseEntity = handler.handleResourceNotFoundException(exceptionMock);
        //then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseEntity.getStatus());
        Problem body = (Problem) responseEntity.getEntity();
        assertNotNull(body);
        assertEquals(DETAIL_MESSAGE, body.getDetail());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), body.getStatus());
    }


    @Test
    void handleProductHasNoRelationshipException() {
        //given
        ManagerNotFoundException exceptionMock = mock(ManagerNotFoundException.class);
        when(exceptionMock.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        Response responseEntity = handler.handleProductHasNoRelationshipException(exceptionMock);
        //then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), responseEntity.getStatus());
        Problem body = (Problem) responseEntity.getEntity();
        assertNotNull(body);
        assertEquals(DETAIL_MESSAGE, body.getDetail());
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), body.getStatus());
    }


    @Test
    void handleUpdateNotAllowedException() {
        // given
        UpdateNotAllowedException mockException = mock(UpdateNotAllowedException.class);
        when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        // when
        Response responseEntity = handler.handleUpdateNotAllowedException(mockException);
        // then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), responseEntity.getStatus());
        Problem body = (Problem) responseEntity.getEntity();
        assertNotNull(body);
        assertEquals(DETAIL_MESSAGE, body.getDetail());
        assertEquals(Response.Status.CONFLICT.getStatusCode(), body.getStatus());
    }


    @Test
    void handleInvalidUserFieldsException() {
        // given
        InvalidUserFieldsException mockException = mock(InvalidUserFieldsException.class);
        when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        final InvalidUserFieldsException.InvalidField invalidField = new InvalidUserFieldsException.InvalidField("name", "reason");
        when(mockException.getInvalidFields())
                .thenReturn(List.of(invalidField));
        // when
        Response responseEntity = handler.handleInvalidUserFieldsException(mockException);
        // then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), responseEntity.getStatus());
        Problem body = (Problem) responseEntity.getEntity();
        assertNotNull(body);
        assertEquals(DETAIL_MESSAGE, body.getDetail());
        assertEquals(Response.Status.CONFLICT.getStatusCode(), body.getStatus());
        assertNotNull(body.getInvalidParams());
        assertEquals(1, body.getInvalidParams().size());
        assertEquals(invalidField.getName(), body.getInvalidParams().get(0).getName());
        assertEquals(invalidField.getReason(), body.getInvalidParams().get(0).getReason());
    }


    @Test
    void handleOnboardingNotAllowedException() {
        // given
        OnboardingNotAllowedException mockException = mock(OnboardingNotAllowedException.class);
        when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        // when
        Response responseEntity = handler.handleOnboardingNotAllowedException(mockException);
        // then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), responseEntity.getStatus());
        Problem body = (Problem) responseEntity.getEntity();
        assertNotNull(body);
        assertEquals(DETAIL_MESSAGE, body.getDetail());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), body.getStatus());
    }

    @Test
    void handleResourceConflictException() {
        // given
        ResourceConflictException mockException = mock(ResourceConflictException.class);
        when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        // when
        Response responseEntity = handler.handleResourceConflictException(mockException);
        // then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.CONFLICT.getStatusCode(), responseEntity.getStatus());
        Problem body = (Problem) responseEntity.getEntity();
        assertNotNull(body);
        assertEquals(DETAIL_MESSAGE, body.getDetail());
        assertEquals(Response.Status.CONFLICT.getStatusCode(), body.getStatus());
    }

    @Test
    void handleCustomSignVerificationException() {
        // given
        CustomVerifyException mockException = mock(CustomVerifyException.class);
        when(mockException.getStatus()).thenReturn(Response.Status.BAD_REQUEST.getStatusCode());
        when(mockException.getBody()).thenReturn(DETAIL_MESSAGE);
        // when
        Response responseEntity = handler.handlePropagatedFrontendException(mockException);
        // then
        assertNotNull(responseEntity);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), responseEntity.getStatus());
        assertNotNull(responseEntity.getEntity());
        assertEquals(DETAIL_MESSAGE, responseEntity.getEntity());
        assertEquals("application/json", Objects.requireNonNull(responseEntity.getMediaType()).toString());
    }
}
