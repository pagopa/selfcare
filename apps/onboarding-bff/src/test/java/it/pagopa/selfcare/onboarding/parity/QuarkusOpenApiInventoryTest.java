package it.pagopa.selfcare.onboarding.parity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The specification the running BFF actually serves, at the same unauthenticated path Spring uses. */
@QuarkusTest
@TestProfile(ParityTestProfile.class)
class QuarkusOpenApiInventoryTest {

  @TestHTTPResource String baseUrl;

  private JsonNode served() throws Exception {
    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), "/v3/api-docs must be public and answer 200");
    return new ObjectMapper().readTree(response.body());
  }

  @Test
  void servedSpecificationDeclaresTheSameContractAsSpring() throws Exception {
    OpenApiGate.assertSameContract(served());
  }

  @Test
  void servedSpecificationDeclaresTheSameErrorResponsesAsSpring() throws Exception {
    OpenApiGate.assertSameErrorResponses(served());
  }
}
