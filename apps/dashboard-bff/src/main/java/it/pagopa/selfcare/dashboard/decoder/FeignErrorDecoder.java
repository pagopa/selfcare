package it.pagopa.selfcare.dashboard.decoder;

import feign.Response;
import feign.codec.ErrorDecoder;
import it.pagopa.selfcare.dashboard.exception.BadGatewayException;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder extends ErrorDecoder.Default {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 500) {
            throw new BadGatewayException(response.reason());
        } else if(response.status() == 404) {
            throw new ResourceNotFoundException(response.reason());
        } else {
            return super.decode(methodKey, response);
        }
    }
}
