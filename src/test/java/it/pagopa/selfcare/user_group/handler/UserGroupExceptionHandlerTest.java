package it.pagopa.selfcare.user_group.handler;

import it.pagopa.selfcare.user_group.exception.ResourceAlreadyExistsException;
import it.pagopa.selfcare.user_group.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.exception.ResourceUpdateException;
import it.pagopa.selfcare.commons.web.model.Problem;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.*;

class UserGroupExceptionHandlerTest {
    private static final String DETAIL_MESSAGE = "detail message";
    private final UserGroupExceptionHandler handler = new UserGroupExceptionHandler();


    @Test
    void resourceNotFoundException() {
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
    void resourceAlreadyExistsException() {
        //given
        ResourceAlreadyExistsException mockException = Mockito.mock(ResourceAlreadyExistsException.class);
        Mockito.when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        ResponseEntity<Problem> responseEntity = handler.handleResourceAlreadyExistsException(mockException);
        //then
        assertNotNull(responseEntity);
        assertEquals(CONFLICT, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(CONFLICT.value(), responseEntity.getBody().getStatus());
    }

    @Test
    void resourceUpdateException() {
        //given
        ResourceUpdateException mockException = Mockito.mock(ResourceUpdateException.class);
        Mockito.when(mockException.getMessage())
                .thenReturn(DETAIL_MESSAGE);
        //when
        ResponseEntity<Problem> responseEntity = handler.handleResourceUpdateException(mockException);
        //then
        assertNotNull(responseEntity);
        assertEquals(BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(DETAIL_MESSAGE, responseEntity.getBody().getDetail());
        assertEquals(BAD_REQUEST.value(), responseEntity.getBody().getStatus());
    }

}