package it.pagopa.selfcare.commons.web.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Slf4j
public class NoAuthoritiesRetriever implements AuthoritiesRetriever {

    @Override
    public Collection<GrantedAuthority> retrieveAuthorities() {
        log.trace("retrieveAuthorities start");
        log.debug("retrieved authorities = null");
        log.trace("retrieveAuthorities end");
        return null;
    }

}
