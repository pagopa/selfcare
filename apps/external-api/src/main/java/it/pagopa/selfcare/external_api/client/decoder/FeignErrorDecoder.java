package it.pagopa.selfcare.external_api.client.decoder;

import feign.Request;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import it.pagopa.selfcare.external_api.exception.InstitutionDoesNotExistException;
import it.pagopa.selfcare.external_api.exception.InvalidRequestException;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;

import static feign.Util.UTF_8;

@Component
@Slf4j
public class FeignErrorDecoder extends ErrorDecoder.Default {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            throw new ResourceNotFoundException(response.reason());
        } else if (response.status() == 400 && response.request().httpMethod().equals(Request.HttpMethod.HEAD)) {
            throw new InstitutionDoesNotExistException();
        } else if(response.status() == 400) {
            String errorMessage = response.reason();
            if (response.body() != null) {
                try (Reader reader = response.body().asReader(UTF_8)) {
                    errorMessage = Util.toString(reader);
                } catch (IOException e) {
                    log.warn("Failed to read Feign response body", e);
                }
            }
            throw new InvalidRequestException(errorMessage);
        } else {
            return super.decode(methodKey, response);
        }
    }
}
