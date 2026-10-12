package it.pagopa.selfcare.onboarding.parity;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Operation inventory of an OpenAPI document and the comparison used by the inventory gate.
 *
 * <p>Every operation is flattened into facts (operationId, tags, deprecated, security requirements,
 * each parameter with its location / required / style / explode / schema, request body content
 * types, schemas and encodings, every response status with content types, headers (with their own
 * schema) and schemas). Schemas are fully dereferenced and flattened to leaf facts so that a
 * difference is reported with the exact path of the property that differs.
 *
 * <p>The comparison is closed by construction: a keyword is either compared, or listed in
 * {@link #DOCUMENTATION} (prose that carries no contract), or the document is rejected with an
 * {@link IllegalStateException} naming every construct the comparator does not support (unknown or
 * vendor keywords, external, unresolved or circular {@code $ref}). Nothing is silently skipped.
 *
 * <p>Only differences that carry no contract meaning are normalised:
 *
 * <ul>
 *   <li>the OpenAPI version, info, the top level tag list, summaries, descriptions, titles and
 *       examples (documentation) and the host of {@code servers} (their base path is compared);
 *   <li>the name of a security scheme (its definition is compared) and the order of independent
 *       things (tags, enum values, required names, security alternatives, oneOf / anyOf / allOf
 *       members);
 *   <li>the keywords spelled with their default value (parameter style / explode / required,
 *       {@code readOnly: false}, {@code uniqueItems: false}, {@code additionalProperties: true},
 *       an empty {@code required} list) against their absence;
 *   <li>the OpenAPI 3.0 spelling against the 3.1 one: {@code nullable} versus {@code type: [x,
 *       null]} or a {@code null} alternative, boolean {@code exclusiveMinimum} next to {@code
 *       minimum} versus the numeric form, integer versus decimal spelling of numbers;
 *   <li>a single-member allOf / oneOf / anyOf versus the member itself (the keywords next to it
 *       are kept), a reusable scalar or enum schema versus the same schema inline (named object
 *       models stay named), and the textual pattern SmallRye adds next to {@code format: uuid}.
 * </ul>
 *
 * Paths are compared literally, including a trailing slash, and so are path parameter names.
 */
public final class OpenApiInventory {

  private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "head", "patch", "options", "trace");

  /** Prose and examples: no contract, accepted on every object. */
  private static final Set<String> DOCUMENTATION = Set.of("summary", "description", "example", "examples", "externalDocs", "title", "$comment");

  private static final Set<String> ROOT = Set.of("openapi", "info", "servers", "tags", "paths", "components", "security");
  private static final Set<String> PATH_ITEM = union(HTTP_METHODS, Set.of("parameters"));
  private static final Set<String> OPERATION = Set.of("tags", "operationId", "parameters", "requestBody", "responses", "deprecated", "security");
  private static final Set<String> HEADER = Set.of("required", "deprecated", "allowEmptyValue", "style", "explode", "allowReserved", "schema", "content");
  private static final Set<String> PARAMETER = union(HEADER, Set.of("name", "in"));
  private static final Set<String> REQUEST_BODY = Set.of("content", "required");
  private static final Set<String> MEDIA_TYPE = Set.of("schema", "encoding");
  private static final Set<String> ENCODING = Set.of("contentType", "headers", "style", "explode", "allowReserved");
  private static final Set<String> RESPONSE = Set.of("headers", "content");
  private static final Set<String> SECURITY_SCHEME = Set.of("type", "scheme", "bearerFormat", "in", "name", "flows", "openIdConnectUrl");
  private static final Set<String> OAUTH_FLOW = Set.of("authorizationUrl", "tokenUrl", "refreshUrl", "scopes");
  private static final Set<String> DISCRIMINATOR = Set.of("propertyName", "mapping");
  private static final Set<String> SCHEMA =
      Set.of(
          "type", "nullable", "allOf", "oneOf", "anyOf", "not", "items", "properties", "additionalProperties", "required", "enum", "const",
          "discriminator", "format", "pattern", "minLength", "maxLength", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum",
          "multipleOf", "minItems", "maxItems", "uniqueItems", "minProperties", "maxProperties", "default", "readOnly", "writeOnly",
          "deprecated");

  /** Scalar constraints compared as they are written (after the number spelling normalisation). */
  private static final List<String> SCALAR_CONSTRAINTS =
      List.of("minLength", "maxLength", "minimum", "maximum", "multipleOf", "minItems", "maxItems", "minProperties", "maxProperties");

  /** Flags whose default is false: only a true value is a fact. */
  private static final List<String> FLAGS = List.of("uniqueItems", "readOnly", "writeOnly", "deprecated");

  private static final Pattern SERVER_AUTHORITY = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://[^/]*");
  private static final Pattern ERROR_RESPONSE = Pattern.compile(" \\| \\w+ response (4\\d\\d|5\\d\\d|default)\\b");
  private static final String UUID_PATTERN = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

  private OpenApiInventory() {}

  /**
   * Operation key ("GET /v1/products") to its facts.
   *
   * @throws IllegalStateException when the document uses a construct the comparator does not support
   */
  public static Map<String, Map<String, String>> inventory(JsonNode spec) {
    return new Walker(spec).operations();
  }

  /**
   * Compares {@code actual} against the {@code expected} (Spring) document.
   *
   * @return human readable differences, empty when the inventories are identical
   * @throws IllegalStateException when the expected document has no operation at all, or when a
   *     document uses a construct the comparator does not support
   */
  public static List<String> compare(JsonNode expected, JsonNode actual) {
    Map<String, Map<String, String>> expectedOps = inventory(expected);
    if (expectedOps.isEmpty()) {
      throw new IllegalStateException("the reference specification declares no operation: the gate would be vacuous");
    }
    Map<String, Map<String, String>> actualOps = inventory(actual);
    List<String> differences = new ArrayList<>();

    for (String key : expectedOps.keySet()) {
      if (!actualOps.containsKey(key)) {
        differences.add("MISSING operation " + key);
      }
    }
    for (String key : actualOps.keySet()) {
      if (!expectedOps.containsKey(key)) {
        differences.add("UNEXPECTED operation " + key);
      }
    }
    for (Map.Entry<String, Map<String, String>> entry : expectedOps.entrySet()) {
      Map<String, String> actualFacts = actualOps.get(entry.getKey());
      if (actualFacts == null) {
        continue;
      }
      Set<String> factNames = new TreeSet<>(entry.getValue().keySet());
      factNames.addAll(actualFacts.keySet());
      for (String fact : factNames) {
        String want = entry.getValue().get(fact);
        String got = actualFacts.get(fact);
        if (want == null) {
          differences.add(entry.getKey() + " | UNEXPECTED " + fact + " = " + got);
        } else if (got == null) {
          differences.add(entry.getKey() + " | MISSING " + fact + " (spring: " + want + ")");
        } else if (!want.equals(got)) {
          differences.add(entry.getKey() + " | DIFFERENT " + fact + " (spring: " + want + ", actual: " + got + ")");
        }
      }
    }

    Map<String, String> expectedModels = models(expected);
    Map<String, String> actualModels = models(actual);
    for (String name : expectedModels.keySet()) {
      if (!actualModels.containsKey(name)) {
        differences.add("MISSING model " + name);
      }
    }
    for (String name : actualModels.keySet()) {
      if (!expectedModels.containsKey(name)) {
        differences.add("UNEXPECTED model " + name);
      }
    }

    Set<String> expectedServers = serverBasePaths(expected);
    Set<String> actualServers = serverBasePaths(actual);
    if (!expectedServers.equals(actualServers)) {
      differences.add("DOCUMENT | DIFFERENT servers base path (spring: " + expectedServers + ", actual: " + actualServers + ")");
    }
    return differences;
  }

  /** Differences about the declaration of an error response (4xx, 5xx, default), as opposed to the rest of the contract. */
  public static boolean isErrorResponseDifference(String difference) {
    return ERROR_RESPONSE.matcher(difference).find();
  }

  private static Map<String, String> models(JsonNode spec) {
    Map<String, String> models = new TreeMap<>();
    spec.path("components").path("schemas").fields().forEachRemaining(schema -> {
      if (isObjectModel(schema.getValue())) {
        models.put(schema.getKey(), "");
      }
    });
    return models;
  }

  /**
   * Only object models are named in the contract: a reusable scalar or enum schema is the same
   * contract whether it is referenced or inlined, and its content is compared where it is used.
   */
  private static boolean isObjectModel(JsonNode schema) {
    return schema.has("properties")
        || "object".equals(schema.path("type").asText())
        || schema.has("allOf")
        || schema.has("oneOf")
        || schema.has("anyOf");
  }

  /** The base paths of the declared servers: the host and the port are deployment, the path prefix is routing. */
  private static Set<String> serverBasePaths(JsonNode spec) {
    JsonNode servers = spec.path("servers");
    Set<String> paths = new TreeSet<>();
    if (servers.isArray()) {
      for (JsonNode server : servers) {
        String url = server.path("url").asText("/");
        if (url.contains("{")) {
          throw new IllegalStateException("server variables are not supported by the comparator: " + url);
        }
        String path = SERVER_AUTHORITY.matcher(url).replaceFirst("");
        paths.add(path.replaceAll("/+$", ""));
      }
    }
    if (paths.isEmpty()) {
      paths.add("");
    }
    return paths;
  }

  private static <T> Set<T> union(Set<T> first, Set<T> second) {
    Set<T> all = new LinkedHashSet<>(first);
    all.addAll(second);
    return Set.copyOf(all);
  }

  private static String lastSegment(String ref) {
    return ref.substring(ref.lastIndexOf('/') + 1);
  }

  private static void put(Map<String, String> out, String key, String value) {
    out.merge(key, value, (a, b) -> a.equals(b) ? a : a + " & " + b);
  }

  private static String canonical(JsonNode value) {
    if (value.isNumber()) {
      return new BigDecimal(value.asText()).stripTrailingZeros().toPlainString();
    }
    if (value.isObject()) {
      Map<String, String> fields = new TreeMap<>();
      value.fields().forEachRemaining(f -> fields.put(f.getKey(), canonical(f.getValue())));
      return fields.entrySet().stream().map(f -> TextNode.valueOf(f.getKey()) + ":" + f.getValue()).collect(Collectors.joining(",", "{", "}"));
    }
    if (value.isArray()) {
      List<String> items = new ArrayList<>();
      value.forEach(v -> items.add(canonical(v)));
      return items.stream().collect(Collectors.joining(",", "[", "]"));
    }
    return value.toString();
  }

  private static String canonicalSet(JsonNode array) {
    List<String> values = new ArrayList<>();
    if (array.isArray()) {
      array.forEach(v -> values.add(canonical(v)));
    }
    Collections.sort(values);
    return values.toString();
  }

  private static String sortedTexts(JsonNode array) {
    List<String> values = new ArrayList<>();
    if (array.isArray()) {
      array.forEach(v -> values.add(v.asText()));
    }
    values.sort(String::compareTo);
    return values.toString();
  }

  private static List<JsonNode> concat(JsonNode first, JsonNode second) {
    List<JsonNode> all = new ArrayList<>();
    if (first.isArray()) {
      first.forEach(all::add);
    }
    if (second.isArray()) {
      second.forEach(all::add);
    }
    return all;
  }

  private static String mediaType(String raw) {
    String[] parts = raw.split(";");
    String base = parts[0].trim().toLowerCase(Locale.ROOT);
    List<String> parameters = new ArrayList<>();
    for (int i = 1; i < parts.length; i++) {
      parameters.add(parts[i].replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
    }
    Collections.sort(parameters);
    return parameters.isEmpty() ? base : base + ";" + String.join(";", parameters);
  }

  /** The target of a {@code $ref} chain, the first reference and the last one (the identity of the target). */
  private record Resolved(JsonNode target, String first, String last) {}

  /** One walk over one document, collecting what it cannot judge. */
  private static final class Walker {

    private final JsonNode root;
    private final Set<String> unsupported = new TreeSet<>();
    private String operation = "document";

    Walker(JsonNode root) {
      this.root = root;
    }

    Map<String, Map<String, String>> operations() {
      Map<String, Map<String, String>> operations = new TreeMap<>();
      check(root, "document", ROOT);
      JsonNode paths = root.path("paths");
      for (Iterator<Map.Entry<String, JsonNode>> it = paths.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> pathItem = it.next();
        check(pathItem.getValue(), "path " + pathItem.getKey(), PATH_ITEM);
        JsonNode sharedParameters = pathItem.getValue().path("parameters");
        for (Iterator<Map.Entry<String, JsonNode>> ops = pathItem.getValue().fields(); ops.hasNext(); ) {
          Map.Entry<String, JsonNode> op = ops.next();
          if (!HTTP_METHODS.contains(op.getKey())) {
            continue;
          }
          String key = op.getKey().toUpperCase(Locale.ROOT) + " " + pathItem.getKey();
          operation = key;
          operations.put(key, facts(op.getValue(), sharedParameters));
        }
      }
      if (!unsupported.isEmpty()) {
        throw new IllegalStateException(
            "the OpenAPI document uses constructs the comparator cannot judge; compare them explicitly, never ignore them:\n  "
                + String.join("\n  ", unsupported));
      }
      return operations;
    }

    private void check(JsonNode node, String where, Set<String> handled) {
      if (!node.isObject()) {
        return;
      }
      node.fieldNames()
          .forEachRemaining(
              keyword -> {
                if (!handled.contains(keyword) && !DOCUMENTATION.contains(keyword)) {
                  unsupported.add(where + ": unsupported keyword '" + keyword + "'");
                }
              });
    }

    /** Follows a {@code $ref} chain; null (and a recorded reason) when it cannot be followed. */
    private Resolved resolve(JsonNode node, String where) {
      Set<String> seen = new LinkedHashSet<>();
      JsonNode current = node;
      String first = null;
      String last = null;
      while (current.isObject() && current.has("$ref")) {
        Set<String> siblings = new TreeSet<>();
        current.fieldNames().forEachRemaining(siblings::add);
        siblings.remove("$ref");
        siblings.removeAll(DOCUMENTATION);
        if (!siblings.isEmpty()) {
          unsupported.add(where + ": keywords next to a $ref " + siblings + " (ignored by OpenAPI 3.0, applied by 3.1)");
        }
        String ref = current.get("$ref").asText();
        if (!ref.startsWith("#/")) {
          unsupported.add(where + ": external or relative $ref '" + ref + "'");
          return null;
        }
        if (!seen.add(ref)) {
          unsupported.add(where + ": circular $ref chain " + seen);
          return null;
        }
        JsonNode target;
        try {
          target = root.at(JsonPointer.compile(ref.substring(1)));
        } catch (IllegalArgumentException e) {
          target = MissingNode.getInstance();
        }
        if (target.isMissingNode()) {
          unsupported.add(where + ": unresolved $ref '" + ref + "'");
          return null;
        }
        first = first == null ? ref : first;
        last = ref;
        current = target;
      }
      return new Resolved(current, first, last);
    }

    /** An object of the document that can be a {@code $ref} (parameter, body, response, header, scheme). */
    private JsonNode object(JsonNode node, String where) {
      if (node == null || node.isMissingNode()) {
        return MissingNode.getInstance();
      }
      Resolved resolved = resolve(node, where);
      return resolved == null ? MissingNode.getInstance() : resolved.target();
    }

    private Map<String, String> facts(JsonNode operationNode, JsonNode sharedParameters) {
      Map<String, String> facts = new TreeMap<>();
      check(operationNode, operation, OPERATION);
      facts.put("operationId", operationNode.path("operationId").asText("<none>"));
      facts.put("tags", sortedTexts(operationNode.path("tags")));
      if (operationNode.path("deprecated").asBoolean(false)) {
        facts.put("deprecated", "true");
      }
      facts.put("security", security(operationNode));

      Map<String, JsonNode> parameters = new TreeMap<>();
      for (JsonNode parameter : concat(sharedParameters, operationNode.path("parameters"))) {
        JsonNode resolved = object(parameter, operation + " parameter");
        String in = resolved.path("in").asText();
        String name = resolved.path("name").asText();
        parameters.put(in + " " + ("header".equals(in) ? name.toLowerCase(Locale.ROOT) : name), resolved);
      }
      parameters.forEach(
          (id, parameter) -> {
            check(parameter, operation + " parameter " + id, PARAMETER);
            parameterLike(facts, "param " + id, parameter, parameter.path("in").asText());
          });

      JsonNode body = object(operationNode.path("requestBody"), operation + " requestBody");
      if (!body.isMissingNode()) {
        check(body, operation + " requestBody", REQUEST_BODY);
        facts.put("body required", String.valueOf(body.path("required").asBoolean(false)));
        mediaTypes(facts, "body", body.path("content"), true);
      }

      Iterator<Map.Entry<String, JsonNode>> responses = operationNode.path("responses").fields();
      while (responses.hasNext()) {
        Map.Entry<String, JsonNode> entry = responses.next();
        String prefix = "response " + entry.getKey();
        JsonNode response = object(entry.getValue(), operation + " " + prefix);
        check(response, operation + " " + prefix, RESPONSE);
        facts.put(prefix, "declared");
        headers(facts, prefix, response.path("headers"));
        mediaTypes(facts, prefix, response.path("content"), false);
      }
      return facts;
    }

    private void headers(Map<String, String> facts, String prefix, JsonNode headers) {
      Iterator<Map.Entry<String, JsonNode>> it = headers.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> entry = it.next();
        String headerPrefix = prefix + " header " + entry.getKey().toLowerCase(Locale.ROOT);
        JsonNode header = object(entry.getValue(), operation + " " + headerPrefix);
        check(header, operation + " " + headerPrefix, HEADER);
        facts.put(headerPrefix, "declared");
        parameterLike(facts, headerPrefix, header, "header");
      }
    }

    private void mediaTypes(Map<String, String> facts, String prefix, JsonNode content, boolean encodable) {
      Iterator<Map.Entry<String, JsonNode>> it = content.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> media = it.next();
        String mediaPrefix = prefix + " " + mediaType(media.getKey());
        check(media.getValue(), operation + " " + mediaPrefix, MEDIA_TYPE);
        schema(facts, mediaPrefix, media.getValue().path("schema"), new ArrayDeque<>());
        if (encodable) {
          encodings(facts, mediaPrefix, media.getValue().path("encoding"));
        }
      }
    }

    private void encodings(Map<String, String> facts, String prefix, JsonNode encodings) {
      Iterator<Map.Entry<String, JsonNode>> it = encodings.fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> encoding = it.next();
        String encodingPrefix = prefix + " encoding " + encoding.getKey();
        check(encoding.getValue(), operation + " " + encodingPrefix, ENCODING);
        facts.put(encodingPrefix, "declared");
        for (String keyword : List.of("contentType", "style", "explode", "allowReserved")) {
          if (encoding.getValue().has(keyword)) {
            String value = encoding.getValue().get(keyword).asText();
            facts.put(encodingPrefix + " " + keyword, "contentType".equals(keyword) ? mediaType(value) : value);
          }
        }
        headers(facts, encodingPrefix, encoding.getValue().path("headers"));
      }
    }

    /** Facts shared by the parameters and the response headers. */
    private void parameterLike(Map<String, String> facts, String prefix, JsonNode parameter, String in) {
      boolean required = "path".equals(in) || parameter.path("required").asBoolean(false);
      facts.put(prefix + " required", String.valueOf(required));
      String defaultStyle = "query".equals(in) || "cookie".equals(in) ? "form" : "simple";
      String style = parameter.path("style").asText(defaultStyle);
      facts.put(prefix + " style", style);
      facts.put(prefix + " explode", String.valueOf(parameter.path("explode").asBoolean("form".equals(style))));
      for (String flag : List.of("deprecated", "allowEmptyValue", "allowReserved")) {
        if (parameter.path(flag).asBoolean(false)) {
          facts.put(prefix + " " + flag, "true");
        }
      }
      if (parameter.has("content")) {
        mediaTypes(facts, prefix + " content", parameter.path("content"), false);
      }
      if (parameter.has("schema") || !parameter.has("content")) {
        schema(facts, prefix + " schema", parameter.path("schema"), new ArrayDeque<>());
      }
    }

    /**
     * The security requirements as the alternatives (OR) of the schemes that must be satisfied
     * together (AND), each with its scopes and the definition of the scheme.
     */
    private String security(JsonNode operationNode) {
      JsonNode requirements = operationNode.has("security") ? operationNode.path("security") : root.path("security");
      if (!requirements.isArray() || requirements.isEmpty()) {
        return "none";
      }
      Set<String> alternatives = new TreeSet<>();
      for (JsonNode requirement : requirements) {
        if (!requirement.isObject()) {
          unsupported.add(operation + ": a security requirement that is not an object");
          continue;
        }
        List<String> together = new ArrayList<>();
        requirement
            .fields()
            .forEachRemaining(
                entry -> {
                  String scopes = sortedTexts(entry.getValue());
                  together.add(schemeDefinition(entry.getKey()) + ("[]".equals(scopes) ? "" : scopes));
                });
        Collections.sort(together);
        alternatives.add(together.isEmpty() ? "<anonymous>" : String.join(" & ", together));
      }
      return String.join(" | ", alternatives);
    }

    private String schemeDefinition(String name) {
      JsonNode declared = root.path("components").path("securitySchemes").path(name);
      JsonNode scheme = object(declared, operation + " security scheme " + name);
      if (scheme.isMissingNode()) {
        unsupported.add(operation + ": the security requirement names the undeclared scheme '" + name + "'");
        return "<undeclared>";
      }
      check(scheme, "security scheme " + name, SECURITY_SCHEME);
      String type = scheme.path("type").asText("?");
      return switch (type) {
        case "http" -> type + "/" + scheme.path("scheme").asText("").toLowerCase(Locale.ROOT) + "/" + scheme.path("bearerFormat").asText("");
        case "apiKey" -> type + "/" + scheme.path("in").asText("") + "/" + scheme.path("name").asText("");
        case "openIdConnect" -> type + "/" + scheme.path("openIdConnectUrl").asText("");
        case "oauth2" -> type + "/" + flows(name, scheme.path("flows"));
        default -> type;
      };
    }

    private String flows(String scheme, JsonNode flows) {
      Map<String, String> described = new TreeMap<>();
      flows
          .fields()
          .forEachRemaining(
              flow -> {
                check(flow.getValue(), "security scheme " + scheme + " flow " + flow.getKey(), OAUTH_FLOW);
                List<String> scopes = new ArrayList<>();
                flow.getValue().path("scopes").fieldNames().forEachRemaining(scopes::add);
                Collections.sort(scopes);
                described.put(
                    flow.getKey(),
                    flow.getValue().path("authorizationUrl").asText("")
                        + "|"
                        + flow.getValue().path("tokenUrl").asText("")
                        + "|"
                        + flow.getValue().path("refreshUrl").asText("")
                        + "|"
                        + scopes);
              });
      return described.toString();
    }

    private void schema(Map<String, String> out, String prefix, JsonNode node, Deque<String> visiting) {
      String where = operation + " " + prefix;
      if (node == null || node.isMissingNode() || node.isNull()) {
        put(out, prefix, "<no schema>");
        return;
      }
      if (node.isBoolean()) {
        put(out, prefix + " boolean", node.asText());
        return;
      }
      if (!node.isObject()) {
        unsupported.add(where + ": a schema that is neither an object nor a boolean");
        return;
      }
      if (node.has("$ref")) {
        Resolved resolved = resolve(node, where);
        if (resolved == null) {
          put(out, prefix, "<unresolved $ref>");
          return;
        }
        String name = lastSegment(resolved.first());
        boolean model = isObjectModel(resolved.target());
        if (visiting.contains(resolved.last())) {
          put(out, prefix + (model ? " #model" : " #recursive"), name + " (recursive)");
          return;
        }
        if (model) {
          put(out, prefix + " #model", name);
        }
        visiting.push(resolved.last());
        schema(out, prefix, resolved.target(), visiting);
        visiting.pop();
        return;
      }

      check(node, where, SCHEMA);
      boolean nullable = node.path("nullable").asBoolean(false);
      String type = null;
      JsonNode typeNode = node.path("type");
      if (typeNode.isArray()) {
        List<String> types = new ArrayList<>();
        for (JsonNode t : typeNode) {
          if ("null".equals(t.asText())) {
            nullable = true;
          } else {
            types.add(t.asText());
          }
        }
        Collections.sort(types);
        type = types.isEmpty() ? "null" : String.join("|", types);
      } else if (typeNode.isTextual()) {
        type = typeNode.asText();
      }

      for (String combinator : List.of("allOf", "oneOf", "anyOf")) {
        JsonNode alternatives = node.path(combinator);
        if (!alternatives.isArray()) {
          continue;
        }
        boolean nullableUnion = isNullableUnion(combinator, alternatives, where);
        List<JsonNode> remaining = new ArrayList<>();
        for (JsonNode alternative : alternatives) {
          if (nullableUnion && "null".equals(alternative.path("type").asText())) {
            nullable = true;
          } else {
            remaining.add(alternative);
          }
        }
        if (remaining.size() == 1) {
          schema(out, prefix, remaining.get(0), visiting);
        } else {
          orderIndependent(out, prefix, combinator, remaining, visiting);
        }
      }
      if (node.has("not")) {
        put(out, prefix + " not", "declared");
        schema(out, prefix + " not", node.get("not"), visiting);
      }

      if (type == null && node.has("properties")) {
        type = "object";
      }
      if (type != null) {
        put(out, prefix + " type", type);
      }
      if (nullable) {
        put(out, prefix + " nullable", "true");
      }
      if (node.has("format")) {
        put(out, prefix + " format", node.get("format").asText());
      }
      if (node.has("pattern") && !redundantUuidPattern(node)) {
        put(out, prefix + " pattern", node.get("pattern").asText());
      }
      for (String keyword : SCALAR_CONSTRAINTS) {
        if (node.has(keyword) && !foldedIntoExclusiveBound(node, keyword)) {
          put(out, prefix + " " + keyword, canonical(node.get(keyword)));
        }
      }
      exclusiveBound(out, prefix, node, "exclusiveMinimum", "minimum");
      exclusiveBound(out, prefix, node, "exclusiveMaximum", "maximum");
      for (String flag : FLAGS) {
        if (node.path(flag).asBoolean(false)) {
          put(out, prefix + " " + flag, "true");
        }
      }
      if (node.has("default")) {
        put(out, prefix + " default", canonical(node.get("default")));
      }
      if (node.has("const")) {
        put(out, prefix + " const", canonical(node.get("const")));
      }
      if (node.has("enum")) {
        put(out, prefix + " enum", canonicalSet(node.get("enum")));
      }
      if (node.has("required") && node.get("required").isArray() && !node.get("required").isEmpty()) {
        put(out, prefix + " required", sortedTexts(node.get("required")));
      }
      if (node.has("discriminator")) {
        discriminator(out, prefix, node.get("discriminator"), where);
      }
      if (node.has("items")) {
        schema(out, prefix + " items", node.get("items"), visiting);
      }
      JsonNode additional = node.path("additionalProperties");
      if (additional.isObject()) {
        schema(out, prefix + " additionalProperties", additional, visiting);
      } else if (additional.isBoolean() && !additional.asBoolean()) {
        put(out, prefix + " additionalProperties", "false");
      }
      Iterator<Map.Entry<String, JsonNode>> properties = node.path("properties").fields();
      while (properties.hasNext()) {
        Map.Entry<String, JsonNode> property = properties.next();
        schema(out, prefix + " ." + property.getKey(), property.getValue(), visiting);
      }
    }

    private boolean isNullableUnion(String combinator, JsonNode alternatives, String where) {
      if ("allOf".equals(combinator) || alternatives.size() != 2) {
        return false;
      }
      JsonNode first = alternatives.get(0);
      JsonNode second = alternatives.get(1);
      boolean firstIsNull = first.size() == 1 && "null".equals(first.path("type").asText());
      boolean secondIsNull = second.size() == 1 && "null".equals(second.path("type").asText());
      if (firstIsNull == secondIsNull) {
        return false;
      }
      Resolved resolved = resolve(firstIsNull ? second : first, where);
      if (resolved == null) {
        return false;
      }
      JsonNode member = resolved.target();
      JsonNode type = member.path("type");
      return type.isTextual()
          && !"null".equals(type.asText())
          && !member.path("nullable").asBoolean(false)
          && !member.has("enum")
          && !member.has("const")
          && !member.has("not")
          && !member.has("allOf")
          && !member.has("oneOf")
          && !member.has("anyOf");
    }

    /** The members of an allOf / oneOf / anyOf are a set: they are emitted in the order of their own facts. */
    private void orderIndependent(Map<String, String> out, String prefix, String combinator, List<JsonNode> members, Deque<String> visiting) {
      List<Map<String, String>> flattened = new ArrayList<>();
      for (int i = 0; i < members.size(); i++) {
        Map<String, String> facts = new TreeMap<>();
        schema(facts, "", members.get(i), visiting);
        flattened.add(facts);
      }
      flattened.sort((a, b) -> a.toString().compareTo(b.toString()));
      for (int i = 0; i < flattened.size(); i++) {
        String memberPrefix = prefix + " " + combinator + "[" + i + "]";
        flattened.get(i).forEach((key, value) -> put(out, memberPrefix + key, value));
      }
    }

    private void discriminator(Map<String, String> out, String prefix, JsonNode discriminator, String where) {
      check(discriminator, where + " discriminator", DISCRIMINATOR);
      put(out, prefix + " discriminator", discriminator.path("propertyName").asText());
      discriminator.path("mapping").fields().forEachRemaining(m -> put(out, prefix + " discriminator " + m.getKey(), lastSegment(m.getValue().asText())));
    }

    /** OpenAPI 3.0 spells an exclusive bound next to minimum / maximum with a boolean, 3.1 with the bound itself. */
    private static boolean foldedIntoExclusiveBound(JsonNode node, String keyword) {
      String exclusive = "minimum".equals(keyword) ? "exclusiveMinimum" : "maximum".equals(keyword) ? "exclusiveMaximum" : null;
      return exclusive != null && node.path(exclusive).isBoolean() && node.path(exclusive).asBoolean();
    }

    private static void exclusiveBound(Map<String, String> out, String prefix, JsonNode node, String exclusiveKeyword, String boundKeyword) {
      JsonNode exclusive = node.path(exclusiveKeyword);
      if (exclusive.isNumber()) {
        put(out, prefix + " " + exclusiveKeyword, canonical(exclusive));
      } else if (exclusive.isBoolean() && exclusive.asBoolean() && node.has(boundKeyword)) {
        put(out, prefix + " " + exclusiveKeyword, canonical(node.get(boundKeyword)));
      } else if (exclusive.isBoolean() && exclusive.asBoolean()) {
        put(out, prefix + " " + exclusiveKeyword, "true");
      }
    }

    /** SmallRye adds the textual form of a UUID next to format: uuid, which already says the same. */
    private static boolean redundantUuidPattern(JsonNode node) {
      return "uuid".equals(node.path("format").asText()) && UUID_PATTERN.equals(node.path("pattern").asText());
    }
  }
}
