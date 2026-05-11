package it.pagopa.selfcare.external_api.strategy;

public interface OnboardingValidationStrategy {

    boolean validate(String productId, String institutionExternalId);

}
