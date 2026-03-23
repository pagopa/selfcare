package it.pagopa.selfcare.party.registry_proxy.web.handler;

import feign.FeignException;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.BadGatewayException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.InvalidRequestException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.web.exception.ValidationFailedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.*;

class PartyRegistryProxyExceptionHandlerTest {

    private static final String DETAIL_MESSAGE = "detail message";
    private final PartyRegistryProxyExceptionHandler handler = new PartyRegistryProxyExceptionHandler();

    @Test
    void handleResourceNotFoundException() {
        //given
        ResourceNotFoundException mockException = Mockito.mock(ResourceNotFoundException.class);
        Mockito.when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        ResponseEntity<Problem> responseEntity = handler.handleResourceNotFoundException(mockException);
        //then
        assertNotNull(responseEntity);
        assertEquals(NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(NOT_FOUND.value(), responseEntity.getBody().getStatus());
    }

    @Test
    void handleResourceNotFound2Exception() {
        //given
        ResourceNotFoundException mockException = Mockito.mock(ResourceNotFoundException.class);
        Mockito.when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        ResponseEntity<Problem> responseEntity = handler.handleResourceNotFoundException(mockException);
        //then
        assertNotNull(responseEntity);
        assertEquals(NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(NOT_FOUND.value(), responseEntity.getBody().getStatus());
    }

    @Test
    void handleInvalidRequestException() {
        //given
        InvalidRequestException mockException = Mockito.mock(InvalidRequestException.class);
        Mockito.when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        ResponseEntity<Problem> responseEntity = handler.handleInvalidRequestException(mockException);
        //then
        assertNotNull(responseEntity);
        assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(BAD_REQUEST.value(), responseEntity.getBody().getStatus());
    }

    @Test
    void handleBadgatewayException() {
        //given
        BadGatewayException mockException = Mockito.mock(BadGatewayException.class);
        Mockito.when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        ResponseEntity<Problem> responseEntity = handler.handleBadGatewayException(mockException);
        //then
        assertNotNull(responseEntity);
        assertEquals(BAD_GATEWAY, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(BAD_GATEWAY.value(), responseEntity.getBody().getStatus());
    }

    /**
     * Method under test: {@link PartyRegistryProxyExceptionHandler#handleValidationFailedException(ValidationFailedException)}
     */
    @Test
    void testHandleValidationFailedException() {
        PartyRegistryProxyExceptionHandler partyRegistryProxyExceptionHandler = new PartyRegistryProxyExceptionHandler();
        ResponseEntity<Problem> actualHandleValidationFailedExceptionResult = partyRegistryProxyExceptionHandler
                .handleValidationFailedException(new ValidationFailedException("An error occurred"));
        assertTrue(actualHandleValidationFailedExceptionResult.hasBody());
        assertEquals(1, actualHandleValidationFailedExceptionResult.getHeaders().size());
        assertEquals(400, actualHandleValidationFailedExceptionResult.getStatusCodeValue());
        Problem body = actualHandleValidationFailedExceptionResult.getBody();
        assert body != null;
        assertEquals(400, body.getStatus());
        assertEquals("An error occurred", body.getDetail());
        assertEquals("Bad Request", body.getTitle());
    }

    @Test
    void handleResourceFoundException() {
        FeignException exception = Mockito.mock(FeignException.class);
        Mockito.when(exception.status())
                .thenReturn(404);
        ResponseEntity<Problem> responseEntity = handler.handleFeignException(exception);
        assertNotNull(responseEntity);
        assertEquals(NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

    @Test
    void handleResourceFoundException2() {
        FeignException exception = Mockito.mock(FeignException.class);
        Mockito.when(exception.status())
                .thenReturn(500);
        ResponseEntity<Problem> responseEntity = handler.handleFeignException(exception);
        assertNotNull(responseEntity);
        assertEquals(INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
    }

}