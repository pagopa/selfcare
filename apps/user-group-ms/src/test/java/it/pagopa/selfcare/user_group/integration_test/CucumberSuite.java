package it.pagopa.selfcare.user_group.integration_test;

import io.cucumber.spring.CucumberContextConfiguration;
import it.pagopa.selfcare.user_group.SelfCareUserGroupApplication;
import org.junit.platform.suite.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameters({
    @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty"),
    @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:target/cucumber-report/cucumber.html")
})

@CucumberContextConfiguration
@SpringBootTest(classes = {SelfCareUserGroupApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
public class CucumberSuite {

    private static final ComposeContainer composeContainer;

    static {
        composeContainer = new ComposeContainer(new File("docker-compose.yml"))
            .withLocalCompose(true)
            .waitingFor("mongodb", Wait.forListeningPort());
        composeContainer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));
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

