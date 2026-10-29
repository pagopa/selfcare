package it.pagopa.selfcare.contractscrm.testsupport;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.jwt.build.Jwt;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/** Generates a throwaway RSA key pair and configures the app to trust its public key. */
public class JwtTestResource implements QuarkusTestResourceLifecycleManager {

  public static final String ISSUER = "PAGOPA";
  public static final String AUDIENCE = "selfcare-webhook";

  private static final KeyPair KEY_PAIR = generateKeyPair();

  @Override
  public Map<String, String> start() {
    return Map.of(
        "mp.jwt.verify.publickey",
        Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded()),
        "contracts-crm.machine-token.private-key",
        Base64.getEncoder().encodeToString(KEY_PAIR.getPrivate().getEncoded()));
  }

  @Override
  public void stop() {}

  public static String token(String audience) {
    return Jwt.issuer(ISSUER)
        .audience(audience)
        .subject("webhook-ms")
        .expiresIn(300)
        .sign(KEY_PAIR.getPrivate());
  }

  public static String validToken() {
    return token(AUDIENCE);
  }

  private static KeyPair generateKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
