package it.pagopa.selfcare.commons.web.security;

import org.springframework.security.core.AuthenticationException;

public class TenantValidationException extends AuthenticationException {

    public TenantValidationException() {
        super("Invalid tenant context");
    }

}
