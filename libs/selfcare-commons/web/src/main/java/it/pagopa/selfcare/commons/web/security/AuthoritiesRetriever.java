package it.pagopa.selfcare.commons.web.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@FunctionalInterface
public interface AuthoritiesRetriever {

    Collection<GrantedAuthority> retrieveAuthorities();

}
