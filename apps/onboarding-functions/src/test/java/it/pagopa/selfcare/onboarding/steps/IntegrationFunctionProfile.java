package it.pagopa.selfcare.onboarding.steps;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import it.pagopa.selfcare.onboarding.utils.JwtData;
import it.pagopa.selfcare.onboarding.utils.JwtUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;

@Slf4j
@QuarkusTest
public class IntegrationFunctionProfile implements QuarkusTestProfile {

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
        "mp.jwt.verify.publickey",
        getPublicKey(),
        "custom.jwt-token-test",
        Objects.requireNonNull(
            JwtUtils.generateToken(
                JwtData.builder()
                    .username("fake-user")
                    .password("fake-password")
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
    jwtPayload.put("family_name", "User");
    jwtPayload.put("fiscal_number", "FAKE-FISCAL-NUMBER");
    jwtPayload.put("name", "Test");
    jwtPayload.put("spid_level", "https://www.spid.gov.it/SpidL2");
    jwtPayload.put("from_aa", "false");
    jwtPayload.put("uid", "fake-onboarding-functions-it-user");
    jwtPayload.put("level", "L2");
    jwtPayload.put("aud", "api.dev.selfcare.pagopa.it");
    jwtPayload.put("iss", "SPID");
    jwtPayload.put("tenant_id", "AR");
    jwtPayload.put("jti", "fake-onboarding-functions-it-jti");
    return jwtPayload;
  }

  public static MongoClient getMongoClientConnection() {
    ConnectionString connectionString =
        new ConnectionString(
            ConfigProvider.getConfig().getValue("quarkus.mongodb.connection-string", String.class));
    return MongoClients.create(connectionString);
  }

  public static MongoDatabase getOnboardingDatabase(MongoClient mongoClient) {
    return mongoClient.getDatabase("selcOnboarding");
  }

  public static MongoDatabase getInstitutionDatabase(MongoClient mongoClient) {
    return mongoClient.getDatabase("selcMsCore");
  }

  public static MongoDatabase getUserDatabase(MongoClient mongoClient) {
    return mongoClient.getDatabase("selcUser");
  }
}
