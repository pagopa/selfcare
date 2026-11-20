package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common helper methods to work with JWT
 */
@Slf4j
@Component
public class JwtService {

    private final PublicKey legacyJwtSigningKey;
    private final TenantRegistry tenantRegistry;
    private final String expectedIssuer;
    private final String expectedAudience;
    private final Map<String, PublicKey> tenantKeys = new ConcurrentHashMap<>();

    @Value("${jwt.tenant-audience-resolution-enabled:false}")
    private boolean tenantAudienceResolutionEnabled;

    @Autowired
    public JwtService(
            @Value("${jwt.signingKey:}") String jwtSigningKey,
            ObjectProvider<TenantRegistry> tenantRegistryProvider,
            @Value("${jwt.issuer:}") String expectedIssuer,
            @Value("${jwt.audience:}") String expectedAudience) throws Exception {
        this(
                jwtSigningKey,
                tenantRegistryProvider.getIfAvailable(),
                expectedIssuer,
                expectedAudience);
    }

    public JwtService(@Value("${jwt.signingKey:}") String jwtSigningKey) throws Exception {
        this(jwtSigningKey, (TenantRegistry) null, "", "");
    }

    JwtService(String jwtSigningKey, TenantRegistry tenantRegistry) throws Exception {
        this(jwtSigningKey, tenantRegistry, "", "");
    }

    JwtService(
            String jwtSigningKey,
            TenantRegistry tenantRegistry,
            String expectedIssuer,
            String expectedAudience) throws Exception {
        this.tenantRegistry = tenantRegistry;
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
        this.legacyJwtSigningKey =
                tenantRegistry == null || !tenantRegistry.isConfigured()
                        ? getPublicKey(jwtSigningKey)
                        : null;
    }


    public Claims getClaims(String token) {
        return getClaims(token, null);
    }

    public Claims getClaims(String token, String tenantId) {
        log.trace("getClaims start");
        PublicKey verificationKey = resolveVerificationKey(tenantId);
        io.jsonwebtoken.JwtParser parser = Jwts.parser().setSigningKey(verificationKey);
        if (StringUtils.hasText(expectedIssuer)) {
            parser.requireIssuer(expectedIssuer);
        }
        if (StringUtils.hasText(expectedAudience)) {
            parser.requireAudience(expectedAudience);
        }
        var parsed = parser.parseClaimsJws(token);
        Claims claims = parsed.getBody();
        if (tenantRegistry != null && tenantRegistry.isConfigured()) {
            var jwt = tenantRegistry.resolve(tenantId).jwt();
            if (jwt != null) {
                if (!jwt.issuers().isEmpty()
                        && (claims.getIssuer() == null || !jwt.issuers().contains(claims.getIssuer()))) {
                    throw new JwtException("Invalid token issuer");
                }
                if (!jwt.audiences().isEmpty()) {
                    boolean matchesAudience = tenantAudienceResolutionEnabled
                            ? !java.util.Collections.disjoint(jwt.audiences(), audiences(claims))
                            : claims.getAudience() != null && jwt.audiences().contains(claims.getAudience());
                    if (!matchesAudience) {
                        throw new JwtException("Invalid token audience");
                    }
                }
                if ((!jwt.issuers().isEmpty() || !jwt.audiences().isEmpty())
                        && (!"RS256".equals(parsed.getHeader().getAlgorithm())
                        || claims.getExpiration() == null)) {
                    throw new JwtException("Invalid token algorithm or expiration");
                }
            }
        }
        return claims;
    }

    public String resolveTenantFromAudience(
            Claims verifiedClaims, String headerTenantId, boolean tenantClaimRequired) {
        if (tenantRegistry == null || !tenantRegistry.isConfigured()) {
            throw new TenantValidationException();
        }
        Set<String> tokenAudiences = audiences(verifiedClaims);
        Set<String> matchingTenants = new HashSet<>();
        for (String tenant : tenantRegistry.supportedTenantIds()) {
            var jwt = tenantRegistry.resolve(tenant).jwt();
            if (jwt != null && !java.util.Collections.disjoint(jwt.audiences(), tokenAudiences)) {
                matchingTenants.add(tenant);
            }
        }
        if (matchingTenants.size() != 1) {
            throw new TenantValidationException();
        }
        String tenant = matchingTenants.iterator().next();
        if (!tenant.equals(tenantRegistry.normalizeAndValidate(headerTenantId))) {
            throw new TenantValidationException();
        }
        Object claim = verifiedClaims.get("tenant_id");
        if (claim == null) {
            if (tenantClaimRequired) {
                throw new TenantValidationException();
            }
        } else if (!(claim instanceof String claimTenant)
                || !StringUtils.hasText(claimTenant)
                || !tenant.equals(tenantRegistry.normalizeAndValidate(claimTenant))) {
            throw new TenantValidationException();
        }
        return tenant;
    }

    private Set<String> audiences(Claims claims) {
        Object audience = claims.get(Claims.AUDIENCE);
        if (audience instanceof String value && StringUtils.hasText(value)) {
            return Set.of(value);
        }
        if (audience instanceof Collection<?> values && !values.isEmpty()) {
            Set<String> result = new HashSet<>();
            for (Object value : values) {
                if (!(value instanceof String text) || !StringUtils.hasText(text)) {
                    throw new JwtException("Invalid token audience");
                }
                result.add(text);
            }
            return result;
        }
        throw new JwtException("Invalid token audience");
    }

    private PublicKey resolveVerificationKey(String tenantId) {
        if (tenantRegistry == null || !tenantRegistry.isConfigured()) {
            if (legacyJwtSigningKey == null) {
                throw new IllegalStateException("Legacy JWT verification key is not configured");
            }
            return legacyJwtSigningKey;
        }
        String normalizedTenant = tenantRegistry.normalizeAndValidate(tenantId);
        return tenantKeys.computeIfAbsent(normalizedTenant, key -> {
            String publicKey = tenantRegistry.jwtPublicKey(key)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing JWT public key for tenant " + key));
            try {
                return getPublicKey(publicKey);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
                throw new IllegalStateException(
                        "Invalid JWT public key for tenant " + key, exception);
            }
        });
    }

    private PublicKey getPublicKey(String signingKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        log.trace("getPublicKey");
        String publicKeyPEM = signingKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PUBLIC KEY-----", "");

        byte[] encoded = Base64.getMimeDecoder().decode(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return keyFactory.generatePublic(keySpec);
    }

}
