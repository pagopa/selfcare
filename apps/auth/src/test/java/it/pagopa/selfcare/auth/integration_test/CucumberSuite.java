package it.pagopa.selfcare.auth.integration_test;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
@TestProfile(IntegrationProfile.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.auth.integration_test"},
    plugin = {
      "html:target/cucumber-report/cucumber.html",
      "json:target/cucumber-report/cucumber.json"
    })
public class CucumberSuite extends CucumberQuarkusTest {

  public static void main(String[] args) {
    runMain(CucumberSuite.class, args);
  }

  @BeforeAll
  static void setup() {
    // By default, quarkus starts the ms on port 8081
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8081;
    RestAssured.requestSpecification =
        new RequestSpecBuilder().addHeader("X-Tenant-Id", "AR").build();

    log.info(
        "\nLANGUAGE: {}\nCOUNTRY: {}\nTIMEZONE: {}\n",
        System.getProperty("user.language"),
        System.getProperty("user.country"),
        System.getProperty("user.timezone"));
  }

  @AfterAll
  static void tearDown() {
    log.info("Cucumber tests are finished.");
  }
}
