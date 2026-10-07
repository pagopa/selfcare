package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Always runs, with or without the Spring jar: a parity run that executes no scenario, or fewer
 * than the validated floor, must fail instead of passing silently.
 */
class ParityCatalogTest {

  @Test
  void catalogIsNotSmallerThanTheValidatedFloor() {
    List<Scenario> all = ParityCatalog.all();

    assertFalse(all.isEmpty(), "the parity catalog is empty");
    assertTrue(all.size() >= ParityCatalog.MINIMUM_SCENARIOS, "the parity catalog shrank to " + all.size());
  }

  @Test
  void scenarioNamesAreUniqueAndDescribeTheirOperation() {
    Set<String> names = new HashSet<>();
    for (Scenario scenario : ParityCatalog.all()) {
      assertNotNull(scenario.group);
      assertNotNull(scenario.id);
      assertTrue(scenario.displayName().contains("::"), scenario.displayName());
      assertTrue(names.add(scenario.displayName()), "duplicate " + scenario.displayName());
    }
  }

  @Test
  void everyGroupHasScenarios() {
    Set<String> groups = new HashSet<>();
    ParityCatalog.all().forEach(scenario -> groups.add(scenario.group));

    assertTrue(
        groups.containsAll(
            Set.of("security", "tokens", "products", "institutions", "users", "transport-failures", "downstream-errors", "http-contract", "spec-driven")),
        groups.toString());
  }

  @Test
  void everyScenarioDeclaresAnExpectation() {
    for (Scenario scenario : ParityCatalog.all()) {
      assertNotNull(scenario.expectation, scenario.displayName());
    }
  }

  @Test
  void selectedIsTheWholeCatalogUnlessNarrowedOnPurpose() {
    withSelection(null, () -> assertEquals(ParityCatalog.all().size(), ParityCatalog.selected().size()));
  }

  @Test
  void aNarrowingThatMatchesNothingIsAnErrorNotAnEmptyPass() {
    withSelection(
        "^no-such-group ::",
        () -> assertThrows(IllegalStateException.class, ParityCatalog::selected));
  }

  private static void withSelection(String selection, Runnable assertion) {
    String previous = System.getProperty(ParityCatalog.ONLY_PROPERTY);
    try {
      if (selection == null) {
        System.clearProperty(ParityCatalog.ONLY_PROPERTY);
      } else {
        System.setProperty(ParityCatalog.ONLY_PROPERTY, selection);
      }
      assertion.run();
    } finally {
      if (previous == null) {
        System.clearProperty(ParityCatalog.ONLY_PROPERTY);
      } else {
        System.setProperty(ParityCatalog.ONLY_PROPERTY, previous);
      }
    }
  }
}
