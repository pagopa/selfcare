package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The documents of the Quarkus BFF against the Spring public specification with no tolerance: same
 * operations, parameters, schemas and also the same texts, defaults, servers and security. The
 * inventory gates compare what a client depends on; this one is the proof that the whole document,
 * served and published, is the Spring one. Necessary, not sufficient: the behaviour is proven by the
 * parity scenarios.
 */
@QuarkusTest
@TestProfile(ParityTestProfile.class)
class OpenApiIdenticalDocumentGateTest {

  private static final String REGENERATE =
      "run `mvn -f apps/onboarding-bff/pom.xml package -DskipTests` and commit the regenerated files";

  @TestHTTPResource String baseUrl;

  private static void assertIdentical(String what, JsonNode actual) {
    List<String> differences = OpenApiExactDiff.diff(SpringSpec.load(), actual);
    assertTrue(
        differences.isEmpty(),
        () ->
            what
                + " differs from the Spring specification in "
                + differences.size()
                + " place(s)\n"
                + differences.stream().limit(60).map(d -> "  " + d).collect(Collectors.joining("\n")));
  }

  @Test
  void servedDocumentIsIdenticalToTheSpringSpecification() throws Exception {
    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());

    assertIdentical("the served /v3/api-docs", new ObjectMapper().readTree(response.body()));
  }

  @Test
  void publishedDocumentIsIdenticalToTheSpringSpecification() throws Exception {
    assertIdentical(
        "the generated " + LegacyOpenApiAliasGateTest.CANONICAL + " (" + REGENERATE + ")",
        new ObjectMapper().readTree(Files.readAllBytes(LegacyOpenApiAliasGateTest.CANONICAL)));
  }

  @Test
  void legacyAliasIsIdenticalToTheSpringSpecification() throws Exception {
    Path legacy = LegacyOpenApiAliasGateTest.LEGACY;

    assertIdentical(
        "the legacy " + legacy + " (" + REGENERATE + ")", new ObjectMapper().readTree(Files.readAllBytes(legacy)));
  }
}
