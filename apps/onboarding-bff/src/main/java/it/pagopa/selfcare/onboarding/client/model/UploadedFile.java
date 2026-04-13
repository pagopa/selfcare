package it.pagopa.selfcare.onboarding.client.model;

public record UploadedFile(
    String fileName,
    String contentType,
    byte[] content
) {
}
