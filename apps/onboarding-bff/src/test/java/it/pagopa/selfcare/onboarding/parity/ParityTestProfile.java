package it.pagopa.selfcare.onboarding.parity;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;

/** One application instance shared by the parity tests, wired to {@link ParityTestEnvironment}. */
public class ParityTestProfile implements QuarkusTestProfile {

  @Override
  public List<TestResourceEntry> testResources() {
    return List.of(new TestResourceEntry(ParityTestEnvironment.class));
  }
}
