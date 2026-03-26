package it.pagopa.selfcare.onboarding.connector.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomVerifyException extends RuntimeException {
    private final int status;
    private final String body;

    public CustomVerifyException(int status, String body) {
        super(body);
        this.status = status;
        this.body = body;
    }

}
