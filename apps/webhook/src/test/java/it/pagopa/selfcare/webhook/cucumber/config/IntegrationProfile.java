package it.pagopa.selfcare.webhook.cucumber.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    return Map.of();
  }

  public static MongoDatabase getMongoClientConnection() {
    log.info("Starting getMongoClientConnection...");
    ConnectionString connectionString =
        new ConnectionString(
            ConfigProvider.getConfig().getValue("quarkus.mongodb.connection-string", String.class));
    MongoClient mongoClient = MongoClients.create(connectionString);
    return mongoClient.getDatabase("testProduct");
  }
}
