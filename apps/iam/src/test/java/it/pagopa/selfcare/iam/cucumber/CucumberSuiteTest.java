package it.pagopa.selfcare.iam.cucumber;

import com.mongodb.client.MongoDatabase;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import it.pagopa.selfcare.iam.cucumber.config.IntegrationProfile;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@Slf4j
@TestProfile(CucumberTestProfile.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {
      "it.pagopa.selfcare.cucumber.utils",
      "it.pagopa.selfcare.iam.cucumber",
      "it.pagopa.selfcare.iam.util"
    },
    plugin = {
      "pretty",
      "html:target/cucumber-report/cucumber.html",
      "json:target/cucumber-report/cucumber.json"
    })
public class CucumberSuiteTest extends CucumberQuarkusTest {

  static MongoDatabase mongoDatabase;
  private static ComposeContainer composeContainer;
  public static String tokenTest;
  private static final String JWT_BEARER_TOKEN_ENV = "custom.jwt-token-test";

  public static void main(String[] args) {
    runMain(CucumberSuiteTest.class, args);
  }

  @BeforeAll
  static void setup() throws IOException {
    // By default, quarkus starts the ms on port 8081
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8081;
    tokenTest = ConfigProvider.getConfig().getValue(JWT_BEARER_TOKEN_ENV, String.class);
    log.info("Starting test containers...");
    composeContainer =
        new ComposeContainer(new File("docker-compose.yml"))
            .withPull(true)
            .waitingFor("mongodb", Wait.forListeningPort())
            // .waitingFor("userms",
            // Wait.forHttp("/q/health/ready").forPort(8087).forStatusCode(200))
            .waitingFor("userms", Wait.forLogMessage(".*Listening on:.*\\n", 1))
            .waitingFor(
                "institutionms",
                Wait.forLogMessage(".*Started SelfCareCoreApplication.*\\n", 1)
                    .withStartupTimeout(Duration.ofMinutes(5)))
            .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(5));

    composeContainer.start();
    Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));
    log.info("Test containers started successfully");
    log.info(
        "\nLANGUAGE: {}\nCOUNTRY: {}\nTIMEZONE: {}\n",
        System.getProperty("user.language"),
        System.getProperty("user.country"),
        System.getProperty("user.timezone"));
  }

  private static void initDb() {
    mongoDatabase = IntegrationProfile.getMongoClientConnection();
  }

  @AfterAll
  static void tearDown() {
    log.info("Cucumber tests are finished.");
  }
}
