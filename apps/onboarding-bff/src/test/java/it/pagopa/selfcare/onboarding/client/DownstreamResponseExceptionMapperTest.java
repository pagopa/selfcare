package it.pagopa.selfcare.onboarding.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.exception.CustomVerifyException;
import it.pagopa.selfcare.onboarding.exception.DownstreamServiceException;
import it.pagopa.selfcare.onboarding.exception.InternalGatewayErrorException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.client.impl.ClientResponseBuilderImpl;
import org.junit.jupiter.api.Test;

class DownstreamResponseExceptionMapperTest {

    private final DownstreamResponseExceptionMapper mapper = new DownstreamResponseExceptionMapper();

    @Test
    void handlesOnlyErrorStatuses() {
        assertFalse(mapper.handles(200, new MultivaluedHashMap<>()));
        assertFalse(mapper.handles(204, new MultivaluedHashMap<>()));
        assertFalse(mapper.handles(399, new MultivaluedHashMap<>()));
        assertTrue(mapper.handles(400, new MultivaluedHashMap<>()));
        assertTrue(mapper.handles(503, new MultivaluedHashMap<>()));
    }

    @Test
    void notFoundKeepsTheRawBodyAsMessage() {
        RuntimeException exception = mapper.toThrowable(response(404, "No product found with id X"));

        assertInstanceOf(ResourceNotFoundException.class, exception);
        assertEquals("No product found with id X", exception.getMessage());
    }

    @Test
    void badRequestKeepsTheRawBodyAsMessage() {
        RuntimeException exception = mapper.toThrowable(response(400, "bad input"));

        assertInstanceOf(InvalidRequestException.class, exception);
        assertEquals("bad input", exception.getMessage());
    }

    @Test
    void conflictKeepsTheRawBodyAsMessage() {
        RuntimeException exception = mapper.toThrowable(response(409, "already there"));

        assertInstanceOf(ResourceConflictException.class, exception);
        assertEquals("already there", exception.getMessage());
    }

    @Test
    void serverErrorsBecomeGatewayErrors() {
        for (int status : new int[]{500, 502, 503, 598}) {
            assertInstanceOf(InternalGatewayErrorException.class, mapper.toThrowable(response(status, "boom")));
        }
    }

    @Test
    void errorsArrayIsForwardedWithTheDownstreamStatusWhateverIt_Is() {
        String body = "{\"errors\":[{\"code\":\"X\",\"detail\":\"d\"}]}";
        for (int status : new int[]{400, 404, 422, 500}) {
            RuntimeException exception = mapper.toThrowable(response(status, body));

            CustomVerifyException custom = assertInstanceOf(CustomVerifyException.class, exception);
            assertEquals(status, custom.getStatus());
            assertEquals(body, custom.getBody());
        }
    }

    @Test
    void nonArrayErrorsPropertyIsNotAVerifyError() {
        assertInstanceOf(InvalidRequestException.class, mapper.toThrowable(response(400, "{\"errors\":\"nope\"}")));
    }

    @Test
    void otherStatusesBecomeDownstreamServiceExceptionCarryingTheStatus() {
        for (int status : new int[]{401, 403, 405, 422, 429}) {
            RuntimeException exception = mapper.toThrowable(response(status, "ignored"));

            DownstreamServiceException downstream = assertInstanceOf(DownstreamServiceException.class, exception);
            assertEquals(status, downstream.getStatus());
            assertEquals("ignored", downstream.getMessage());
            assertFalse(exception instanceof WebApplicationException);
        }
    }

    @Test
    void bodylessErrorHasNullMessage() {
        Response response = new ClientResponseBuilderImpl().status(404).build();

        RuntimeException exception = mapper.toThrowable(response);

        assertInstanceOf(ResourceNotFoundException.class, exception);
        assertEquals(null, exception.getMessage());
    }

    @Test
    void unreadableBodyPreservesTheDownstreamStatus() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(String.class)).thenThrow(new ProcessingException("cannot read body"));

        ResourceNotFoundException exception =
                assertInstanceOf(ResourceNotFoundException.class, mapper.toThrowable(response));

        assertEquals(null, exception.getMessage());
    }

    @Test
    void unexpectedResponseAccessFailureIsNotHiddenAsAnEmptyBody() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(String.class)).thenThrow(new IllegalStateException("already closed"));

        assertThrows(IllegalStateException.class, () -> mapper.toThrowable(response));
    }

    private static Response response(int status, String body) {
        return new ClientResponseBuilderImpl().status(status).entity(body).build();
    }
}
