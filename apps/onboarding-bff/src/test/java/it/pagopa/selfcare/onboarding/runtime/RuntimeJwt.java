package it.pagopa.selfcare.onboarding.runtime;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

/** RS256 tokens signed with a key generated for the test run: the application verifies them with {@link #publicKeyPem()}. */
public final class RuntimeJwt {

    public static final String UID = "11111111-1111-4111-8111-111111111111";

    private static final KeyPair TRUSTED_KEYS = generate();
    private static final KeyPair UNTRUSTED_KEYS = generate();

    private RuntimeJwt() {
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String publicKeyPem() {
        String body = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(TRUSTED_KEYS.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----";
    }

    /** Claims of a selfcare token, with no expiration and no tenant. */
    public static JWTCreator.Builder claims(String issuer, String uid) {
        JWTCreator.Builder builder = JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(new Date())
                .withClaim("name", "Jane")
                .withClaim("family_name", "Admin")
                .withClaim("email", "jane.admin@example.test")
                .withClaim("fiscal_number", "AAAAAA00A00A000A");
        return uid == null ? builder : builder.withClaim("uid", uid);
    }

    public static JWTCreator.Builder spid(String uid) {
        return claims("SPID", uid).withExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
    }

    /** Expiring token with no iat claim. */
    public static JWTCreator.Builder spidWithoutIssuedAt(String uid) {
        return JWT.create()
                .withIssuer("SPID")
                .withClaim("uid", uid)
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
    }

    public static JWTCreator.Builder pagopa(String uid) {
        return claims("PAGOPA", uid).withExpiresAt(Date.from(Instant.now().plusSeconds(3600)));
    }

    public static String spidToken(String tenant) {
        JWTCreator.Builder builder = spid(UID);
        return sign(tenant == null ? builder : builder.withClaim("tenant_id", tenant));
    }

    public static String sign(JWTCreator.Builder builder) {
        return builder.sign(algorithm(TRUSTED_KEYS));
    }

    /** Well formed token signed with a key the application does not trust. */
    public static String signWithUntrustedKey(JWTCreator.Builder builder) {
        return builder.sign(algorithm(UNTRUSTED_KEYS));
    }

    public static String signRs384(JWTCreator.Builder builder) {
        return builder.sign(Algorithm.RSA384((RSAPublicKey) TRUSTED_KEYS.getPublic(), (RSAPrivateKey) TRUSTED_KEYS.getPrivate()));
    }

    public static String signRs512(JWTCreator.Builder builder) {
        return builder.sign(Algorithm.RSA512((RSAPublicKey) TRUSTED_KEYS.getPublic(), (RSAPrivateKey) TRUSTED_KEYS.getPrivate()));
    }

    /** RSASSA-PSS with SHA-256 of the trusted key, which java-jwt cannot produce. */
    public static String signPs256(String issuer, String uid) {
        try {
            JwtClaims claims = new JwtClaims();
            claims.setIssuer(issuer);
            claims.setClaim("uid", uid);
            claims.setExpirationTimeMinutesInTheFuture(60);
            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(claims.toJson());
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_PSS_USING_SHA256);
            jws.setKey(TRUSTED_KEYS.getPrivate());
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new IllegalStateException(e);
        }
    }

    /** HS256 token keyed with the bytes of the public key, the classic algorithm confusion attack. */
    public static String signWithThePublicKeyAsSecret(JWTCreator.Builder builder) {
        return builder.sign(Algorithm.HMAC256(publicKeyPem().getBytes()));
    }

    public static String unsecured(JWTCreator.Builder builder) {
        return builder.sign(Algorithm.none());
    }

    private static Algorithm algorithm(KeyPair keys) {
        return Algorithm.RSA256((RSAPublicKey) keys.getPublic(), (RSAPrivateKey) keys.getPrivate());
    }
}
