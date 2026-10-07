package it.pagopa.selfcare.onboarding.parity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Every behavioural scenario, executed identically against the Spring reference and the Quarkus BFF. */
public final class ParityCatalog {

  /** Scenarios validated on the Spring reference when this floor was set; the catalog may only grow. */
  public static final int MINIMUM_SCENARIOS = 516;

  /** Optional regular expression on the scenario names, to iterate on one group: {@code -Dparity.only='^security ::'}. */
  public static final String ONLY_PROPERTY = "parity.only";

  private ParityCatalog() {}

  /** The scenarios a run must execute: the whole catalog, never empty and never smaller than the floor, unless narrowed on purpose. */
  public static List<Scenario> selected() {
    List<Scenario> all = all();
    if (all.size() < MINIMUM_SCENARIOS) {
      throw new IllegalStateException("the parity catalog shrank to " + all.size() + " scenarios (minimum " + MINIMUM_SCENARIOS + ")");
    }
    String only = System.getProperty(ONLY_PROPERTY);
    if (only == null || only.isBlank()) {
      return all;
    }
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(only);
    List<Scenario> picked = all.stream().filter(scenario -> pattern.matcher(scenario.displayName()).find()).toList();
    if (picked.isEmpty()) {
      throw new IllegalStateException("-D" + ONLY_PROPERTY + "=" + only + " matches no scenario");
    }
    return picked;
  }

  public static List<Scenario> all() {
    List<Scenario> scenarios = new ArrayList<>();
    scenarios.addAll(SecurityScenarios.all());
    scenarios.addAll(TokenScenarios.all());
    scenarios.addAll(ProductScenarios.all());
    scenarios.addAll(InstitutionScenarios.all());
    scenarios.addAll(UserScenarios.all());
    scenarios.addAll(TransportScenarios.all());
    scenarios.addAll(ErrorScenarios.all());
    scenarios.addAll(SpecDrivenScenarios.all());
    scenarios.addAll(HttpContractScenarios.all());
    scenarios.addAll(RequestBindingScenarios.all());
    Set<String> names = new HashSet<>();
    for (Scenario scenario : scenarios) {
      if (!names.add(scenario.displayName())) {
        throw new IllegalStateException("duplicate scenario " + scenario.displayName());
      }
      scenario.expect(scenario.expectation.andThen(check ->
          check.downstreamApiKey(ParityTargets.USER_REGISTRY_API_KEY)));
    }
    return scenarios;
  }
}
