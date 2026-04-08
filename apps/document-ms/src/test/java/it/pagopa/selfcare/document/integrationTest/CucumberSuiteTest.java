package it.pagopa.selfcare.document.integrationTest;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

@Slf4j
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.document.integrationTest"},
    plugin = {
      "html:target/cucumber-report/cucumber.html",
      "json:target/cucumber-report/cucumber.json"
    })
@TestProfile(IntegrationProfile.class)
public class CucumberSuiteTest extends CucumberQuarkusTest {

  public static void main(String[] args) {
    runMain(CucumberSuiteTest.class, args);
  }

  @BeforeAll
  static void setup() {
    RestAssured.baseURI = "http://localhost";
    // Quarkus tests run the HTTP server on 8081 by default to avoid conflicts with dev mode
    RestAssured.port = 8081;

    log.info("Starting test containers...");

    var composeContainer =
        new ComposeContainer(new File("./src/test/resources/docker-compose.yml"))
            .withPull(true)
            .waitingFor("mongodb", Wait.forListeningPort())
            .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(5));

    composeContainer.start();
    Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));

    log.info(
        "\nLANGUAGE: {}\nCOUNTRY: {}\nTIMEZONE: {}\n",
        System.getProperty("user.language"),
        System.getProperty("user.country"),
        System.getProperty("user.timezone"));
    log.info("Test containers started successfully");
  }

  @AfterAll
  static void tearDown() {
    log.info("Cucumber tests are finished.");
  }
}
