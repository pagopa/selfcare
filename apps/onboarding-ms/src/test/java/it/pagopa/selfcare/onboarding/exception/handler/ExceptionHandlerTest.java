package it.pagopa.selfcare.onboarding.exception.handler;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.model.Problem;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static it.pagopa.selfcare.onboarding.util.ErrorMessage.TAX_CODE_NOT_FOUND_IN_SIGNATURE;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ExceptionHandlerTest {

    ExceptionHandler exceptionHandler = new ExceptionHandler();

    @Test
    void toResponse() {
        InvalidRequestException exception = new InvalidRequestException(TAX_CODE_NOT_FOUND_IN_SIGNATURE.getMessage(), TAX_CODE_NOT_FOUND_IN_SIGNATURE.getCode());
        Response response = exceptionHandler.toResponse(exception);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof Problem);

        Problem actual = response.readEntity(Problem.class);
        assertNotNull(actual.getErrors());
        assertFalse(actual.getErrors().isEmpty());
        assertEquals(exception.getMessage(), actual.getErrors().get(0).getDetail());
        assertEquals(exception.getCode(), actual.getErrors().get(0).getCode());
    }
}
