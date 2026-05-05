package it.pagopa.selfcare.external_api.client.decoder;

import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import it.pagopa.selfcare.external_api.exception.InstitutionDoesNotExistException;
import it.pagopa.selfcare.external_api.exception.InvalidRequestException;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FeignErrorDecoder extends ErrorDecoder.Default {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            throw new ResourceNotFoundException(response.reason());
        } else if (response.status() == 400 && response.request().httpMethod().equals(Request.HttpMethod.HEAD)) {
            throw new InstitutionDoesNotExistException();
        } else if(response.status() == 400) {
            String errorMessage = Optional.ofNullable(response.body()).map(Object::toString).orElse(null);
            throw new InvalidRequestException(errorMessage);
        } else {
            return super.decode(methodKey, response);
        }
    }
}
