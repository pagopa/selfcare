package it.pagopa.selfcare.commons.web.security;

import org.springframework.security.core.AuthenticationException;

/**
 * A Strategy Pattern to manage the Business Logic related to JWT authentication process
 */
public interface JwtAuthenticationStrategy {

    JwtAuthenticationToken authenticate(JwtAuthenticationToken authentication) throws AuthenticationException;

}
