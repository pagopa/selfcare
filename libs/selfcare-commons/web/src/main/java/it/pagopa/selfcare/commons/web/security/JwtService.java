package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private final Map<String, PublicKey> tenantKeys = new ConcurrentHashMap<>();

    @Autowired
    public JwtService(
            @Value("${jwt.signingKey}") String jwtSigningKey,
            ObjectProvider<TenantRegistry> tenantRegistryProvider) throws Exception {
        this(jwtSigningKey, tenantRegistryProvider.getIfAvailable());
    }

    public JwtService(@Value("${jwt.signingKey}") String jwtSigningKey) throws Exception {
        this(jwtSigningKey, (TenantRegistry) null);
    }

    JwtService(String jwtSigningKey, TenantRegistry tenantRegistry) throws Exception {
        this.legacyJwtSigningKey = getPublicKey(jwtSigningKey);
        this.tenantRegistry = tenantRegistry;
    }


    public Claims getClaims(String token) {
        return getClaims(token, null);
    }

    public Claims getClaims(String token, String tenantId) {
        log.trace("getClaims start");
        PublicKey verificationKey = resolveVerificationKey(tenantId);
        return Jwts.parser()
                .setSigningKey(verificationKey)
                .parseClaimsJws(token)
                .getBody();
    }

    private PublicKey resolveVerificationKey(String tenantId) {
        if (tenantRegistry == null || !tenantRegistry.isConfigured()) {
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
