package it.pagopa.selfcare.auth.integration_test;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;

public class IntegrationProfile implements QuarkusTestProfile {

  @Override
  public List<TestResourceEntry> testResources() {
    return List.of(new TestResourceEntry(AuthIntegrationTestResource.class));
  }
}
