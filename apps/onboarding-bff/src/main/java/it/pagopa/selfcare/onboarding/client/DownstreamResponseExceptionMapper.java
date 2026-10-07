package it.pagopa.selfcare.onboarding.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.exception.CustomVerifyException;
import it.pagopa.selfcare.onboarding.exception.DownstreamServiceException;
import it.pagopa.selfcare.onboarding.exception.InternalGatewayErrorException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Translates every downstream error response into the BFF exceptions, once for all REST clients.
 * The downstream status and body drive which exception is raised; any other status keeps the plain
 * {@link DownstreamServiceException}, which the BFF reports with the same status and a generic detail.
 */
@Slf4j
@Provider
public class DownstreamResponseExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean handles(int status, MultivaluedMap<String, Object> headers) {
        return status >= 400;
    }

    @Override
    public RuntimeException toThrowable(Response response) {
        int status = response.getStatus();
        String body = readBody(response);

        if (hasErrorsArray(body)) {
            return new CustomVerifyException(status, body);
        }
        if (status == 404) {
            return new ResourceNotFoundException(body);
        }
        if (status == 400) {
            return new InvalidRequestException(body);
        }
        if (status == 409) {
            return new ResourceConflictException(body);
        }
        if (status >= 500 && status < 599) {
            log.error(body);
            return new InternalGatewayErrorException(body);
        }
        return new DownstreamServiceException(status, body);
    }

    private static String readBody(Response response) {
        if (!response.hasEntity()) {
            return null;
        }
        try {
            return response.readEntity(String.class);
        } catch (ProcessingException e) {
            log.warn("Failed to read downstream error body", e);
            return null;
        }
    }

    private static boolean hasErrorsArray(String body) {
        if (body == null) {
            return false;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            return root != null && root.has("errors") && root.get("errors").isArray();
        } catch (JsonProcessingException e) {
            log.warn("Downstream exception response: {}", body);
            return false;
        }
    }
}
