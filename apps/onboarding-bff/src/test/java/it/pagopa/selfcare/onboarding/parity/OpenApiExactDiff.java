package it.pagopa.selfcare.onboarding.parity;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Structural comparison of two JSON documents with nothing tolerated: object members are compared
 * regardless of their order, array elements by position, every value including the texts. The
 * differences are reported with the JSON pointer where they occur.
 */
final class OpenApiExactDiff {

  private static final int MAX_VALUE_LENGTH = 100;

  private OpenApiExactDiff() {}

  static List<String> diff(JsonNode expected, JsonNode actual) {
    List<String> differences = new ArrayList<>();
    walk(expected, actual, "", differences);
    return differences;
  }

  private static void walk(JsonNode expected, JsonNode actual, String pointer, List<String> differences) {
    if (expected.isObject() && actual.isObject()) {
      TreeSet<String> names = new TreeSet<>();
      expected.fieldNames().forEachRemaining(names::add);
      actual.fieldNames().forEachRemaining(names::add);
      for (String name : names) {
        String child = pointer + "/" + name.replace("~", "~0").replace("/", "~1");
        if (!actual.has(name)) {
          differences.add("missing " + child + " (expected " + show(expected.get(name)) + ")");
        } else if (!expected.has(name)) {
          differences.add("unexpected " + child + " = " + show(actual.get(name)));
        } else {
          walk(expected.get(name), actual.get(name), child, differences);
        }
      }
    } else if (expected.isArray() && actual.isArray()) {
      if (expected.size() != actual.size()) {
        differences.add("array length " + pointer + ": expected " + expected.size() + " but was " + actual.size()
            + " (expected " + show(expected) + ", was " + show(actual) + ")");
        return;
      }
      for (int i = 0; i < expected.size(); i++) {
        walk(expected.get(i), actual.get(i), pointer + "/" + i, differences);
      }
    } else if (!expected.equals(actual)) {
      differences.add("value " + pointer + ": expected " + show(expected) + " but was " + show(actual));
    }
  }

  private static String show(JsonNode node) {
    String text = node.toString();
    return text.length() <= MAX_VALUE_LENGTH ? text : text.substring(0, MAX_VALUE_LENGTH) + "...";
  }
}
