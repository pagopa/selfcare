package it.pagopa.selfcare.onboarding.parity;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Scenarios generated from the Spring public specification: for every operation and every
 * required query parameter, a request that omits it must be rejected with a 400 problem+json that
 * names the parameter and must not reach any downstream; for every enumerated query parameter, a
 * value outside the enumeration must be rejected the same way.
 */
public final class SpecDrivenScenarios {

  private static final List<String> METHODS = List.of("get", "post", "put", "delete", "head", "patch");

  private SpecDrivenScenarios() {}

  public static List<Scenario> all() {
    JsonNode spec = SpringSpec.load();
    List<Scenario> scenarios = new ArrayList<>();
    Iterator<Map.Entry<String, JsonNode>> paths = spec.path("paths").fields();
    while (paths.hasNext()) {
      Map.Entry<String, JsonNode> pathItem = paths.next();
      for (String method : METHODS) {
        JsonNode operation = pathItem.getValue().path(method);
        if (operation.isMissingNode()) {
          continue;
        }
        List<JsonNode> queryParameters = new ArrayList<>();
        for (JsonNode parameter : operation.path("parameters")) {
          if ("query".equals(parameter.path("in").asText())) {
            queryParameters.add(parameter);
          }
        }
        for (JsonNode parameter : queryParameters) {
          String name = parameter.path("name").asText();
          if (parameter.path("required").asBoolean(false)) {
            scenarios.add(
                build(spec, pathItem.getKey(), method, operation, queryParameters, name, null)
                    .expect(
                        check -> {
                          check.status(400).contentType("application/problem+json").totalCalls(0);
                          if (!isMultipart(operation) && !"head".equals(method)) {
                            check.bodyContains(name);
                          }
                        }));
          }
          JsonNode enumeration = parameter.path("schema").path("enum");
          if (enumeration.isArray() && !enumeration.isEmpty() && !"HEAD".equals(method.toUpperCase(Locale.ROOT))) {
            scenarios.add(
                build(spec, pathItem.getKey(), method, operation, queryParameters, name, "NOT_A_VALID_VALUE")
                    .expect(check -> check.status(400).contentType("application/problem+json").totalCalls(0)));
          }
        }
      }
    }
    return scenarios;
  }

  /** Builds the request with every required parameter valid, except {@code target}: omitted, or set to {@code invalid}. */
  private static Scenario build(
      JsonNode spec,
      String path,
      String method,
      JsonNode operation,
      List<JsonNode> queryParameters,
      String target,
      String invalid) {
    StringBuilder query = new StringBuilder();
    for (JsonNode parameter : queryParameters) {
      String name = parameter.path("name").asText();
      boolean isTarget = name.equals(target);
      if (isTarget && invalid == null) {
        continue;
      }
      if (!isTarget && !parameter.path("required").asBoolean(false)) {
        continue;
      }
      query.append(query.length() == 0 ? '?' : '&').append(name).append('=').append(isTarget ? invalid : validValue(parameter));
    }
    String concrete = path.replaceAll("\\{[^}]+}", "x1") + query;
    String kind = invalid == null ? "missing" : "invalid";
    Scenario scenario =
        Scenario.api("spec-driven", method.toUpperCase(Locale.ROOT) + " " + path + " " + kind + " " + target, method.toUpperCase(Locale.ROOT), concrete);
    JsonNode content = operation.path("requestBody").path("content");
    if (isMultipart(operation)) {
      scenario.multipart(Multipart.body().field("unrelated", "x"));
    } else if (content.has("application/json")) {
      scenario.json("{}");
    }
    return scenario;
  }

  private static boolean isMultipart(JsonNode operation) {
    return operation.path("requestBody").path("content").has("multipart/form-data");
  }

  private static String validValue(JsonNode parameter) {
    JsonNode schema = parameter.path("schema");
    if (schema.path("enum").isArray() && !schema.path("enum").isEmpty()) {
      return schema.path("enum").get(0).asText();
    }
    switch (schema.path("type").asText("string")) {
      case "integer":
      case "number":
        return "1";
      case "boolean":
        return "true";
      default:
        return "x";
    }
  }
}
