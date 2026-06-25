package it.pagopa.selfcare.mscore.integration_test;

import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import it.pagopa.selfcare.mscore.SelfCareCoreApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.suite.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameters({
    @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty"),
    @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:target/cucumber-report/cucumber.html"),
    @ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "it.pagopa.selfcare.cucumber.utils,it.pagopa.selfcare.mscore.integration_test")
})
@CucumberContextConfiguration
@SpringBootTest(classes = {SelfCareCoreApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@Slf4j
public class CucumberSuite {

    private static final ComposeContainer composeContainer;

    static {
        log.info("Starting test containers...");

        composeContainer = new ComposeContainer(new File("../docker-compose.yml"))
                .withLocalCompose(true)
                .withPull(true)
                .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*\\n", 1).withStartupTimeout(Duration.ofMinutes(5)))
                .waitingFor("userms", Wait.forLogMessage(".*user-ms.*started in.*Listening on.*", 1).withStartupTimeout(Duration.ofMinutes(5)));
        composeContainer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));

        log.info("Test containers started successfully");
    }

    public CucumberSuite(Environment environment) {
        RestAssured.port = environment.getProperty("rest-assured.port", Integer.class, 8082);
        RestAssured.baseURI = environment.getProperty("rest-assured.base-url", String.class, "http://localhost");
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("key/public-key.pub");
        if (inputStream == null) {
            throw new IOException("Public key file not found in classpath");
        }
        String publicKey = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        registry.add("JWT_TOKEN_PUBLIC_KEY", () -> publicKey);
    }

}
