package it.pagopa.selfcare.auth.integration_test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AuthIntegrationTestResource implements QuarkusTestResourceLifecycleManager {

  private ComposeContainer composeContainer;

  @Override
  public Map<String, String> start() {
    composeContainer =
        new ComposeContainer(new File("src/test/resources/docker-compose.yml"))
            .withLocalCompose(true)
            .withPull(true)
            .waitingFor(
                "institutionms",
                Wait.forLogMessage(".*Started SelfCareCoreApplication.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(5)))
            .waitingFor(
                "userms",
                Wait.forLogMessage(".*Listening on:.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(5)))
            .waitingFor(
                "iamms",
                Wait.forLogMessage(".*Listening on:.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(5)))
            .withStartupTimeout(Duration.ofMinutes(5));
    composeContainer.start();

    return Map.of("mp.jwt.verify.publickey", readPublicKey());
  }

  @Override
  public void stop() {
    if (composeContainer != null) {
      composeContainer.stop();
    }
  }

  private String readPublicKey() {
    try (InputStream inputStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("key/public-key.pub")) {
      if (inputStream == null) {
        throw new IllegalStateException("Public key file not found in classpath");
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read public key from classpath", e);
    }
  }
}
