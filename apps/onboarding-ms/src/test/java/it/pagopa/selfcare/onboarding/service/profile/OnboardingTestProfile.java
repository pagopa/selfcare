package it.pagopa.selfcare.onboarding.service.profile;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class OnboardingTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("onboarding-ms.signature.verify-enabled", "true",
                "onboarding-ms.istat-cache-duration-days","30");
    }
}
