package it.pagopa.selfcare.onboarding.parity;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Operations of the current Spring public specification, the source of truth for the inventory. */
final class Operations {

  private static final Set<String> HTTP_METHODS =
      Set.of("get", "put", "post", "delete", "options", "head", "patch", "trace");

  /** An operation of the contract. {@code concretePath} has sample values for path variables. */
  record Operation(String method, String path, String operationId, JsonNode node) {

    String concretePath() {
      return path.replaceAll("\\{[^}]+}", "sample");
    }

    String label() {
      return method + " " + path;
    }
  }

  private Operations() {}

  static List<Operation> fromSpec(JsonNode spec) {
    List<Operation> operations = new ArrayList<>();
    Iterator<Map.Entry<String, JsonNode>> paths = spec.path("paths").fields();
    while (paths.hasNext()) {
      Map.Entry<String, JsonNode> path = paths.next();
      Iterator<Map.Entry<String, JsonNode>> methods = path.getValue().fields();
      while (methods.hasNext()) {
        Map.Entry<String, JsonNode> method = methods.next();
        if (HTTP_METHODS.contains(method.getKey())) {
          operations.add(
              new Operation(
                  method.getKey().toUpperCase(Locale.ROOT),
                  path.getKey(),
                  method.getValue().path("operationId").asText(null),
                  method.getValue()));
        }
      }
    }
    return operations;
  }

  static List<Operation> springOperations() {
    return fromSpec(SpringSpec.load());
  }
}
