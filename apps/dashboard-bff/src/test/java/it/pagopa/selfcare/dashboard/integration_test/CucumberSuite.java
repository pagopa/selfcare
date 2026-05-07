package it.pagopa.selfcare.dashboard.integration_test;

import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import it.pagopa.selfcare.dashboard.SelfCareDashboardApplication;
import it.pagopa.selfcare.dashboard.integration_test.steps.DashboardStepsUtil;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Duration;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameters({
    @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty"),
    @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:target/cucumber-report/cucumber.html"),
    @ConfigurationParameter(key = "cucumber.filter.tags", value = "not @skip")
})
@CucumberContextConfiguration
@TestPropertySource(locations = "classpath:application-test.properties")
@SpringBootTest(classes = {SelfCareDashboardApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Slf4j
public class CucumberSuite {

    private DashboardStepsUtil dashboardStepsUtil;

    private static final ComposeContainer composeContainer;

    static {
        log.info("Starting test containers...");

        composeContainer = new ComposeContainer(new File("docker-compose.yml"))
                .withLocalCompose(true)
                .withPull(true)
                .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*\\n", 1).withStartupTimeout(Duration.ofMinutes(5)))
                .waitingFor("onboardingms", Wait.forLogMessage(".*onboarding-ms.*started in.*Listening on.*", 1).withStartupTimeout(Duration.ofMinutes(5)))
                .waitingFor("documentms", Wait.forLogMessage(".*document-ms.*started in.*Listening on.*", 1).withStartupTimeout(Duration.ofMinutes(5)))
                .waitingFor("institutionms", Wait.forLogMessage(".*Started SelfCareCoreApplication.*", 1).withStartupTimeout(Duration.ofMinutes(5)))
                .waitingFor("usergroupms", Wait.forLogMessage(".*Started SelfCareUserGroupApplication.*", 1).withStartupTimeout(Duration.ofMinutes(5)))
                .waitingFor("userms", Wait.forLogMessage(".*user-ms.*started in.*Listening on.*", 1).withStartupTimeout(Duration.ofMinutes(5)))
                .waitingFor("iamms", Wait.forLogMessage(".*iam.*started in.*Listening on.*", 1).withStartupTimeout(Duration.ofMinutes(5)));
        composeContainer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));

        log.info("Test containers started successfully");
    }

    @Before
    public void setUp() {
        dashboardStepsUtil = new DashboardStepsUtil();
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

        InputStream privateKeyStream = classLoader.getResourceAsStream("key/private-key.pem");
        if (privateKeyStream == null) {
            throw new IOException("Private key file not found in classpath");
        }
        String privateKey = new String(privateKeyStream.readAllBytes(), StandardCharsets.UTF_8);
        registry.add("JWT_TOKEN_EXCHANGE_PRIVATE_KEY", () -> privateKey);
    }
}

