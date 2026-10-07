package it.pagopa.selfcare.onboarding.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;

class SecurityIdentityUtilsTest {

    @Test
    void getUid_readsTheUidClaimOfTheVerifiedToken_notTheAttributesOrThePrincipalName() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("SPID");
        claims.setSubject("subject");
        claims.setClaim("uid", "user-id");
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new DefaultJWTCallerPrincipal(claims))
                .addAttribute("uid", "attribute-uid")
                .build();

        assertEquals("user-id", SecurityIdentityUtils.getUid(identity));
    }

    @Test
    void getUid_ofATokenWithoutUid_isNullEvenWithAnAttribute() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("SPID");
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new DefaultJWTCallerPrincipal(claims))
                .addAttribute("uid", "attribute-uid")
                .build();

        assertNull(SecurityIdentityUtils.getUid(identity));
    }

    @Test
    void getUid_ofATokenWithJsonNullUid_isNull() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("SPID");
        claims.setClaim("uid", null);
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new DefaultJWTCallerPrincipal(claims))
                .build();

        assertNull(SecurityIdentityUtils.getUid(identity));
    }

    @Test
    void getUid_ofANonStringUid_isNull() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("SPID");
        claims.setClaim("uid", 42);
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new DefaultJWTCallerPrincipal(claims))
                .build();

        assertNull(SecurityIdentityUtils.getUid(identity));
    }

    @Test
    void getUid_ofNonJwtIdentities_usesTheUidAttribute() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("name"))
                .addAttribute("uid", "attribute-uid")
                .build();

        assertEquals("attribute-uid", SecurityIdentityUtils.getUid(identity));
        assertNull(SecurityIdentityUtils.getUid(null));
    }

    @Test
    void getFiscalCode_readsTheFiscalNumberClaim() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("SPID");
        claims.setClaim("fiscal_number", "AAAAAA00A00A000A");
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new DefaultJWTCallerPrincipal(claims))
                .build();

        assertEquals("AAAAAA00A00A000A", SecurityIdentityUtils.getFiscalCode(identity));
    }
}
