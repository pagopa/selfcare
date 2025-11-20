package it.pagopa.selfcare.product.integrationTest;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import it.pagopa.selfcare.cucumber.utils.CommonSteps;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;
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
        glue = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.product.integrationTest"},
        plugin = {
                "html:target/cucumber-report/cucumber.html",
                "json:target/cucumber-report/cucumber.json"
        })
public class CucumberSuiteTest extends CucumberQuarkusTest {

    public static void main(String[] args) {
        runMain(CucumberSuiteTest.class, args);
    }


    @BeforeAll
    static void setup() {
        // By default, quarkus starts the ms on port 8081
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;

        log.info("Starting test containers...");

        var composeContainer = new ComposeContainer(new File("./src/test/resources/docker-compose.yml"))
                .withLocalCompose(true)
                .withPull(true)
                .waitingFor("mongodb", Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(5));

        composeContainer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));
        log.info("Test containers started successfully");
        log.info("\nLANGUAGE: {}\nCOUNTRY: {}\nTIMEZONE: {}\n", System.getProperty("user.language"), System.getProperty("user.country"), System.getProperty("user.timezone"));
    }

    @AfterAll
    static void tearDown() {
        log.info("Cucumber tests are finished.");
    }
}
