package it.pagopa.selfcare.onboarding.service.profile;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class CheckOrganizationByPassTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("external.by-pass-check-organization", "true");
    }

}
