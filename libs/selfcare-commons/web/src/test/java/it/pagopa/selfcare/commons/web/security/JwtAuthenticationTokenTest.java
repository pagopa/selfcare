package it.pagopa.selfcare.commons.web.security;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtAuthenticationTokenTest {

    @Test
    void JwtAuthenticationToken_notAuthenticated() {
        // given
        String token = "token";
        // when
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);
        // then
        Assertions.assertEquals(token, authentication.getCredentials());
        Assertions.assertNull(authentication.getPrincipal());
        Assertions.assertFalse(authentication.isAuthenticated());
        Assertions.assertNotNull(authentication.getAuthorities());
        Assertions.assertTrue(authentication.getAuthorities().isEmpty());
    }


    @Test
    void JwtAuthenticationToken_authenticatedWithNullAuthorities() {
        // given
        String token = "token";
        SelfCareUser user = SelfCareUser.builder("id").build();
        Collection<GrantedAuthority> authorities = null;
        // when
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(token, user, authorities);
        // then
        Assertions.assertEquals(token, authentication.getCredentials());
        Assertions.assertSame(user, authentication.getPrincipal());
        Assertions.assertTrue(authentication.isAuthenticated());
        Assertions.assertEquals(user.getId(), authentication.getName());
        Assertions.assertNotNull(authentication.getAuthorities());
        Assertions.assertTrue(authentication.getAuthorities().isEmpty());
    }


    @Test
    void JwtAuthenticationToken_authenticated() {
        // given
        String token = "token";
        SelfCareUser user = SelfCareUser.builder("id").surname("surname").name("name").email("email").build();
        SimpleGrantedAuthority grantedAuthority = new SimpleGrantedAuthority("role");
        // when
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(token, user, List.of(grantedAuthority));
        // then
        Assertions.assertEquals(token, authentication.getCredentials());
        Assertions.assertSame(user, authentication.getPrincipal());
        Assertions.assertTrue(authentication.isAuthenticated());
        Assertions.assertEquals(user.getId(), authentication.getName());
        Assertions.assertNotNull(authentication.getAuthorities());
        Assertions.assertEquals(1, authentication.getAuthorities().size());
        Assertions.assertEquals(grantedAuthority.getAuthority(), authentication.getAuthorities().iterator().next().getAuthority());
    }


    @Test
    void setAuthenticated_true() {
        // given
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(null);
        // when
        Executable executable = () -> authentication.setAuthenticated(true);
        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead", e.getMessage());
    }


    @Test
    void setAuthenticated_false() {
        // given
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(null);
        // when
        authentication.setAuthenticated(false);
        // then
        Assertions.assertFalse(authentication.isAuthenticated());
    }

}