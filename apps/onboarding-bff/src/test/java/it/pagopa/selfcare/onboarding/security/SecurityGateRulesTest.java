package it.pagopa.selfcare.onboarding.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SecurityGateRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html",
            "/swagger-ui", "/swagger-resources", "/swagger-resources/configuration/ui", "/favicon.ico", "/error",
            "/actuator", "/actuator/health", "/dapr/subscribe"})
    void publicPaths(String path) {
        assertTrue(SecurityPaths.isPublic(path), path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/v1/institutions", "/v3/api-docs-private", "/actuators", "/errors", "/swagger-ui.html/x",
            "/v1/swagger-ui/index.html", "/daprs", "/favicon.ico/x"})
    void protectedPaths(String path) {
        assertFalse(SecurityPaths.isPublic(path), path);
    }

    @Test
    void nullPath_isProtected() {
        assertFalse(SecurityPaths.isPublic(null));
    }

    @ParameterizedTest
    @CsvSource(value = {"Bearer abc,true", "'Bearer ',true", "bearer abc,false", "Basic abc,false", "abc,false", "Bearer,false", "'',false"})
    void bearerPrefix_isCaseSensitive(String header, boolean expected) {
        // "Bearer " alone passes the prefix check: the token verification rejects it afterwards
        assertEquals(expected, SecurityProblems.hasBearerToken(header));
    }

    @Test
    void bearerPrefix_missingHeader() {
        assertFalse(SecurityProblems.hasBearerToken(null));
    }

    @Test
    void spidToken_tenantRules() {
        assertTrue(TenantPolicy.isValid(spid(null), "PNPG"));
        assertTrue(TenantPolicy.isValid(spid("AR"), "AR"));
        assertFalse(TenantPolicy.isValid(spid(null), null));
        assertFalse(TenantPolicy.isValid(spid(null), "AR"));
        assertFalse(TenantPolicy.isValid(spid("AR"), "ar"));
        assertFalse(TenantPolicy.isValid(spid("XX"), "XX"));
        assertFalse(TenantPolicy.isValid(spid(7), "PNPG"));
        assertFalse(TenantPolicy.isValid(spid(" "), " "));
    }

    @Test
    void otherIssuers_areNotCheckedAgainstTheTenantHeader() {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("PAGOPA");
        claims.setClaim("tenant_id", "XX");
        DefaultJWTCallerPrincipal pagopa = new DefaultJWTCallerPrincipal(claims);

        assertTrue(TenantPolicy.isValid(pagopa, null));
        assertTrue(TenantPolicy.isValid(pagopa, "anything"));
    }

    @Test
    void headersFactory_forwardsOnlyAuthorizationAndNonBlankTenant() {
        MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
        incoming.add("Authorization", "Bearer token");
        incoming.add("Authorization", "Bearer second");
        incoming.add("X-Tenant-Id", "PNPG");
        incoming.add("X-Tenant-Id", "AR");
        incoming.add("Accept-Language", "it");
        MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();
        outgoing.add("X-Tenant-Id", "stale");

        new AuthenticationPropagationHeadersFactory().update(incoming, outgoing);

        assertEquals(Map.of("Authorization", List.of("Bearer token"), "X-Tenant-Id", List.of("PNPG")), outgoing);
    }

    @Test
    void headersFactory_skipsMissingAndBlankHeaders() {
        MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
        incoming.add("X-Tenant-Id", "  ");

        MultivaluedMap<String, String> outgoing =
                new AuthenticationPropagationHeadersFactory().update(incoming, new MultivaluedHashMap<>());
        assertTrue(outgoing.isEmpty());

        outgoing = new AuthenticationPropagationHeadersFactory().update(null, new MultivaluedHashMap<>());
        assertNull(outgoing.getFirst("Authorization"));
        assertTrue(outgoing.isEmpty());
    }

    @Test
    void functionsHeadersFactory_addsTheFunctionsKey() {
        OnboardingFunctionsHeadersFactory factory = new OnboardingFunctionsHeadersFactory();
        factory.functionsKey = "functions-key";
        MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
        incoming.add("Authorization", "Bearer token");

        MultivaluedMap<String, String> outgoing = factory.update(incoming, new MultivaluedHashMap<>());

        assertEquals("Bearer token", outgoing.getFirst("Authorization"));
        assertEquals("functions-key", outgoing.getFirst("x-functions-key"));
    }

    private static DefaultJWTCallerPrincipal spid(Object tenant) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("SPID");
        if (tenant != null) {
            claims.setClaim("tenant_id", tenant);
        }
        return new DefaultJWTCallerPrincipal(claims);
    }
}
