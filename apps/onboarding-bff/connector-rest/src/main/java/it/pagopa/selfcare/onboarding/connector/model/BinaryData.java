package it.pagopa.selfcare.onboarding.connector.model;

public record BinaryData(
    String fileName,
    byte[] content
) {
}
