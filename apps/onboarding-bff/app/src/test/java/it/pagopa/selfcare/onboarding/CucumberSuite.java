package it.pagopa.selfcare.onboarding;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import io.cucumber.spring.CucumberContextConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "it.pagopa.selfcare.cucumber.utils, it.pagopa.selfcare.onboarding")
@CucumberContextConfiguration
@SpringBootTest(
    classes = {SelfCareOnboardingApplication.class, RestAssuredConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@Slf4j
public class CucumberSuite {

  private static final ComposeContainer composeContainer;

  static {
    log.info("Starting test containers...");

    try {
      URL resource = CucumberSuite.class.getClassLoader().getResource("docker-compose.yml");
      if (resource == null) {
        throw new IllegalStateException("Cannot find docker-compose.yml in resources");
      }

      File composeFile = new File(resource.toURI());
      composeContainer = new ComposeContainer(composeFile)
              .withLocalCompose(true)
              .withTailChildContainers(true)
              .withLogConsumer("azure-cli", new Slf4jLogConsumer(log))
              .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*\\n", 1));

      composeContainer.start();

      Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));

      log.info("Test containers started successfully");
    } catch (URISyntaxException e) {
      throw new RuntimeException("Failed to load or start docker-compose container", e);
    }
  }

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) throws IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    String publicKey;
    try (InputStream inputStream = classLoader.getResourceAsStream("key/public-key.pub")) {
      if (inputStream == null) {
        throw new IOException("Public key file not found in classpath");
      }
      publicKey = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
    registry.add("JWT_TOKEN_PUBLIC_KEY", () -> publicKey);
  }
}
