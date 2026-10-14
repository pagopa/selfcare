package it.pagopa.selfcare.document.filter;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

/** Runtime-generated RSA keys and signed tokens used to exercise tenant-aware JWT validation. */
public final class TenantJwtTestSupport {

  public static final KeyPair AR_KEYS = generate();
  public static final KeyPair PNPG_KEYS = generate();
  public static final KeyPair FOREIGN_KEYS = generate();

  private TenantJwtTestSupport() {}

  public static String pem(PublicKey publicKey) {
    return "-----BEGIN PUBLIC KEY-----\n"
        + Base64.getMimeEncoder().encodeToString(publicKey.getEncoded())
        + "\n-----END PUBLIC KEY-----";
  }

  /** Token with the given issuer and optional {@code tenant_id} claim, signed by {@code keys}. */
  public static String token(String issuer, String tenantClaim, KeyPair keys) {
    JwtClaimsBuilder claims = Jwt.issuer(issuer).claim("uid", "user-id").expiresIn(300);
    if (tenantClaim != null) {
      claims.claim("tenant_id", tenantClaim);
    }
    return claims.jws().sign(keys.getPrivate());
  }

  private static KeyPair generate() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
