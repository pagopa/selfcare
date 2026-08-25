package it.pagopa.selfcare.auth.util;

import static io.quarkus.logging.Log.log;

import io.smallrye.mutiny.Uni;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import org.jboss.logging.Logger;

public class Pkcs8Utils {

  private Pkcs8Utils() {}

  public static Uni<PrivateKey> extractRSAPrivateKeyFromPem(String privateKeyPem) {
    try {
      return Uni.createFrom().item(parseRSAPrivateKeyFromPem(privateKeyPem));
    } catch (Exception e) {
      log(
          Logger.Level.ERROR,
          String.format("Cannot parse private key on pkcs8 format: %s", e.getMessage()));
      return Uni.createFrom().failure(e);
    }
  }

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
