package it.pagopa.selfcare.onboarding.parity;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Starts the unchanged Spring BFF (executable jar built from main) as a separate process against
 * the same controlled downstream. It is the oracle that proves the scenario expectations are the
 * behaviour of the reference, not an assumption about it.
 */
public final class SpringReference implements AutoCloseable {

  public static final String JAR_PROPERTY = "parity.spring.jar";

  private final Process process;
  private final int port;
  private final String scheme;

  SpringReference(Process process, int port) {
    this(process, port, "http");
  }

  private SpringReference(Process process, int port, String scheme) {
    this.process = process;
    this.port = port;
    this.scheme = scheme;
  }

  public static SpringReference start(Path jar, DownstreamStub stub) throws Exception {
    return start(jar, stub, Map.of(), "http", HttpClient.newHttpClient());
  }

  static SpringReference startSecure(Path jar, DownstreamStub stub, Path keyStore, String password,
      HttpClient client) throws Exception {
    return start(jar, stub, Map.of(
        "SERVER_SSL_ENABLED", "true",
        "SERVER_SSL_KEY_STORE", keyStore.toUri().toString(),
        "SERVER_SSL_KEY_STORE_TYPE", "PKCS12",
        "SERVER_SSL_KEY_STORE_PASSWORD", password), "https", client);
  }

  private static SpringReference start(Path jar, DownstreamStub stub, Map<String, String> overrides,
      String scheme, HttpClient client) throws Exception {
    if (!Files.isRegularFile(jar)) {
      throw new IllegalStateException("Spring reference jar not found: " + jar);
    }
    int port = freePort();
    Path logDir = Path.of("target", "parity");
    Files.createDirectories(logDir);
    File log = logDir.resolve("https".equals(scheme) ? "spring-reference-https.log" : "spring-reference.log").toFile();

    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    ProcessBuilder builder =
        new ProcessBuilder(java, "-Xmx512m", "-jar", jar.toAbsolutePath().toString())
            .redirectErrorStream(true)
            .redirectOutput(log);
    Map<String, String> env = builder.environment();
    env.keySet().removeIf(name -> name.startsWith("JAVA_TOOL_OPTIONS"));
    env.putAll(ParityTargets.springEnvironment(stub, port));
    env.putAll(overrides);
    Process process = builder.start();
    SpringReference reference = new SpringReference(process, port, scheme);
    reference.awaitReady(Duration.ofSeconds(180), client);
    return reference;
  }

  public String baseUrl() {
    return scheme + "://127.0.0.1:" + port;
  }

  void awaitReady(Duration timeout) throws InterruptedException {
    awaitReady(timeout, HttpClient.newHttpClient());
  }

  private void awaitReady(Duration timeout, HttpClient client) throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    boolean ready = false;
    try {
      while (System.nanoTime() < deadline) {
        if (!process.isAlive()) {
          throw new IllegalStateException(
              "Spring reference exited with " + process.exitValue() + ", see target/parity/spring-reference.log");
        }
        try {
          HttpResponse<Void> response =
              client.send(
                  HttpRequest.newBuilder(URI.create(baseUrl() + "/actuator/health"))
                      .timeout(Duration.ofSeconds(2))
                      .build(),
                  HttpResponse.BodyHandlers.discarding());
          if (response.statusCode() == 200) {
            ready = true;
            return;
          }
        } catch (IOException notYet) {
          // Connection failures are expected while the reference is starting.
        }
        Thread.sleep(500);
      }
      throw new IllegalStateException("Spring reference did not start within " + timeout);
    } finally {
      if (!ready) {
        close();
      }
    }
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  @Override
  public void close() {
    process.destroy();
    try {
      if (!process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }
}
