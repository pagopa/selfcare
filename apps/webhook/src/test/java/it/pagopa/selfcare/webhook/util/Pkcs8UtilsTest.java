package it.pagopa.selfcare.webhook.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class Pkcs8UtilsTest {

  @Test
  void parseRSAPrivateKeyFromPem_shouldParsePkcs8KeyWithWhitespace() throws Exception {
    KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    String encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded());
    String pem = "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";

    assertThat(Pkcs8Utils.parseRSAPrivateKeyFromPem(pem).getEncoded())
        .isEqualTo(keyPair.getPrivate().getEncoded());
  }

  @Test
  void extractRSAPrivateKeyFromPem_shouldReturnFailedUniForInvalidPem() {
    assertThatThrownBy(
            () -> Pkcs8Utils.extractRSAPrivateKeyFromPem("not-a-private-key").await().indefinitely())
        .isInstanceOf(IllegalArgumentException.class);
  }
}
