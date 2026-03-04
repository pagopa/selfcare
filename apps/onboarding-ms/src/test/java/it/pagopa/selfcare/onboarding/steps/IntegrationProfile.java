package it.pagopa.selfcare.onboarding.steps;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.junit.QuarkusTestProfile;
import it.pagopa.selfcare.onboarding.util.JwtData;
import it.pagopa.selfcare.onboarding.util.JwtUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;

@Slf4j
@NoArgsConstructor
public class IntegrationProfile implements QuarkusTestProfile {

  @Override
  public String getConfigProfile() {
    return "integrationProfile";
  }

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
        "mp.jwt.verify.publickey",
        getPublicKey(),
        "custom.jwt-token-test",
        Objects.requireNonNull(
            JwtUtils.generateToken(
                JwtData.builder()
                    .username("f.rossi")
                    .password("test")
                    .jwtHeader(buildJwtHeader())
                    .jwtPayload(buildJwtPayload())
                    .build())));
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

  public static Map<String, Object> buildJwtHeader() {
    Map<String, Object> jwtHeader = new HashMap<>();
    jwtHeader.put("alg", "RS256");
    jwtHeader.put("typ", "JWT");
    jwtHeader.put("kid", "jwt_test_kid");
    return jwtHeader;
  }

  public static Map<String, String> buildJwtPayload() {
    Map<String, String> jwtPayload = new HashMap<>();
    jwtPayload.put("family_name", "Rossi");
    jwtPayload.put("fiscal_number", "RSSFNC85M01H501Z");
    jwtPayload.put("name", "Francesco");
    jwtPayload.put("spid_level", "https://www.spid.gov.it/SpidL2");
    jwtPayload.put("from_aa", "false");
    jwtPayload.put("uid", "b6a7c8d2-3b4a-4f7b-8e85-2cba7f6b7bf7");
    jwtPayload.put("level", "L2");
    jwtPayload.put("aud", "api.dev.selfcare.pagopa.it");
    jwtPayload.put("iss", "SPID");
    jwtPayload.put("jti", "_3f603a8bc36b1231b1a7");
    return jwtPayload;
  }

  public static MongoDatabase getMongoClientConnection() {
    ConnectionString connectionString =
        new ConnectionString(
            ConfigProvider.getConfig().getValue("quarkus.mongodb.connection-string", String.class));
    MongoClient mongoClient = MongoClients.create(connectionString);
    return mongoClient.getDatabase("dummyOnboarding");
  }
}
