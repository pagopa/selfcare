package it.pagopa.selfcare.onboarding;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.restassured.RestAssured;
import java.io.File;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

@Slf4j
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.onboarding"},
    plugin = {
      "html:target/cucumber-report/cucumber.html",
      "json:target/cucumber-report/cucumber.json"
    })
public class CucumberSuite extends CucumberQuarkusTest {

  @BeforeAll
  static void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8081;

    log.info("Starting test containers...");

    var composeContainer =
        new ComposeContainer(new File("./src/test/resources/docker-compose.yml"))
            .withPull(true)
            .withTailChildContainers(true)
            .withLogConsumer("azure-cli", new Slf4jLogConsumer(log))
            .waitingFor("mongodb", Wait.forListeningPort())
            .waitingFor("azurite", Wait.forListeningPort())
            .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*", 1))
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
