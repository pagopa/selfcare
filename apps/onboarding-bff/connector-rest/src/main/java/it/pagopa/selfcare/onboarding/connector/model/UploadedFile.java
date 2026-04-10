package it.pagopa.selfcare.onboarding.connector.model;

public record UploadedFile(
    String fileName,
    String contentType,
    byte[] content
) {
}
