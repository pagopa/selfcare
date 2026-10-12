package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The comparison of a Quarkus OpenAPI document with the Spring public specification, split in the
 * contract (operations, parameters, bodies, success responses, models) and the declared error
 * responses so the owners see them separately. Shared by the gates on the published and on the
 * served document.
 */
final class OpenApiGate {

  private OpenApiGate() {}

  static String report(String what, List<String> differences) {
    return what
        + ": "
        + differences.size()
        + " difference(s) from the Spring specification\n"
        + differences.stream().map(d -> "  " + d).collect(Collectors.joining("\n"));
  }

  static void assertSameContract(JsonNode actual) {
    List<String> differences = OpenApiInventory.compare(SpringSpec.load(), actual);
    List<String> contract = differences.stream().filter(d -> !OpenApiInventory.isErrorResponseDifference(d)).toList();
    assertTrue(contract.isEmpty(), () -> report("operations, parameters, bodies, success responses, models", contract));
  }

  static void assertSameErrorResponses(JsonNode actual) {
    List<String> differences = OpenApiInventory.compare(SpringSpec.load(), actual);
    List<String> errors = differences.stream().filter(OpenApiInventory::isErrorResponseDifference).toList();
    assertTrue(errors.isEmpty(), () -> report("declared error responses", errors));
  }
}
