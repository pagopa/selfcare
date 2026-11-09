package it.pagopa.selfcare.commons.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.TenantDefinition;
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
        when(registry.resolve("AR")).thenReturn(new TenantDefinition(null, null, java.util.Map.of()));
        JwtService service = new JwtService(pem(ar.getPublic()), registry);
        String token = Jwts.builder()
                .claim("tenant_id", "AR")
                .signWith(ar.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        Claims claims = service.getClaims(token, "AR");

        assertEquals("AR", claims.get("tenant_id"));
        assertThrows(RuntimeException.class, () -> service.getClaims(token, "PNPG"));
    }

    @Test
    void tenantKeysDoNotRequireTheLegacyGlobalVerificationKey() throws Exception {
        KeyPair ar = keyPair();
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.isConfigured()).thenReturn(true);
        when(registry.normalizeAndValidate("AR")).thenReturn("AR");
        when(registry.jwtPublicKey("AR")).thenReturn(java.util.Optional.of(pem(ar.getPublic())));
        when(registry.resolve("AR")).thenReturn(new TenantDefinition(null, null, java.util.Map.of()));
        JwtService service = new JwtService("", registry);
        String token = Jwts.builder()
                .claim("tenant_id", "AR")
                .signWith(ar.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        Claims claims = service.getClaims(token, "AR");

        assertEquals("AR", claims.get("tenant_id"));
    }

    @Test
    void validatesConfiguredIssuerAndAudience() throws Exception {
        KeyPair ar = keyPair();
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.isConfigured()).thenReturn(true);
        when(registry.normalizeAndValidate("AR")).thenReturn("AR");
        when(registry.jwtPublicKey("AR")).thenReturn(java.util.Optional.of(pem(ar.getPublic())));
        when(registry.resolve("AR")).thenReturn(new TenantDefinition(null, null, java.util.Map.of()));
        JwtService service = new JwtService("", registry, "trusted-issuer", "trusted-audience");
        String validToken = Jwts.builder()
                .setIssuer("trusted-issuer")
                .setAudience("trusted-audience")
                .claim("tenant_id", "AR")
                .signWith(ar.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
        String invalidAudienceToken = Jwts.builder()
                .setIssuer("trusted-issuer")
                .setAudience("other-audience")
                .claim("tenant_id", "AR")
                .signWith(ar.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        assertEquals("AR", service.getClaims(validToken, "AR").get("tenant_id"));
        assertThrows(RuntimeException.class, () -> service.getClaims(invalidAudienceToken, "AR"));
    }

    @Test
    void audienceResolutionReconcilesClaimsAndHonorsStrictMode() throws Exception {
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.isConfigured()).thenReturn(true);
        when(registry.supportedTenantIds()).thenReturn(java.util.Set.of("AR"));
        when(registry.normalizeAndValidate("AR")).thenReturn("AR");
        when(registry.resolve("AR")).thenReturn(new TenantDefinition(null,
                new TenantDefinition.JwtDefinition("KEY", java.util.Set.of("SPID"),
                        java.util.Set.of("ar-audience")), java.util.Map.of()));
        JwtService service = new JwtService("", registry);
        Claims claims = Jwts.claims().setAudience("ar-audience");

        assertEquals("AR", service.resolveTenantFromAudience(claims, "AR", false));
        assertThrows(TenantValidationException.class,
                () -> service.resolveTenantFromAudience(claims, "AR", true));
        claims.put("tenant_id", "AR");
        assertEquals("AR", service.resolveTenantFromAudience(claims, "AR", true));
    }

    @Test
    void audienceResolutionRejectsAmbiguousRegistryBindings() throws Exception {
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.isConfigured()).thenReturn(true);
        when(registry.supportedTenantIds()).thenReturn(java.util.Set.of("AR", "PNPG"));
        var definition = new TenantDefinition(null,
                new TenantDefinition.JwtDefinition("KEY", java.util.Set.of("SPID"),
                        java.util.Set.of("shared-audience")), java.util.Map.of());
        when(registry.resolve("AR")).thenReturn(definition);
        when(registry.resolve("PNPG")).thenReturn(definition);
        JwtService service = new JwtService("", registry);

        assertThrows(TenantValidationException.class,
                () -> service.resolveTenantFromAudience(
                        Jwts.claims().setAudience("shared-audience"), "AR", false));
    }

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    void enforcesTenantIssuerAudienceExpirationAndAlgorithm() throws Exception {
        KeyPair ar = keyPair();
        TenantRegistry registry = mock(TenantRegistry.class);
        when(registry.isConfigured()).thenReturn(true);
        when(registry.normalizeAndValidate("AR")).thenReturn("AR");
        when(registry.jwtPublicKey("AR")).thenReturn(java.util.Optional.of(pem(ar.getPublic())));
        when(registry.resolve("AR")).thenReturn(new TenantDefinition(null,
                new TenantDefinition.JwtDefinition("KEY_AR", java.util.Set.of("SPID"),
                        java.util.Set.of("ar-audience")), java.util.Map.of()));
        JwtService service = new JwtService("", registry);
        java.util.Date expiry = java.util.Date.from(java.time.Instant.now().plusSeconds(300));
        String valid = Jwts.builder().setIssuer("SPID").setAudience("ar-audience")
                .setExpiration(expiry).signWith(ar.getPrivate(), SignatureAlgorithm.RS256).compact();
        assertEquals("ar-audience", service.getClaims(valid, "AR").getAudience());
        for (String invalid : java.util.List.of(
                Jwts.builder().setIssuer("other-issuer").setAudience("ar-audience").setExpiration(expiry)
                        .signWith(ar.getPrivate(), SignatureAlgorithm.RS256).compact(),
                Jwts.builder().setAudience("ar-audience").setExpiration(expiry)
                        .signWith(ar.getPrivate(), SignatureAlgorithm.RS256).compact(),
                Jwts.builder().setIssuer("SPID").setExpiration(expiry)
                        .signWith(ar.getPrivate(), SignatureAlgorithm.RS256).compact(),
                Jwts.builder().setIssuer("SPID").setAudience("pnpg-audience").setExpiration(expiry)
                        .signWith(ar.getPrivate(), SignatureAlgorithm.RS256).compact(),
                Jwts.builder().setIssuer("SPID").claim("aud", java.util.List.of("ar-audience"))
                        .setExpiration(expiry).signWith(ar.getPrivate(), SignatureAlgorithm.RS256).compact(),
                Jwts.builder().setIssuer("SPID").setAudience("ar-audience")
                        .signWith(ar.getPrivate(), SignatureAlgorithm.RS256).compact(),
                Jwts.builder().setIssuer("SPID").setAudience("ar-audience").setExpiration(expiry)
                        .signWith(ar.getPrivate(), SignatureAlgorithm.RS512).compact())) {
            assertThrows(io.jsonwebtoken.JwtException.class, () -> service.getClaims(invalid, "AR"));
        }
    }

    private static String pem(PublicKey key) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[] {'\n'})
                        .encodeToString(key.getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }
}
