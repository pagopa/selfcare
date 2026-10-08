package it.pagopa.selfcare.onboarding.client.model;

import java.util.Arrays;
import java.util.Objects;

public record UploadedFile(
    String fileName,
    String contentType,
    byte[] content
) {

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof UploadedFile file
                && Objects.equals(fileName, file.fileName) && Objects.equals(contentType, file.contentType)
                && Arrays.equals(content, file.content);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(fileName, contentType) + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "UploadedFile[fileName=" + fileName + ", contentType=" + contentType + ", contentLength="
                + (content == null ? "null" : content.length) + "]";
    }
}
