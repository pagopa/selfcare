package it.pagopa.selfcare.mscore.connector.rest.decoder;

import feign.Response;
import feign.codec.ErrorDecoder;
import it.pagopa.selfcare.mscore.exception.BadGatewayException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.exception.ServiceUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder extends ErrorDecoder.Default {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 503 || response.status() == 504)
            throw new ServiceUnavailableException();
        if(response.status() >= 500 && response.status() <= 509)
            throw new BadGatewayException(response.reason());
        if(response.status() == 404)
            throw new ResourceNotFoundException(response.reason(), "0000");

        return super.decode(methodKey, response);
    }
}
