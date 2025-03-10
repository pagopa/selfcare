package it.pagopa.selfcare.user.integration_test;

import io.cucumber.java.Before;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@CucumberOptions(
        features = "src/test/resources/features",
        plugin = {
                "html:target/cucumber-report/cucumber.html",
                "json:target/cucumber-report/cucumber.json"
        })
public class CucumberSuite extends CucumberQuarkusTest {
    @BeforeAll
    static void setup() throws IOException {
        Path filePath = Paths.get("src/test/resources/key/public-key.pub");
        String publicKey = Files.readString(filePath);

        System.setProperty("JWT-PUBLIC-KEY", publicKey);

        log.info(System.getProperty("JWT-PUBLIC-KEY"));
    }

    @Before
    public void setupRestAssured() {
        RestAssured.baseURI = ConfigProvider.getConfig().getValue("rest-assured.base-url", String.class);
        RestAssured.port = ConfigProvider.getConfig().getValue("cucumber.http.test-port", Integer.class);

        System.out.println("RestAssured configurato su: " + RestAssured.baseURI + ":" + RestAssured.port);
    }

}
