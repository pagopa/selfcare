package it.pagopa.selfcare.commons.web.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

class NoAuthoritiesRetrieverTest {

    @Test
    void retrieveAuthorities() {
        // given
        NoAuthoritiesRetriever authoritiesRetriever = new NoAuthoritiesRetriever();
        // when
        Collection<GrantedAuthority> authorities = authoritiesRetriever.retrieveAuthorities();
        // then
        Assertions.assertNull(authorities);
    }

}