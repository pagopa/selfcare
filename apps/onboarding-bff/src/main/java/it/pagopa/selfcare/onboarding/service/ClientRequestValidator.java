package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies the bean validation constraints declared by the downstream OpenAPI contract to a request before it is
 * sent, as the previous Spring clients did: a violation is a 400 answered by the BFF, without any downstream call.
 * The detail lists {@code <client method>.<parameter>.<property>: <message>}, comma separated.
 */
@ApplicationScoped
public class ClientRequestValidator {

    private final Validator validator;

    public ClientRequestValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> T validated(String clientMethod, String parameter, T request) {
        if (request == null) {
            return null;
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            // Spring reports the violations in an unspecified (hash) order; a stable one is easier to rely on
            throw new InvalidRequestException(violations.stream()
                    .map(violation -> clientMethod + "." + parameter + "." + violation.getPropertyPath()
                            + ": " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
        return request;
    }
}
