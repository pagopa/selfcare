package it.pagopa.selfcare.product.integrationTest;

import io.quarkus.test.junit.QuarkusTestProfile;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class IntegrationProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
        "mp.jwt.verify.publickey",
        getPublicKey());
  }

  private String getPublicKey() {
    File file = new File("src/test/resources/certs/pk-key.pub");
    String key = StringUtils.EMPTY;
    try {
      key = new String(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      log.error("Exception reading file", e);
    }
    return key;
  }

}
