package it.pagopa.selfcare.auth.util;

import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import static io.quarkus.logging.Log.log;

public class Pkcs8Utils {

  private Pkcs8Utils() {}

  public static Uni<PrivateKey> extractRSAPrivateKeyFromPem(String privateKeyPem) {
    try {
      String privateKeyContent =
          privateKeyPem
              .replace("------BEGIN RSA PRIVATE KEY-----", "")
              .replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END RSA PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replaceAll("\\s", "")
              .replaceAll("\\n", "");

      byte[] decodedKey = Base64.getDecoder().decode(privateKeyContent);

      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return Uni.createFrom().item(kf.generatePrivate(keySpec));
    } catch (Exception e) {
      log(
          Logger.Level.ERROR,
          String.format("Cannot parse private key on pkcs8 format: %s", e.getMessage()));
      return Uni.createFrom().failure(e);
    }
  }
}
