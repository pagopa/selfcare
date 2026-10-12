package it.pagopa.selfcare.onboarding.runtime;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.List;
import java.util.Map;

/** Starts {@link TransportTestEnvironment} (every downstream is a {@link RawTransportStub}). */
public class TransportTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(RuntimeProbeResource.ENABLED_PROPERTY, "true");
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(TransportTestEnvironment.class));
    }
}
