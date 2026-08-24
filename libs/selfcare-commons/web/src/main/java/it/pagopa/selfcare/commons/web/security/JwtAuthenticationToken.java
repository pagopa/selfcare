package it.pagopa.selfcare.commons.web.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;
    private Object principal;


    /**
     * This constructor can be safely used by any code that wishes to create a
     * <code>JwtAuthenticationToken</code>, as the {@link #isAuthenticated()}
     * will return <code>false</code>.
     *
     * @param token The JWT token for this user
     */
    public JwtAuthenticationToken(final String token) {
        super(null);
        setAuthenticated(false);
        this.token = token;
    }


    /**
     * This constructor should only be used by <code>AuthenticationManager</code> or
     * <code>AuthenticationProvider</code> implementations that are satisfied with
     * producing a trusted (i.e. {@link #isAuthenticated()} = <code>true</code>)
     * authentication token.
     *
     * @param token       The JWT token for this user
     * @param user        The authenticated user
     * @param authorities The user authorities
     */
    JwtAuthenticationToken(final String token, final AuthenticatedPrincipal user, Collection<GrantedAuthority> authorities) {
        super(authorities);
        this.token = token;
        principal = user;
        super.setAuthenticated(true); // must use super, as we override
    }


    @Override
    public String getCredentials() {
        return token;
    }


    @Override
    public Object getPrincipal() {
        return principal;
    }


    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        Assert.isTrue(!isAuthenticated,
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        super.setAuthenticated(false);
    }

}
