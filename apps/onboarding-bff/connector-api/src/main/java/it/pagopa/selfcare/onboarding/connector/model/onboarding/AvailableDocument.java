package it.pagopa.selfcare.onboarding.connector.model.onboarding;

import lombok.Data;

@Data
public class AvailableDocument {

    private String name;
    private String type;
    private String onboardingId;
    private String rootOnboardingId;
}

