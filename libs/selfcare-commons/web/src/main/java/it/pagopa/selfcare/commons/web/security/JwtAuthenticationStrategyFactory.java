package it.pagopa.selfcare.commons.web.security;

/**
 * Abstract Factory of {@link JwtAuthenticationStrategy}
 */
public interface JwtAuthenticationStrategyFactory {

    JwtAuthenticationStrategy create(String jwt);

}
