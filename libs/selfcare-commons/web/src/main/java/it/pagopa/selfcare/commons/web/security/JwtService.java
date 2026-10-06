package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
import java.util.Map;
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
        return parser.parseClaimsJws(token).getBody();
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
