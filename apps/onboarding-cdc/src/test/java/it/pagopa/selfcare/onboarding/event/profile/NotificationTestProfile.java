package it.pagopa.selfcare.onboarding.event.profile;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class NotificationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("onboarding-cdc.mongodb.watch.enabled", "false");
    }
}
