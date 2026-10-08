package it.pagopa.selfcare.onboarding.integrationTest;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.platform.console.ConsoleLauncher;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

@Slf4j
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.onboarding"},
    plugin = {
      "html:target/cucumber-report/cucumber.html",
      "json:target/cucumber-report/cucumber.json"
    })
@TestProfile(IntegrationProfile.class)
public class CucumberSuiteTest extends CucumberQuarkusTest {

    public static void main(String[] args) {
        ConsoleLauncher.main(new String[] {"execute", "-c", CucumberSuiteTest.class.getName()});
    }

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
            .waitingFor("mongo-db", Wait.forListeningPort())
            .waitingFor("azurite", Wait.forListeningPort())
            .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5));

    composeContainer.start();
    Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));
    verifyIamFixture();

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

  private static void verifyIamFixture() {
    String path = "/iam/users/97a511a7-2acc-47b9-afed-2f3c65753b4a/permissions/Selc:ViewAccountPage";
    RestAssured.given().port(1080).urlEncodingEnabled(false).header("X-Tenant-Id", "AR")
        .queryParam("productId", "prod-io").get(path).then().statusCode(200);
    for (String institutionId : new String[] {"", "unexpected-institution"}) {
      RestAssured.given().port(1080).urlEncodingEnabled(false).header("X-Tenant-Id", "AR")
          .queryParam("productId", "prod-io").queryParam("institutionId", institutionId)
          .get(path).then().statusCode(400);
    }
    RestAssured.given().port(1080).urlEncodingEnabled(false).header("X-Tenant-Id", "PNPG")
        .queryParam("productId", "prod-io").get(path).then().statusCode(404);
    RestAssured.given().port(1080).urlEncodingEnabled(false).header("X-Tenant-Id", "AR")
        .queryParam("productId", "unexpected-product").get(path).then().statusCode(404);
  }
}
