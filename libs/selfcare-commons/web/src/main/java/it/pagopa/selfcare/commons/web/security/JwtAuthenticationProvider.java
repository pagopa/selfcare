package it.pagopa.selfcare.commons.web.security;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@Slf4j
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtAuthenticationStrategyFactory jwtAuthenticationStrategyFactory;


    public JwtAuthenticationProvider(JwtAuthenticationStrategyFactory jwtAuthenticationStrategyFactory) {
        log.trace("Initializing {}", JwtAuthenticationProvider.class.getSimpleName());
        this.jwtAuthenticationStrategyFactory = jwtAuthenticationStrategyFactory;
    }


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.trace("authenticate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "authenticate authentication = {}", authentication);
        final JwtAuthenticationToken requestAuth = (JwtAuthenticationToken) authentication;
        final JwtAuthenticationStrategy jwtAuthenticationStrategy = jwtAuthenticationStrategyFactory.create(requestAuth.getCredentials());
        final JwtAuthenticationToken jwtAuthenticationToken = jwtAuthenticationStrategy.authenticate(requestAuth);
        jwtAuthenticationToken.setDetails(authentication.getDetails());
        log.trace("authenticate end");
        return jwtAuthenticationToken;
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
