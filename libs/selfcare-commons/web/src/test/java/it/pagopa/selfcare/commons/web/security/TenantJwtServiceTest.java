package it.pagopa.selfcare.commons.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TenantJwtServiceTest {

    @Test
    void verifiesTokenWithKeySelectedByTenant() throws Exception {
        KeyPair ar = keyPair();
        KeyPair pnpg = keyPair();
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.isConfigured()).thenReturn(true);
        when(registry.normalizeAndValidate("AR")).thenReturn("AR");
        when(registry.normalizeAndValidate("PNPG")).thenReturn("PNPG");
        when(registry.jwtPublicKey("AR")).thenReturn(java.util.Optional.of(pem(ar.getPublic())));
        when(registry.jwtPublicKey("PNPG")).thenReturn(java.util.Optional.of(pem(pnpg.getPublic())));
        JwtService service = new JwtService(pem(ar.getPublic()), registry);
        String token = Jwts.builder()
                .claim("tenant_id", "AR")
                .signWith(ar.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        Claims claims = service.getClaims(token, "AR");

        assertEquals("AR", claims.get("tenant_id"));
        assertThrows(RuntimeException.class, () -> service.getClaims(token, "PNPG"));
    }

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String pem(PublicKey key) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'})
                        .encodeToString(key.getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }
}
