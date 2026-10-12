package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The exact comparison must see every kind of difference: a gate that tolerates one is a false green. */
class OpenApiExactDiffTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode json(String text) throws IOException {
    return MAPPER.readTree(text);
  }

  @Test
  void identicalDocumentsHaveNoDifference() throws IOException {
    assertEquals(List.of(), OpenApiExactDiff.diff(json("{\"a\":{\"b\":[1,{\"c\":\"x\"}]}}"), json("{\"a\":{\"b\":[1,{\"c\":\"x\"}]}}")));
  }

  @Test
  void theOrderOfTheObjectMembersIsNotADifference() throws IOException {
    assertEquals(List.of(), OpenApiExactDiff.diff(json("{\"a\":1,\"b\":2}"), json("{\"b\":2,\"a\":1}")));
  }

  @Test
  void aChangedTextIsReportedWithItsPointer() throws IOException {
    List<String> differences = OpenApiExactDiff.diff(json("{\"a\":{\"description\":\"one\"}}"), json("{\"a\":{\"description\":\"two\"}}"));

    assertEquals(1, differences.size());
    assertTrue(differences.get(0).contains("/a/description"), differences.get(0));
  }

  @Test
  void aMissingAndAnUnexpectedMemberAreReported() throws IOException {
    List<String> differences = OpenApiExactDiff.diff(json("{\"a\":1}"), json("{\"b\":1}"));

    assertEquals(2, differences.size());
    assertTrue(differences.stream().anyMatch(d -> d.startsWith("missing /a")), differences.toString());
    assertTrue(differences.stream().anyMatch(d -> d.startsWith("unexpected /b")), differences.toString());
  }

  @Test
  void theOrderOfAnArrayIsADifference() throws IOException {
    assertEquals(2, OpenApiExactDiff.diff(json("{\"required\":[\"a\",\"b\"]}"), json("{\"required\":[\"b\",\"a\"]}")).size());
  }

  @Test
  void aDifferentArrayLengthIsADifference() throws IOException {
    List<String> differences = OpenApiExactDiff.diff(json("{\"a\":[1,2]}"), json("{\"a\":[1]}"));

    assertEquals(1, differences.size());
    assertTrue(differences.get(0).startsWith("array length /a"), differences.get(0));
  }

  @Test
  void aDifferentTypeOfValueIsADifference() throws IOException {
    assertEquals(1, OpenApiExactDiff.diff(json("{\"a\":\"1\"}"), json("{\"a\":1}")).size());
    assertEquals(1, OpenApiExactDiff.diff(json("{\"a\":{}}"), json("{\"a\":[]}")).size());
    assertEquals(1, OpenApiExactDiff.diff(json("{\"a\":null}"), json("{\"a\":false}")).size());
  }

  @Test
  void anEmptyObjectIsNotAMissingMember() throws IOException {
    assertEquals(1, OpenApiExactDiff.diff(json("{\"schema\":{}}"), json("{}")).size());
  }
}
