package it.pagopa.selfcare.party.registry_proxy.connector.rest.decoder;

import it.pagopa.selfcare.party.registry_proxy.connector.exception.InternalException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.InvalidRequestException;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

@Configuration
public class RestTemplateErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
        if (status == null) {
            throw new InternalException("Unknown error: " + response.getStatusText());
        }
        switch (status) {
            case BAD_REQUEST -> throw new InvalidRequestException(response.getStatusText());
            case NOT_FOUND -> throw new ResourceNotFoundException(response.getStatusText());
            default -> throw new InternalException(response.getStatusText());
        }
    }
}
