package it.pagopa.selfcare.onboarding.connector.api;

public interface OnboardingFunctionsConnector {
    void checkOrganization(String fiscalCode, String vatNumber);
}