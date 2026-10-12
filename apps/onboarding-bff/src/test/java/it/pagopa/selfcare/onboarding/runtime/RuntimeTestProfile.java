package it.pagopa.selfcare.onboarding.runtime;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;
import java.util.Map;

/** Enables the probe endpoints of the runtime tests and starts {@link RuntimeTestEnvironment}. */
public class RuntimeTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(RuntimeProbeResource.ENABLED_PROPERTY, "true");
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(RuntimeTestEnvironment.class));
    }
}
