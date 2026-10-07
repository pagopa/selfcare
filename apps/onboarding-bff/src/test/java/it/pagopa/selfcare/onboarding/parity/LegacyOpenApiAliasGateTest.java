package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The Spring BFF published its specification at app/src/main/resources/swagger/api-docs.json: the
 * Terraform APIM definitions and the onboarding frontend sync read that path. The Quarkus build
 * generates the document under src/main/docs and the build copies it, unchanged, to the legacy path.
 * The test class is a {@code QuarkusTest} so the generated document is the one of the current build
 * before the comparison.
 */
@QuarkusTest
@TestProfile(ParityTestProfile.class)
class LegacyOpenApiAliasGateTest {

  static final Path CANONICAL = Path.of("src/main/docs/openapi.json");
  static final Path LEGACY = Path.of("app/src/main/resources/swagger/api-docs.json");
  private static final String REGENERATE =
      "run `mvn -f apps/onboarding-bff/pom.xml package -DskipTests` and commit the regenerated files";

  @TestHTTPResource String baseUrl;

  @Test
  void legacyAliasIsByteIdenticalToTheGeneratedDocument() throws Exception {
    assertTrue(Files.isRegularFile(LEGACY), () -> LEGACY + " is missing: " + REGENERATE);

    assertArrayEquals(
        Files.readAllBytes(CANONICAL),
        Files.readAllBytes(LEGACY),
        () -> LEGACY + " differs from " + CANONICAL + ": " + REGENERATE);
  }

  @Test
  void generatedDocumentIsTheOneTheBffServes() throws Exception {
    HttpResponse<String> served =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    assertEquals(200, served.statusCode());

    assertEquals(
        new ObjectMapper().readTree(served.body()),
        new ObjectMapper().readTree(Files.readAllBytes(CANONICAL)),
        () -> CANONICAL + " is not the served document: " + REGENERATE);
  }

  /** Terraform renders the file with templatefile(): any interpolation or directive breaks the plan. */
  @Test
  void legacyAliasIsAValidTemplatefileInput() throws Exception {
    String content = Files.readString(LEGACY, StandardCharsets.UTF_8);

    assertFalse(content.contains("${"), "the document contains a ${...} interpolation");
    assertFalse(content.contains("%{"), "the document contains a %{...} directive");
    JsonNode document = new ObjectMapper().readTree(content);
    assertTrue(document.path("openapi").asText().startsWith("3."));
    assertFalse(document.path("paths").isEmpty());
  }
}
