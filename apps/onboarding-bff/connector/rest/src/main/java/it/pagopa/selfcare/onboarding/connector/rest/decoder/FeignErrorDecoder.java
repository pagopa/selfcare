package it.pagopa.selfcare.onboarding.connector.rest.decoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import it.pagopa.selfcare.onboarding.connector.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class FeignErrorDecoder extends ErrorDecoder.Default {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = null;

        if (response.body() != null) {
            try (InputStream inputStream = response.body().asInputStream()) {
                errorMessage = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                try {
                    JsonNode root = objectMapper.readTree(errorMessage);
                    if (root.has("errors") && root.get("errors").isArray()) {
                        return new CustomVerifyException(response.status(), errorMessage);
                    }
                } catch (JsonProcessingException e) {
                    log.warn("Feign exception response: {}", errorMessage);
                }
            } catch (IOException e) {
                log.warn("Failed to read Feign response body", e);
            }
        }

        if (response.status() == 404)
            throw new ResourceNotFoundException(errorMessage);

        if (response.status() == 400)
            throw new InvalidRequestException(errorMessage);

        if (response.status() == 409)
            throw new ResourceConflictException(errorMessage);

        if (response.status() >= 500 && response.status() < 599) {
            log.error(errorMessage);
            throw new InternalGatewayErrorException(errorMessage);
        }

        return super.decode(methodKey, response);
    }
}
