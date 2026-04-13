package it.pagopa.selfcare.onboarding.client.model;

public record BinaryData(
    String fileName,
    byte[] content
) {
}
