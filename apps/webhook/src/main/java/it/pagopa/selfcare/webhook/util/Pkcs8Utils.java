package it.pagopa.selfcare.webhook.util;

import io.smallrye.mutiny.Uni;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Pkcs8Utils {

  private Pkcs8Utils() {}

  public static Uni<PrivateKey> extractRSAPrivateKeyFromPem(String privateKeyPem) {
    try {
      return Uni.createFrom().item(parseRSAPrivateKeyFromPem(privateKeyPem));
    } catch (Exception e) {
      log.error("Cannot parse private key on pkcs8 format: {}", e.getMessage());
      return Uni.createFrom().failure(e);
    }
  }

  /**
   * Synchronous variant, for callers that must parse the key outside a reactive pipeline (e.g. a
   * {@code @PostConstruct} initializer). Blocking on the {@link Uni} variant instead is unsafe:
   * bean creation can be triggered from a Vert.x event-loop thread, where Mutiny refuses to block
   * and throws {@code IllegalStateException: The current thread cannot be blocked}.
   */
  public static PrivateKey parseRSAPrivateKeyFromPem(String privateKeyPem)
      throws GeneralSecurityException {
    String privateKeyContent =
        privateKeyPem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

    byte[] decodedKey = Base64.getDecoder().decode(privateKeyContent);

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(keySpec);
  }
}
