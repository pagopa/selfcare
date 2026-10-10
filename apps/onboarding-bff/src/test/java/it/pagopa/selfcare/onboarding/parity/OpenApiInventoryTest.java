package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Self-checks of the OpenAPI comparator: it must detect every kind of difference it normalises
 * around, so a green inventory gate is never vacuous, and it must refuse (not skip) what it cannot
 * judge. The gates on the real documents are {@code PublishedOpenApiGateTest} and {@code
 * QuarkusOpenApiInventoryTest}.
 */
class OpenApiInventoryTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void springReferenceDeclaresOperations() {
    Map<String, Map<String, String>> inventory = OpenApiInventory.inventory(SpringSpec.load());

    assertTrue(inventory.size() >= 50, "the reference declares only " + inventory.size() + " operations");
    assertTrue(inventory.containsKey("GET /v1/institutions/from-infocamere/"), "the trailing slash is part of the contract");
  }

  private static ObjectNode spring() {
    return SpringSpec.load().deepCopy();
  }

  @Test
  void identicalSpecificationsHaveNoDifferences() {
    assertTrue(OpenApiInventory.compare(spring(), spring()).isEmpty());
  }

  @Test
  void aSpecificationWithoutOperationsIsRejected() {
    assertThrows(IllegalStateException.class, () -> OpenApiInventory.compare(JSON.createObjectNode(), spring()));
  }

  @Test
  void aMissingOperationIsDetected() {
    ObjectNode actual = spring();
    ((ObjectNode) actual.path("paths").path("/v1/products")).remove("get");

    assertTrue(OpenApiInventory.compare(spring(), actual).contains("MISSING operation GET /v1/products"));
  }

  @Test
  void aTrailingSlashIsADifferentPath() {
    ObjectNode actual = spring();
    ObjectNode paths = (ObjectNode) actual.path("paths");
    paths.set("/v1/institutions/from-infocamere", paths.remove("/v1/institutions/from-infocamere/"));

    List<String> differences = OpenApiInventory.compare(spring(), actual);

    assertTrue(differences.contains("MISSING operation GET /v1/institutions/from-infocamere/"));
    assertTrue(differences.contains("UNEXPECTED operation GET /v1/institutions/from-infocamere"));
  }

  @Test
  void aChangedOperationIdIsDetected() {
    ObjectNode actual = spring();
    ((ObjectNode) actual.path("paths").path("/v1/products").path("get")).put("operationId", "listProducts");

    assertTrue(
        OpenApiInventory.compare(spring(), actual).stream()
            .anyMatch(d -> d.startsWith("GET /v1/products | DIFFERENT operationId")));
  }

  @Test
  void aParameterThatStopsBeingRequiredIsDetected() {
    ObjectNode actual = spring();
    for (JsonNode parameter : actual.path("paths").path("/v2/institutions/onboarding/active").path("get").path("parameters")) {
      if ("productId".equals(parameter.path("name").asText())) {
        ((ObjectNode) parameter).put("required", false);
      }
    }

    assertTrue(
        OpenApiInventory.compare(spring(), actual).stream()
            .anyMatch(d -> d.contains("DIFFERENT param query productId required (spring: true, actual: false)")));
  }

  @Test
  void aChangedPropertyTypeInAReferencedModelIsDetected() {
    ObjectNode actual = spring();
    ObjectNode properties = (ObjectNode) actual.path("components").path("schemas").path("ProductResource").path("properties");
    String property = properties.fieldNames().next();
    ((ObjectNode) properties.path(property)).put("type", "boolean");

    assertTrue(
        OpenApiInventory.compare(spring(), actual).stream()
            .anyMatch(d -> d.startsWith("GET /v1/products | DIFFERENT response 200 application/json items ." + property + " type")));
  }

  @Test
  void aMissingErrorResponseIsClassifiedSeparately() {
    ObjectNode actual = spring();
    ((ObjectNode) actual.path("paths").path("/v1/products").path("get").path("responses")).remove("404");

    List<String> differences = OpenApiInventory.compare(spring(), actual);

    assertTrue(differences.contains("GET /v1/products | MISSING response 404 (spring: declared)"), differences.toString());
    assertTrue(differences.stream().allMatch(OpenApiInventory::isErrorResponseDifference), differences.toString());

    ObjectNode contract = spring();
    ((ObjectNode) contract.path("paths").path("/v1/products").path("get").path("responses")).remove("200");
    assertTrue(
        OpenApiInventory.compare(spring(), contract).stream().noneMatch(OpenApiInventory::isErrorResponseDifference),
        "a missing success response must not be classified as an error response");
  }

  private static JsonNode specWithResponseSchema(String schema, String components) throws IOException {
    return JSON.readTree(
        "{\"paths\":{\"/a\":{\"get\":{\"operationId\":\"a\",\"responses\":{\"200\":{\"content\":{\"application/json\":{\"schema\":"
            + schema
            + "}}}}}}},\"components\":{\"schemas\":"
            + components
            + "}}");
  }

  @Test
  void aReferencedEnumIsTheSameContractAsTheSameEnumInline() throws IOException {
    JsonNode inline = specWithResponseSchema("{\"type\":\"string\",\"enum\":[\"A\",\"B\"]}", "{}");
    JsonNode referenced =
        specWithResponseSchema("{\"$ref\":\"#/components/schemas/Kind\"}", "{\"Kind\":{\"type\":\"string\",\"enum\":[\"B\",\"A\"]}}");

    assertTrue(OpenApiInventory.compare(inline, referenced).isEmpty(), OpenApiInventory.compare(inline, referenced).toString());
  }

  @Test
  void aReferencedEnumWithAnotherValueIsStillDetected() throws IOException {
    JsonNode inline = specWithResponseSchema("{\"type\":\"string\",\"enum\":[\"A\",\"B\"]}", "{}");
    JsonNode referenced =
        specWithResponseSchema("{\"$ref\":\"#/components/schemas/Kind\"}", "{\"Kind\":{\"type\":\"string\",\"enum\":[\"A\",\"C\"]}}");

    assertTrue(OpenApiInventory.compare(inline, referenced).stream().anyMatch(d -> d.contains("DIFFERENT response 200 application/json enum")));
  }

  @Test
  void aRenamedObjectModelIsStillDetected() throws IOException {
    String body = "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}";
    JsonNode expected = specWithResponseSchema("{\"$ref\":\"#/components/schemas/Product\"}", "{\"Product\":" + body + "}");
    JsonNode actual = specWithResponseSchema("{\"$ref\":\"#/components/schemas/ProductResource\"}", "{\"ProductResource\":" + body + "}");

    List<String> differences = OpenApiInventory.compare(expected, actual);

    assertTrue(differences.contains("MISSING model Product"), differences.toString());
    assertTrue(differences.contains("UNEXPECTED model ProductResource"), differences.toString());
  }

  @Test
  void theTextualPatternNextToAUuidFormatIsRedundantButOtherPatternsAreNot() throws IOException {
    String uuidPattern = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    JsonNode expected = specWithResponseSchema("{\"type\":\"string\",\"format\":\"uuid\"}", "{}");
    JsonNode withUuidPattern = specWithResponseSchema("{\"type\":\"string\",\"format\":\"uuid\",\"pattern\":\"" + uuidPattern + "\"}", "{}");
    JsonNode withOtherPattern = specWithResponseSchema("{\"type\":\"string\",\"format\":\"uuid\",\"pattern\":\"^[0-9]+$\"}", "{}");

    assertTrue(OpenApiInventory.compare(expected, withUuidPattern).isEmpty());
    assertTrue(OpenApiInventory.compare(expected, withOtherPattern).stream().anyMatch(d -> d.contains("UNEXPECTED response 200 application/json pattern")));
  }

  @Test
  void oas30NullableAndOas31TypeArraysAreTheSameSchema() throws IOException {
    JsonNode v30 = JSON.readTree("{\"paths\":{\"/a\":{\"get\":{\"operationId\":\"a\",\"responses\":{\"200\":{\"content\":{\"application/json\":{\"schema\":{\"type\":\"string\",\"nullable\":true}}}}}}}}}");
    JsonNode v31 = JSON.readTree("{\"paths\":{\"/a\":{\"get\":{\"operationId\":\"a\",\"responses\":{\"200\":{\"content\":{\"application/json\":{\"schema\":{\"type\":[\"string\",\"null\"]}}}}}}}}}");

    assertTrue(OpenApiInventory.compare(v30, v31).isEmpty());
  }

  // ---- helpers of the keyword level tests: single quotes stand for double quotes ----

  private static JsonNode j(String json) {
    try {
      return JSON.readerFor(JsonNode.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json.replace('\'', '"'));
    } catch (IOException e) {
      throw new IllegalStateException(json, e);
    }
  }

  private static JsonNode doc(String operation, String components) {
    return j("{'paths':{'/a':{'get':" + operation + "}},'components':" + components + "}");
  }

  private static JsonNode docWithSchema(String schema) {
    return doc("{'operationId':'a','responses':{'200':{'description':'ok','content':{'application/json':{'schema':" + schema + "}}}}}", "{}");
  }

  private static JsonNode docWithSchema(String schema, String schemas) {
    return doc("{'operationId':'a','responses':{'200':{'description':'ok','content':{'application/json':{'schema':" + schema + "}}}}}", "{'schemas':" + schemas + "}");
  }

  private static JsonNode docWithHeader(String header) {
    return doc("{'operationId':'a','responses':{'200':{'description':'ok','headers':{'X-Total':" + header + "}}}}", "{}");
  }

  private static JsonNode docWithParameter(String parameter) {
    return doc("{'operationId':'a','parameters':[" + parameter + "],'responses':{}}", "{}");
  }

  private static final String SCHEMES =
      "{'securitySchemes':{'a':{'type':'http','scheme':'bearer','bearerFormat':'JWT'},'b':{'type':'apiKey','in':'header','name':'X-Key'}}}";

  private static JsonNode docWithSecurity(String security) {
    return docWithSecurity(security, SCHEMES);
  }

  private static JsonNode docWithSecurity(String security, String components) {
    return doc("{'operationId':'a','security':" + security + ",'responses':{}}", components);
  }

  private static void assertSame(JsonNode first, JsonNode second) {
    List<String> forward = OpenApiInventory.compare(first, second);
    assertTrue(forward.isEmpty(), "expected no difference, got " + forward);
    List<String> backward = OpenApiInventory.compare(second, first);
    assertTrue(backward.isEmpty(), "expected no difference (reversed), got " + backward);
  }

  /** The two documents differ, in both directions, and the difference names {@code fragment}. */
  private static void assertDetected(JsonNode first, JsonNode second, String fragment) {
    for (List<String> differences : List.of(OpenApiInventory.compare(first, second), OpenApiInventory.compare(second, first))) {
      assertTrue(differences.stream().anyMatch(d -> d.contains(fragment)), "no difference mentions '" + fragment + "': " + differences);
    }
  }

  private static void assertRefused(JsonNode document, String... fragments) {
    IllegalStateException refusal = assertThrows(IllegalStateException.class, () -> OpenApiInventory.inventory(document));
    for (String fragment : fragments) {
      assertTrue(refusal.getMessage().contains(fragment), "the refusal does not mention '" + fragment + "': " + refusal.getMessage());
    }
  }

  // ---- schema keywords ----

  static Stream<Arguments> schemaKeywordsAddedToAnInteger() {
    return Stream.of(
        Arguments.of("readOnly", "'readOnly':true"),
        Arguments.of("writeOnly", "'writeOnly':true"),
        Arguments.of("deprecated", "'deprecated':true"),
        Arguments.of("uniqueItems", "'uniqueItems':true"),
        Arguments.of("minLength", "'minLength':1"),
        Arguments.of("maxLength", "'maxLength':9"),
        Arguments.of("minimum", "'minimum':0"),
        Arguments.of("maximum", "'maximum':9"),
        Arguments.of("exclusiveMinimum", "'exclusiveMinimum':0"),
        Arguments.of("exclusiveMaximum", "'exclusiveMaximum':9"),
        Arguments.of("multipleOf", "'multipleOf':2"),
        Arguments.of("minItems", "'minItems':1"),
        Arguments.of("maxItems", "'maxItems':2"),
        Arguments.of("minProperties", "'minProperties':1"),
        Arguments.of("maxProperties", "'maxProperties':2"),
        Arguments.of("pattern", "'pattern':'^a$'"),
        Arguments.of("format", "'format':'int64'"),
        Arguments.of("default", "'default':1"),
        Arguments.of("const", "'const':1"),
        Arguments.of("enum", "'enum':[1,2]"),
        Arguments.of("nullable", "'nullable':true"),
        Arguments.of("additionalProperties", "'additionalProperties':false"),
        Arguments.of("additionalProperties", "'additionalProperties':{'type':'string'}"),
        Arguments.of("required", "'required':['a']"),
        Arguments.of(" not ", "'not':{'type':'string'}"),
        Arguments.of("discriminator", "'discriminator':{'propertyName':'kind'}"),
        Arguments.of("allOf[", "'allOf':[{'type':'string'},{'type':'integer'}]"),
        Arguments.of("items", "'items':{'type':'string'}"),
        Arguments.of(".child", "'properties':{'child':{'type':'string'}}"));
  }

  @ParameterizedTest(name = "{0}: {1}")
  @MethodSource("schemaKeywordsAddedToAnInteger")
  void everySchemaKeywordIsCompared(String fragment, String keyword) {
    assertDetected(docWithSchema("{'type':'integer'}"), docWithSchema("{'type':'integer'," + keyword + "}"), fragment);
  }

  static Stream<Arguments> schemaKeywordValues() {
    return Stream.of(
        Arguments.of("minLength", "1", "2"),
        Arguments.of("maxLength", "1", "2"),
        Arguments.of("minimum", "1", "2"),
        Arguments.of("maximum", "1", "2"),
        Arguments.of("exclusiveMinimum", "1", "2"),
        Arguments.of("exclusiveMaximum", "1", "2"),
        Arguments.of("multipleOf", "1", "2"),
        Arguments.of("minItems", "1", "2"),
        Arguments.of("maxItems", "1", "2"),
        Arguments.of("minProperties", "1", "2"),
        Arguments.of("maxProperties", "1", "2"),
        Arguments.of("pattern", "'^a$'", "'^b$'"),
        Arguments.of("format", "'int32'", "'int64'"),
        Arguments.of("default", "1", "2"),
        Arguments.of("default", "1", "'1'"),
        Arguments.of("default", "{'a':1}", "{'a':2}"),
        Arguments.of("const", "1", "2"),
        Arguments.of("enum", "[1]", "['1']"),
        Arguments.of("enum", "['A','B']", "['A','C']"));
  }

  @ParameterizedTest(name = "{0}: {1} against {2}")
  @MethodSource("schemaKeywordValues")
  void aDifferentValueOfASchemaKeywordIsDetected(String keyword, String first, String second) {
    assertDetected(
        docWithSchema("{'type':'integer','" + keyword + "':" + first + "}"),
        docWithSchema("{'type':'integer','" + keyword + "':" + second + "}"),
        "DIFFERENT response 200 application/json " + keyword);
  }

  static Stream<Arguments> spellingsOfTheSameSchema() {
    return Stream.of(
        Arguments.of("{'type':'integer'}", "{'type':'integer','readOnly':false}"),
        Arguments.of("{'type':'integer'}", "{'type':'integer','writeOnly':false}"),
        Arguments.of("{'type':'integer'}", "{'type':'integer','deprecated':false}"),
        Arguments.of("{'type':'integer'}", "{'type':'integer','uniqueItems':false}"),
        Arguments.of("{'type':'integer'}", "{'type':'integer','nullable':false}"),
        Arguments.of("{'type':'integer'}", "{'type':'integer','additionalProperties':true}"),
        Arguments.of("{'type':'integer'}", "{'type':'integer','required':[]}"),
        Arguments.of("{'type':'integer'}", "{'type':'integer','title':'T','description':'d','example':1,'examples':[1,2],'externalDocs':{'url':'u'}}"),
        Arguments.of("{'type':'integer','default':1}", "{'type':'integer','default':1.0}"),
        Arguments.of("{'type':'integer','maximum':10}", "{'type':'integer','maximum':10.00}"),
        Arguments.of("{'type':'integer','minimum':1,'exclusiveMinimum':true}", "{'type':'integer','exclusiveMinimum':1}"),
        Arguments.of("{'type':'integer','maximum':9,'exclusiveMaximum':true}", "{'type':'integer','exclusiveMaximum':9}"),
        Arguments.of("{'type':'integer','minimum':1,'exclusiveMinimum':false}", "{'type':'integer','minimum':1}"),
        Arguments.of("{'type':['string','integer']}", "{'type':['integer','string']}"),
        Arguments.of("{'type':'string','enum':['A','B']}", "{'type':'string','enum':['B','A']}"),
        Arguments.of("{'type':'object','required':['a','b']}", "{'type':'object','required':['b','a']}"),
        Arguments.of("{'type':'string','nullable':true}", "{'oneOf':[{'type':'string'},{'type':'null'}]}"),
        Arguments.of("{'type':'string','nullable':true}", "{'anyOf':[{'type':'null'},{'type':'string'}]}"),
        Arguments.of("{'oneOf':[{'type':'string'},{'type':'integer'}]}", "{'oneOf':[{'type':'integer'},{'type':'string'}]}"),
        Arguments.of("{'allOf':[{'type':'string'},{'minLength':1}]}", "{'allOf':[{'minLength':1},{'type':'string'}]}"));
  }

  @ParameterizedTest(name = "{0} is {1}")
  @MethodSource("spellingsOfTheSameSchema")
  void spellingsWithTheSameMeaningAreNotDifferences(String first, String second) {
    assertSame(docWithSchema(first), docWithSchema(second));
  }

  static Stream<Arguments> lookAlikesThatAreDifferentSchemas() {
    return Stream.of(
        Arguments.of("{'type':'integer','minimum':1,'exclusiveMinimum':true}", "{'type':'integer','minimum':1}", "exclusiveMinimum"),
        Arguments.of("{'type':'integer','exclusiveMinimum':1}", "{'type':'integer','minimum':1}", "inimum"),
        Arguments.of("{'type':'integer','minimum':1,'exclusiveMinimum':true}", "{'type':'integer','minimum':2,'exclusiveMinimum':true}", "exclusiveMinimum"),
        Arguments.of("{'type':'integer'}", "{'type':'number'}", "type"),
        Arguments.of("{'type':'string','nullable':true}", "{'type':'string'}", "nullable"),
        Arguments.of("{'type':'string','nullable':true}", "{'allOf':[{'type':'string'},{'type':'null'}]}", "allOf"),
        Arguments.of("{'type':'string','nullable':true}", "{'oneOf':[{'type':'string','nullable':true},{'type':'null'}]}", "oneOf"),
        Arguments.of("{'type':'null'}", "{'oneOf':[{'type':'null'},{'type':'null'}]}", "oneOf"),
        Arguments.of("{'type':'string','nullable':true}", "{'anyOf':[{'type':'string'},{'type':'null','not':{}}]}", "not"),
        Arguments.of("{'type':'string','enum':['a'],'nullable':true}", "{'anyOf':[{'type':'string','enum':['a']},{'type':'null'}]}", "anyOf"),
        Arguments.of("{'oneOf':[{'type':'string'},{'type':'integer'}]}", "{'anyOf':[{'type':'string'},{'type':'integer'}]}", "[0]"),
        Arguments.of("{'oneOf':[{'type':'string'},{'type':'integer'}]}", "{'oneOf':[{'type':'string'},{'type':'boolean'}]}", "boolean"),
        Arguments.of("{'allOf':[{'type':'string'},{'minLength':1}]}", "{'allOf':[{'type':'string'},{'minLength':2}]}", "minLength"));
  }

  @ParameterizedTest(name = "{0} is not {1}")
  @MethodSource("lookAlikesThatAreDifferentSchemas")
  void lookAlikeSchemasThatMeanSomethingElseAreDifferences(String first, String second, String fragment) {
    assertDetected(docWithSchema(first), docWithSchema(second), fragment);
  }

  private static final String ITEM = "{'Item':{'type':'object','properties':{'id':{'type':'string'}}}}";

  @Test
  void aSingleMemberWrapperKeepsTheKeywordsNextToIt() {
    JsonNode plain = docWithSchema("{'$ref':'#/components/schemas/Item'}", ITEM);

    for (String keyword : List.of("'readOnly':true", "'writeOnly':true", "'deprecated':true", "'nullable':true", "'default':{'id':'x'}")) {
      assertDetected(plain, docWithSchema("{'allOf':[{'$ref':'#/components/schemas/Item'}]," + keyword + "}", ITEM), keyword.substring(1, keyword.indexOf('\'', 1)));
    }
    assertSame(plain, docWithSchema("{'allOf':[{'$ref':'#/components/schemas/Item'}],'description':'the item'}", ITEM));
    assertSame(
        docWithSchema("{'allOf':[{'$ref':'#/components/schemas/Item'}],'nullable':true}", ITEM),
        docWithSchema("{'oneOf':[{'$ref':'#/components/schemas/Item'},{'type':'null'}]}", ITEM));
  }

  @Test
  void aKeywordNextToARefIsRefusedButItsDocumentationIsNot() {
    assertSame(docWithSchema("{'$ref':'#/components/schemas/Item'}", ITEM), docWithSchema("{'$ref':'#/components/schemas/Item','description':'the item'}", ITEM));
    assertRefused(docWithSchema("{'$ref':'#/components/schemas/Item','readOnly':true}", ITEM), "keywords next to a $ref", "readOnly");
  }

  // ---- references: cycles, unresolved, external ----

  @Test
  void aRecursiveObjectModelTerminatesAndStillCompares() {
    String node = "{'Node':{'type':'object','properties':{'children':{'type':'array','items':{'$ref':'#/components/schemas/Node'}},'name':{'type':'string'}}}}";
    String changed = "{'Node':{'type':'object','properties':{'children':{'type':'array','items':{'$ref':'#/components/schemas/Node'}},'name':{'type':'integer'}}}}";

    assertTimeoutPreemptively(
        Duration.ofSeconds(10),
        () -> {
          assertSame(docWithSchema("{'$ref':'#/components/schemas/Node'}", node), docWithSchema("{'$ref':'#/components/schemas/Node'}", node));
          assertDetected(docWithSchema("{'$ref':'#/components/schemas/Node'}", node), docWithSchema("{'$ref':'#/components/schemas/Node'}", changed), ".name type");
        });
  }

  @Test
  void aCycleThroughAScalarOrArraySchemaTerminates() {
    String nested = "{'Nested':{'type':'array','items':{'$ref':'#/components/schemas/Nested'}}}";
    String viaAlias = "{'Nested':{'type':'array','items':{'$ref':'#/components/schemas/Alias'}},'Alias':{'$ref':'#/components/schemas/Nested'}}";

    assertTimeoutPreemptively(
        Duration.ofSeconds(10),
        () -> {
          Map<String, Map<String, String>> inventory = OpenApiInventory.inventory(docWithSchema("{'$ref':'#/components/schemas/Nested'}", nested));
          assertTrue(inventory.get("GET /a").toString().contains("(recursive)"), inventory.toString());
          assertFalse(OpenApiInventory.compare(
                  docWithSchema("{'$ref':'#/components/schemas/Nested'}", nested), docWithSchema("{'type':'array','items':{'type':'string'}}")).isEmpty());
          assertTrue(OpenApiInventory.inventory(docWithSchema("{'$ref':'#/components/schemas/Nested'}", viaAlias)).get("GET /a").toString().contains("(recursive)"));
        });
  }

  @Test
  void aCircularChainOfReferencesWithoutASchemaIsRefused() {
    String circular = "{'A':{'$ref':'#/components/schemas/B'},'B':{'$ref':'#/components/schemas/A'}}";

    assertTimeoutPreemptively(
        Duration.ofSeconds(10), () -> assertRefused(docWithSchema("{'$ref':'#/components/schemas/A'}", circular), "circular $ref chain", "#/components/schemas/A"));
  }

  @Test
  void anUnresolvedReferenceIsRefusedNotIgnored() {
    assertRefused(docWithSchema("{'$ref':'#/components/schemas/Missing'}"), "unresolved $ref", "Missing");
    assertRefused(doc("{'operationId':'a','parameters':[{'$ref':'#/components/parameters/Missing'}],'responses':{}}", "{}"), "unresolved $ref", "Missing");
    assertRefused(doc("{'operationId':'a','responses':{'200':{'$ref':'#/components/responses/Missing'}}}", "{}"), "unresolved $ref", "Missing");
  }

  @Test
  void anExternalReferenceIsRefused() {
    assertRefused(docWithSchema("{'$ref':'other.json#/components/schemas/Item'}"), "external or relative $ref", "other.json");
    assertRefused(docWithSchema("{'$ref':'https://example.org/schemas.json'}"), "external or relative $ref");
  }

  @Test
  void referencedParametersResponsesHeadersAndBodiesAreFollowed() {
    String components =
        "{'parameters':{'Q':{'name':'q','in':'query','required':true,'schema':{'type':'string'}}},"
            + "'headers':{'Total':{'schema':{'type':'integer'}}},"
            + "'responses':{'Ok':{'description':'ok','headers':{'X-Total':{'$ref':'#/components/headers/Total'}}}},"
            + "'requestBodies':{'In':{'required':true,'content':{'application/json':{'schema':{'type':'string'}}}}}}";
    JsonNode referenced = doc("{'operationId':'a','parameters':[{'$ref':'#/components/parameters/Q'}],'requestBody':{'$ref':'#/components/requestBodies/In'},'responses':{'200':{'$ref':'#/components/responses/Ok'}}}", components);
    JsonNode inline =
        doc(
            "{'operationId':'a','parameters':[{'name':'q','in':'query','required':true,'schema':{'type':'string'}}],'requestBody':{'required':true,'content':{'application/json':{'schema':{'type':'string'}}}},"
                + "'responses':{'200':{'description':'ok','headers':{'X-Total':{'schema':{'type':'integer'}}}}}}",
            "{}");

    assertSame(referenced, inline);
  }

  // ---- constructs the comparator does not know ----

  static Stream<Arguments> unsupportedConstructs() {
    String response = "{'description':'ok','content':{'application/json':{'schema':{'type':'string'}}}}";
    return Stream.of(
        Arguments.of("an unknown schema keyword", docWithSchema("{'type':'string','x-vendor':1}"), "x-vendor"),
        Arguments.of("a schema xml", docWithSchema("{'type':'string','xml':{'name':'n'}}"), "xml"),
        Arguments.of("a 3.1 schema keyword", docWithSchema("{'type':'object','patternProperties':{}}"), "patternProperties"),
        Arguments.of("a 3.1 content keyword", docWithSchema("{'type':'string','contentEncoding':'base64'}"), "contentEncoding"),
        Arguments.of("an operation callback", doc("{'operationId':'a','callbacks':{},'responses':{}}", "{}"), "callbacks"),
        Arguments.of("an operation server", doc("{'operationId':'a','servers':[],'responses':{}}", "{}"), "servers"),
        Arguments.of("an operation extension", doc("{'operationId':'a','x-codegen':true,'responses':{}}", "{}"), "x-codegen"),
        Arguments.of("a parameter keyword", docWithParameter("{'name':'q','in':'query','schema':{'type':'string'},'foo':1}"), "foo"),
        Arguments.of("a response link", doc("{'operationId':'a','responses':{'200':{'description':'ok','links':{}}}}", "{}"), "links"),
        Arguments.of("a header keyword", docWithHeader("{'schema':{'type':'string'},'foo':1}"), "foo"),
        Arguments.of("a request body keyword", doc("{'operationId':'a','requestBody':{'x-body':1,'content':{}},'responses':{}}", "{}"), "x-body"),
        Arguments.of("a media type keyword", doc("{'operationId':'a','responses':{'200':{'description':'ok','content':{'application/json':{'x-media':1}}}}}", "{}"), "x-media"),
        Arguments.of("a document keyword", j("{'paths':{'/a':{'get':{'operationId':'a','responses':{'200':" + response + "}}}},'webhooks':{}}"), "webhooks"),
        Arguments.of("a path item reference", j("{'paths':{'/a':{'$ref':'#/x','get':{'operationId':'a','responses':{}}}}}"), "$ref"),
        Arguments.of("a path item server", j("{'paths':{'/a':{'servers':[],'get':{'operationId':'a','responses':{}}}}}"), "servers"),
        Arguments.of("a security scheme keyword", docWithSecurity("[{'a':[]}]", "{'securitySchemes':{'a':{'type':'http','scheme':'bearer','x-y':1}}}"), "x-y"),
        Arguments.of("an undeclared security scheme", docWithSecurity("[{'missing':[]}]"), "missing"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unsupportedConstructs")
  void aConstructTheComparatorDoesNotKnowIsRefusedNamingIt(String what, JsonNode document, String fragment) {
    assertRefused(document, fragment);
    assertThrows(IllegalStateException.class, () -> OpenApiInventory.compare(document, document));
  }

  @Test
  void everyRefusedConstructIsReportedAtOnce() {
    IllegalStateException refusal =
        assertThrows(
            IllegalStateException.class,
            () -> OpenApiInventory.inventory(docWithSchema("{'type':'string','x-one':1,'x-two':2,'$ref2':3}")));

    assertTrue(refusal.getMessage().contains("x-one") && refusal.getMessage().contains("x-two"), refusal.getMessage());
  }

  @Test
  void documentationIsNotAContractOnAnyObject() {
    JsonNode plain =
        j(
            "{'paths':{'/a':{'post':{'operationId':'a','tags':['t'],'security':[{'a':[]}],"
                + "'parameters':[{'name':'q','in':'query','schema':{'type':'string'}}],"
                + "'requestBody':{'content':{'application/json':{'schema':{'type':'string'}}}},"
                + "'responses':{'200':{'content':{'application/json':{'schema':{'type':'string'}}},'headers':{'X-A':{'schema':{'type':'string'}}}}}}}},"
                + "'components':"
                + SCHEMES
                + "}");
    String prose = "'summary':'s','description':'d','example':'e','examples':['e'],'externalDocs':{'url':'u'},'title':'t'";
    JsonNode documented =
        j(
            "{'info':{'title':'x'},'tags':[{'name':'t','description':'d'}],'servers':[{'url':'http://other:9/','description':'d'}],"
                + "'paths':{'/a':{"
                + prose
                + ",'post':{"
                + prose
                + ",'operationId':'a','tags':['t'],'security':[{'a':[]}],"
                + "'parameters':[{"
                + prose
                + ",'name':'q','in':'query','schema':{'type':'string',"
                + prose
                + "}}],"
                + "'requestBody':{"
                + prose
                + ",'content':{'application/json':{"
                + prose
                + ",'schema':{'type':'string',"
                + prose
                + "}}}},"
                + "'responses':{'200':{"
                + prose
                + ",'content':{'application/json':{"
                + prose
                + ",'schema':{'type':'string',"
                + prose
                + "}}},'headers':{'X-A':{"
                + prose
                + ",'schema':{'type':'string'}}}}}}}},"
                + "'components':{'securitySchemes':{'a':{'type':'http','scheme':'bearer','bearerFormat':'JWT','description':'d'},'b':{'type':'apiKey','in':'header','name':'X-Key'}}}}");

    assertSame(plain, documented);
  }

  // ---- operations, parameters, headers, bodies ----

  @Test
  void aDeprecatedOperationIsADifferenceButDeprecatedFalseIsNot() {
    JsonNode plain = doc("{'operationId':'a','responses':{}}", "{}");

    assertDetected(plain, doc("{'operationId':'a','deprecated':true,'responses':{}}", "{}"), "deprecated");
    assertSame(plain, doc("{'operationId':'a','deprecated':false,'responses':{}}", "{}"));
  }

  static Stream<Arguments> parameterKeywordsAddedToAQueryParameter() {
    return Stream.of(
        Arguments.of("style", "'style':'spaceDelimited'"),
        Arguments.of("style", "'style':'pipeDelimited'"),
        Arguments.of("style", "'style':'deepObject'"),
        Arguments.of("explode", "'explode':false"),
        Arguments.of("allowReserved", "'allowReserved':true"),
        Arguments.of("deprecated", "'deprecated':true"),
        Arguments.of("allowEmptyValue", "'allowEmptyValue':true"),
        Arguments.of("required", "'required':true"));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("parameterKeywordsAddedToAQueryParameter")
  void everyParameterKeywordIsCompared(String fragment, String keyword) {
    assertDetected(
        docWithParameter("{'name':'q','in':'query','schema':{'type':'string'}}"),
        docWithParameter("{'name':'q','in':'query','schema':{'type':'string'}," + keyword + "}"),
        "param query q " + fragment);
  }

  @Test
  void aParameterSpelledWithItsDefaultsIsTheSameParameter() {
    JsonNode implicit = docWithParameter("{'name':'q','in':'query','schema':{'type':'string'}}");

    for (String defaults : List.of("'style':'form'", "'explode':true", "'style':'form','explode':true", "'required':false", "'deprecated':false", "'allowReserved':false", "'allowEmptyValue':false", "'description':'d','example':'e'")) {
      assertSame(implicit, docWithParameter("{'name':'q','in':'query','schema':{'type':'string'}," + defaults + "}"));
    }
    assertSame(
        docWithParameter("{'name':'id','in':'path','required':true,'schema':{'type':'string'}}"),
        docWithParameter("{'name':'id','in':'path','required':true,'style':'simple','explode':false,'schema':{'type':'string'}}"));
    assertSame(
        docWithParameter("{'name':'q','in':'query','style':'spaceDelimited','schema':{'type':'array','items':{'type':'string'}}}"),
        docWithParameter("{'name':'q','in':'query','style':'spaceDelimited','explode':false,'schema':{'type':'array','items':{'type':'string'}}}"));
  }

  @Test
  void theStyleOfAParameterFollowsItsLocation() {
    assertDetected(
        docWithParameter("{'name':'id','in':'path','required':true,'schema':{'type':'string'}}"),
        docWithParameter("{'name':'id','in':'path','required':true,'style':'label','schema':{'type':'string'}}"),
        "param path id style");
    assertDetected(
        docWithParameter("{'name':'c','in':'cookie','schema':{'type':'string'}}"),
        docWithParameter("{'name':'c','in':'cookie','style':'form','explode':false,'schema':{'type':'string'}}"),
        "param cookie c explode");
    assertDetected(
        docWithParameter("{'name':'q','in':'query','style':'spaceDelimited','schema':{'type':'array','items':{'type':'string'}}}"),
        docWithParameter("{'name':'q','in':'query','style':'spaceDelimited','explode':true,'schema':{'type':'array','items':{'type':'string'}}}"),
        "explode");
  }

  @Test
  void aHeaderParameterIsIdentifiedWithoutItsCaseButAQueryParameterIsNot() {
    assertSame(
        docWithParameter("{'name':'X-Request-Id','in':'header','schema':{'type':'string'}}"),
        docWithParameter("{'name':'x-request-id','in':'header','schema':{'type':'string'}}"));
    assertDetected(
        docWithParameter("{'name':'userId','in':'query','schema':{'type':'string'}}"),
        docWithParameter("{'name':'userid','in':'query','schema':{'type':'string'}}"),
        "param query");
  }

  @Test
  void aParameterDescribedByContentIsNotTheSameAsOneDescribedBySchema() {
    assertDetected(
        docWithParameter("{'name':'q','in':'query','schema':{'type':'string'}}"),
        docWithParameter("{'name':'q','in':'query','content':{'application/json':{'schema':{'type':'string'}}}}"),
        "param query q");
  }

  static Stream<Arguments> headerChanges() {
    return Stream.of(
        Arguments.of("{'schema':{'type':'integer'}}", "{'schema':{'type':'string'}}", "header x-total schema type"),
        Arguments.of("{'schema':{'type':'integer'}}", "{'schema':{'type':'integer','minimum':1}}", "header x-total schema minimum"),
        Arguments.of("{'schema':{'type':'integer'}}", "{'schema':{'type':'integer'},'required':true}", "header x-total required"),
        Arguments.of("{'schema':{'type':'integer'}}", "{'schema':{'type':'integer'},'explode':true}", "header x-total explode"),
        Arguments.of("{'schema':{'type':'integer'}}", "{'schema':{'type':'integer'},'deprecated':true}", "header x-total deprecated"),
        Arguments.of("{'schema':{'type':'integer'}}", "{'schema':{'type':'integer'},'style':'simple','allowEmptyValue':true}", "header x-total allowEmptyValue"),
        Arguments.of("{'schema':{'type':'string'}}", "{'content':{'text/plain':{'schema':{'type':'string'}}}}", "header x-total"));
  }

  @ParameterizedTest(name = "{2}")
  @MethodSource("headerChanges")
  void aResponseHeaderIsComparedWithItsSchemaAndFlags(String first, String second, String fragment) {
    assertDetected(docWithHeader(first), docWithHeader(second), "response 200 " + fragment.substring(0, fragment.indexOf("x-total") + 7));
    assertDetected(docWithHeader(first), docWithHeader(second), fragment);
  }

  @Test
  void aResponseHeaderIsComparedByPresenceAndNotByCase() {
    assertDetected(doc("{'operationId':'a','responses':{'200':{'description':'ok'}}}", "{}"), docWithHeader("{'schema':{'type':'integer'}}"), "response 200 header x-total");
    assertSame(
        docWithHeader("{'schema':{'type':'integer'}}"),
        doc("{'operationId':'a','responses':{'200':{'description':'ok','headers':{'x-total':{'description':'d','required':false,'style':'simple','explode':false,'schema':{'type':'integer'}}}}}}", "{}"));
  }

  @Test
  void aMultipartEncodingAndAMediaTypeParameterAreCompared() {
    String multipart = "{'operationId':'a','requestBody':{'content':{'multipart/form-data':{'schema':{'type':'object','properties':{'file':{'type':'string','format':'binary'}}},'encoding':{'file':{'contentType':'%s'}}}}},'responses':{}}";

    assertDetected(doc(multipart.formatted("image/png"), "{}"), doc(multipart.formatted("application/pdf"), "{}"), "encoding file contentType");
    assertSame(doc(multipart.formatted("image/png"), "{}"), doc(multipart.formatted("Image/PNG"), "{}"));
    assertDetected(
        doc("{'operationId':'a','requestBody':{'content':{'multipart/form-data':{'schema':{'type':'string'}}}},'responses':{}}", "{}"),
        doc(multipart.formatted("image/png"), "{}"),
        "encoding file");
    assertSame(
        doc("{'operationId':'a','responses':{'200':{'description':'ok','content':{'application/json;charset=UTF-8':{'schema':{'type':'string'}}}}}}", "{}"),
        doc("{'operationId':'a','responses':{'200':{'description':'ok','content':{'application/json; charset=utf-8':{'schema':{'type':'string'}}}}}}", "{}"));
    assertDetected(
        doc("{'operationId':'a','responses':{'200':{'description':'ok','content':{'application/json;charset=UTF-8':{'schema':{'type':'string'}}}}}}", "{}"),
        doc("{'operationId':'a','responses':{'200':{'description':'ok','content':{'application/json':{'schema':{'type':'string'}}}}}}", "{}"),
        "application/json");
  }

  // ---- security: alternatives, combinations, scopes, scheme definition ----

  @Test
  void alternativeAndCombinedSecurityRequirementsAreDifferent() {
    assertDetected(docWithSecurity("[{'a':[],'b':[]}]"), docWithSecurity("[{'a':[]},{'b':[]}]"), "security");
    assertSame(docWithSecurity("[{'a':[]},{'b':[]}]"), docWithSecurity("[{'b':[]},{'a':[]},{'a':[]}]"));
    assertSame(docWithSecurity("[{'a':[],'b':[]}]"), docWithSecurity("[{'b':[],'a':[]}]"));
    assertDetected(docWithSecurity("[{'a':[]}]"), docWithSecurity("[{'a':[],'b':[]}]"), "security");
  }

  @Test
  void theScopesOfARequirementAreCompared() {
    assertDetected(docWithSecurity("[{'a':[]}]"), docWithSecurity("[{'a':['global']}]"), "security");
    assertDetected(docWithSecurity("[{'a':['read']}]"), docWithSecurity("[{'a':['write']}]"), "security");
    assertDetected(docWithSecurity("[{'a':['read']}]"), docWithSecurity("[{'a':['read','write']}]"), "security");
    assertSame(docWithSecurity("[{'a':['read','write']}]"), docWithSecurity("[{'a':['write','read']}]"));
  }

  @Test
  void anAnonymousAlternativeIsNotNoSecurityAndNotSecurity() {
    assertDetected(docWithSecurity("[]"), docWithSecurity("[{}]"), "security");
    assertDetected(docWithSecurity("[{'a':[]}]"), docWithSecurity("[{'a':[]},{}]"), "security");
    assertSame(doc("{'operationId':'a','responses':{}}", SCHEMES), docWithSecurity("[]"));
  }

  @Test
  void theDefinitionOfASchemeIsComparedButNotItsName() {
    String bearer = "'a':{'type':'http','scheme':'bearer','bearerFormat':'JWT'}";
    assertSame(
        docWithSecurity("[{'a':[]}]", "{'securitySchemes':{" + bearer + "}}"),
        docWithSecurity("[{'bearerAuth':[]}]", "{'securitySchemes':{'bearerAuth':{'type':'http','scheme':'Bearer','bearerFormat':'JWT','description':'d'}}}"));
    assertDetected(
        docWithSecurity("[{'a':[]}]", "{'securitySchemes':{" + bearer + "}}"),
        docWithSecurity("[{'a':[]}]", "{'securitySchemes':{'a':{'type':'http','scheme':'bearer','bearerFormat':'opaque'}}}"),
        "security");
    assertDetected(
        docWithSecurity("[{'a':[]}]", "{'securitySchemes':{" + bearer + "}}"),
        docWithSecurity("[{'a':[]}]", "{'securitySchemes':{'a':{'type':'http','scheme':'basic'}}}"),
        "security");
  }

  @Test
  void anApiKeyIsComparedByLocationAndName() {
    String key = "{'securitySchemes':{'k':{'type':'apiKey','in':'header','name':'X-Key'}}}";
    assertDetected(docWithSecurity("[{'k':[]}]", key), docWithSecurity("[{'k':[]}]", "{'securitySchemes':{'k':{'type':'apiKey','in':'header','name':'X-Other'}}}"), "security");
    assertDetected(docWithSecurity("[{'k':[]}]", key), docWithSecurity("[{'k':[]}]", "{'securitySchemes':{'k':{'type':'apiKey','in':'query','name':'X-Key'}}}"), "security");
    assertDetected(docWithSecurity("[{'k':[]}]", key), docWithSecurity("[{'k':[]}]", "{'securitySchemes':{'k':{'type':'http','scheme':'bearer'}}}"), "security");
    assertSame(docWithSecurity("[{'k':[]}]", key), docWithSecurity("[{'other':[]}]", "{'securitySchemes':{'other':{'type':'apiKey','in':'header','name':'X-Key'}}}"));
  }

  @Test
  void theScopesAndEndpointsOfAnOauthFlowAreCompared() {
    String flow = "{'securitySchemes':{'o':{'type':'oauth2','flows':{'clientCredentials':{'tokenUrl':'https://t','scopes':{'read':'r','write':'w'}}}}}}";
    String sameFlow = "{'securitySchemes':{'o':{'type':'oauth2','flows':{'clientCredentials':{'tokenUrl':'https://t','scopes':{'write':'W','read':'R'}}}}}}";
    String otherScopes = "{'securitySchemes':{'o':{'type':'oauth2','flows':{'clientCredentials':{'tokenUrl':'https://t','scopes':{'read':'r'}}}}}}";
    String otherUrl = "{'securitySchemes':{'o':{'type':'oauth2','flows':{'clientCredentials':{'tokenUrl':'https://u','scopes':{'read':'r','write':'w'}}}}}}";

    assertSame(docWithSecurity("[{'o':['read']}]", flow), docWithSecurity("[{'o':['read']}]", sameFlow));
    assertDetected(docWithSecurity("[{'o':['read']}]", flow), docWithSecurity("[{'o':['read']}]", otherScopes), "security");
    assertDetected(docWithSecurity("[{'o':['read']}]", flow), docWithSecurity("[{'o':['read']}]", otherUrl), "security");
  }

  @Test
  void theSecurityOfTheDocumentIsInheritedUnlessTheOperationDeclaresItsOwn() {
    ObjectNode global = (ObjectNode) doc("{'operationId':'a','responses':{}}", SCHEMES).deepCopy();
    global.set("security", j("[{'a':[]}]"));

    assertSame(global, docWithSecurity("[{'a':[]}]"));
    assertDetected(global, doc("{'operationId':'a','security':[],'responses':{}}", SCHEMES), "security");
    assertDetected(global, doc("{'operationId':'a','security':[{'b':[]}],'responses':{}}", SCHEMES), "security");
  }

  // ---- document level ----

  @Test
  void theServerHostIsDeploymentButItsBasePathIsRouting() {
    JsonNode plain = docWithSchema("{'type':'string'}");
    ObjectNode other = plain.deepCopy();
    other.set("servers", j("[{'url':'http://localhost:8080','description':'d'},{'url':'https://0.0.0.0:8443/'}]"));
    ObjectNode prefixed = plain.deepCopy();
    prefixed.set("servers", j("[{'url':'http://localhost:8080/api/'}]"));
    ObjectNode relative = plain.deepCopy();
    relative.set("servers", j("[{'url':'/'}]"));
    ObjectNode templated = plain.deepCopy();
    templated.set("servers", j("[{'url':'https://{host}/api','variables':{'host':{'default':'h'}}}]"));

    assertSame(plain, other);
    assertSame(plain, relative);
    assertDetected(plain, prefixed, "DOCUMENT | DIFFERENT servers base path");
    assertThrows(IllegalStateException.class, () -> OpenApiInventory.compare(plain, templated));
  }

  // ---- sweeps over the real Spring document: no keyword of any node is silently dropped ----

  private static String pointer(String segment) {
    return segment.replace("~", "~0").replace("/", "~1");
  }

  private static Set<String> reachableModels(JsonNode spec) {
    Set<String> reachable = new LinkedHashSet<>();
    List<JsonNode> todo = new ArrayList<>();
    todo.add(spec.path("paths"));
    while (!todo.isEmpty()) {
      JsonNode node = todo.remove(todo.size() - 1);
      if (node.has("$ref")) {
        String ref = node.get("$ref").asText();
        String name = ref.substring(ref.lastIndexOf('/') + 1);
        if (reachable.add(name)) {
          todo.add(spec.path("components").path("schemas").path(name));
        }
      }
      node.forEach(todo::add);
    }
    return reachable;
  }

  @Test
  void everyKeywordAddedToEveryPropertyOfEveryReachableSpringModelIsDetected() {
    ObjectNode spring = spring();
    Map<String, Map<String, String>> baseline = OpenApiInventory.inventory(spring);
    List<String> keywords =
        List.of(
            "'readOnly':true", "'writeOnly':true", "'deprecated':true", "'minLength':3", "'maxLength':7", "'minimum':1", "'exclusiveMaximum':8",
            "'multipleOf':3", "'minItems':1", "'minProperties':1", "'pattern':'^zz$'", "'const':'zz'", "'format':'zz'", "'uniqueItems':true");

    int mutations = 0;
    List<String> undetected = new ArrayList<>();
    for (String model : reachableModels(spring)) {
      JsonNode properties = spring.path("components").path("schemas").path(model).path("properties");
      for (Iterator<String> names = properties.fieldNames(); names.hasNext(); ) {
        String property = names.next();
        if (properties.path(property).has("$ref")) {
          continue;
        }
        for (String keyword : keywords) {
          ObjectNode actual = spring.deepCopy();
          ObjectNode target = (ObjectNode) actual.at("/components/schemas/" + pointer(model) + "/properties/" + pointer(property));
          target.setAll((ObjectNode) j("{" + keyword + "}"));
          mutations++;
          if (OpenApiInventory.inventory(actual).equals(baseline)) {
            undetected.add(model + "." + property + " " + keyword);
          }
        }
      }
    }

    assertTrue(mutations >= 500, "only " + mutations + " mutations: the sweep is vacuous");
    assertTrue(undetected.isEmpty(), undetected.size() + " of " + mutations + " mutations went unnoticed: " + undetected);
  }

  @Test
  void everyOperationParameterAndResponseOfTheSpringDocumentIsComparedInEveryAspect() {
    ObjectNode spring = spring();
    Map<String, Map<String, String>> baseline = OpenApiInventory.inventory(spring);
    int mutations = 0;
    List<String> undetected = new ArrayList<>();

    for (Iterator<Map.Entry<String, JsonNode>> paths = spring.path("paths").fields(); paths.hasNext(); ) {
      Map.Entry<String, JsonNode> path = paths.next();
      for (Iterator<String> methods = path.getValue().fieldNames(); methods.hasNext(); ) {
        String operation = "/paths/" + pointer(path.getKey()) + "/" + methods.next();
        JsonNode original = spring.at(operation);
        List<String> changes = new ArrayList<>(List.of("deprecated", "tag", "securityNone", "securityScope", "header200"));
        for (int p = 0; p < original.path("parameters").size(); p++) {
          for (String change : List.of("p.deprecated", "p.allowReserved", "p.style", "p.explode", "p.requiredFlip", "p.remove", "p.schemaMinLength")) {
            changes.add(change + ":" + p);
          }
        }
        for (Iterator<String> statuses = original.path("responses").fieldNames(); statuses.hasNext(); ) {
          String status = statuses.next();
          changes.add("r.remove:" + status);
          changes.add("r.header:" + status);
        }
        if (original.has("requestBody")) {
          changes.add("body.requiredFlip");
          changes.add("body.remove");
        }

        for (String change : changes) {
          ObjectNode actual = spring.deepCopy();
          ObjectNode op = (ObjectNode) actual.at(operation);
          String[] parts = change.split(":");
          switch (parts[0]) {
            case "deprecated" -> op.put("deprecated", true);
            case "tag" -> op.putArray("tags").add("zz");
            case "securityNone" -> op.putArray("security");
            case "securityScope" -> op.set("security", j("[{'bearerAuth':['zz']}]"));
            case "header200" -> ((ObjectNode) op.path("responses").elements().next()).set("headers", j("{'X-Probe':{'schema':{'type':'string'}}}"));
            case "p.deprecated" -> ((ObjectNode) op.path("parameters").get(Integer.parseInt(parts[1]))).put("deprecated", true);
            case "p.allowReserved" -> ((ObjectNode) op.path("parameters").get(Integer.parseInt(parts[1]))).put("allowReserved", true);
            case "p.style" -> ((ObjectNode) op.path("parameters").get(Integer.parseInt(parts[1]))).put("style", "zz");
            case "p.explode" -> {
              ObjectNode parameter = (ObjectNode) op.path("parameters").get(Integer.parseInt(parts[1]));
              boolean query = "query".equals(parameter.path("in").asText());
              parameter.put("explode", !query);
            }
            case "p.requiredFlip" -> {
              ObjectNode parameter = (ObjectNode) op.path("parameters").get(Integer.parseInt(parts[1]));
              if ("path".equals(parameter.path("in").asText())) {
                parameter.put("style", "label");
              } else {
                parameter.put("required", !parameter.path("required").asBoolean(false));
              }
            }
            case "p.remove" -> ((ArrayNode) op.path("parameters")).remove(Integer.parseInt(parts[1]));
            case "p.schemaMinLength" -> ((ObjectNode) op.path("parameters").get(Integer.parseInt(parts[1]))).with("schema").put("minLength", 9);
            case "r.remove" -> ((ObjectNode) op.path("responses")).remove(parts[1]);
            case "r.header" -> ((ObjectNode) op.path("responses").path(parts[1])).set("headers", j("{'X-Probe':{'schema':{'type':'string'}}}"));
            case "body.requiredFlip" -> ((ObjectNode) op.path("requestBody")).put("required", !op.path("requestBody").path("required").asBoolean(false));
            case "body.remove" -> op.remove("requestBody");
            default -> throw new IllegalStateException(change);
          }
          mutations++;
          if (OpenApiInventory.inventory(actual).equals(baseline)) {
            undetected.add(operation + " " + change);
          }
        }
      }
    }

    assertTrue(mutations >= 600, "only " + mutations + " mutations: the sweep is vacuous");
    assertTrue(undetected.isEmpty(), undetected.size() + " of " + mutations + " mutations went unnoticed: " + undetected);
  }

  /** Adds {@code keyword} to every schema node reachable through properties, items, additionalProperties and the combinators. */
  private static int annotateSchema(JsonNode schema, String keyword, JsonNode value) {
    if (!schema.isObject()) {
      return 0;
    }
    ((ObjectNode) schema).set(keyword, value);
    int touched = 1;
    for (JsonNode property : schema.path("properties")) {
      touched += annotateSchema(property, keyword, value);
    }
    touched += annotateSchema(schema.path("items"), keyword, value);
    touched += annotateSchema(schema.path("additionalProperties"), keyword, value);
    for (String combinator : List.of("allOf", "oneOf", "anyOf")) {
      for (JsonNode member : schema.path(combinator)) {
        touched += annotateSchema(member, keyword, value);
      }
    }
    return touched;
  }

  private static int annotateContent(JsonNode content, String keyword, JsonNode value) {
    int touched = 0;
    for (JsonNode media : content) {
      ((ObjectNode) media).set(keyword, value);
      touched += 1 + annotateSchema(media.path("schema"), keyword, value);
    }
    return touched;
  }

  @ParameterizedTest
  @ValueSource(strings = {"summary", "description", "example", "examples", "externalDocs", "title"})
  void documentationAddedToEveryObjectOfTheSpringDocumentIsNotADifference(String keyword) {
    ObjectNode documented = spring();
    JsonNode value = switch (keyword) {
      case "externalDocs" -> j("{'url':'https://example.org'}");
      case "examples" -> j("['zz']");
      default -> j("'zz'");
    };
    int touched = 0;
    for (JsonNode pathItem : documented.path("paths")) {
      for (JsonNode operation : pathItem) {
        ((ObjectNode) operation).set(keyword, value);
        touched++;
        for (JsonNode parameter : operation.path("parameters")) {
          ((ObjectNode) parameter).set(keyword, value);
          touched += 1 + annotateSchema(parameter.path("schema"), keyword, value);
        }
        if (operation.has("requestBody")) {
          ((ObjectNode) operation.path("requestBody")).set(keyword, value);
          touched += 1 + annotateContent(operation.path("requestBody").path("content"), keyword, value);
        }
        for (JsonNode response : operation.path("responses")) {
          ((ObjectNode) response).set(keyword, value);
          touched += 1 + annotateContent(response.path("content"), keyword, value);
        }
      }
    }
    for (JsonNode schema : documented.path("components").path("schemas")) {
      touched += annotateSchema(schema, keyword, value);
    }

    assertTrue(touched > 1000, "only " + touched + " nodes annotated: the sweep is vacuous");
    assertSame(spring(), documented);
  }
}
