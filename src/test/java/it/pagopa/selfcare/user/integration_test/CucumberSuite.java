package it.pagopa.selfcare.user.integration_test;

import io.cucumber.java.Before;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Slf4j
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"it.pagopa.selfcare.cucumber.utils", "it.pagopa.selfcare.user.integration_test"},
        plugin = {
                "html:target/cucumber-report/cucumber.html",
                "json:target/cucumber-report/cucumber.json"
        })
public class CucumberSuite extends CucumberQuarkusTest {

    public static void main(String[] args) {
        runMain(CucumberSuite.class, args);
    }

    @BeforeAll
    static void setup() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("key/public-key.pub")) {
            if (inputStream == null) {
                throw new IOException("Public key file not found in classpath");
            }
            String publicKey = new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
            System.setProperty("JWT-PUBLIC-KEY", publicKey);
        }
    }

    @Before
    public void setupRestAssured() {
        RestAssured.baseURI = ConfigProvider.getConfig().getValue("rest-assured.base-url", String.class);
        RestAssured.port = ConfigProvider.getConfig().getValue("cucumber.http.test-port", Integer.class);
    }

    @AfterAll
    static void tearDown() {
        System.out.println("Cucumber tests are finished.");
        System.exit(0);
    }

}
