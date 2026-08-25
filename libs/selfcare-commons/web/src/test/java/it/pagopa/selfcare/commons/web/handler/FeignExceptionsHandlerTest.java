package it.pagopa.selfcare.commons.web.handler;

import feign.FeignException;
import it.pagopa.selfcare.commons.web.model.Problem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FeignExceptionsHandlerTest {

    private final FeignExceptionsHandler handler;

    FeignExceptionsHandlerTest() {
        this.handler = new FeignExceptionsHandler();
    }


    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "INTERNAL_SERVER_ERROR", "OK"})
    void handleFeignException(HttpStatus httpStatus) {
        // given
        FeignException exceptionMock = Mockito.mock(FeignException.class);
        Mockito.when(exceptionMock.status())
                .thenReturn(httpStatus.value());
        // when
        ResponseEntity<Problem> responseEntity = handler.handleFeignException(exceptionMock);
        // then
        assertNotNull(responseEntity);
        assertEquals(httpStatus.is2xxSuccessful() ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("An error occurred during a downstream service request", responseEntity.getBody().getDetail());
    }

}