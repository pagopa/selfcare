package it.pagopa.selfcare.onboarding.client.model;

import java.util.Arrays;
import java.util.Objects;

public record BinaryData(
    String fileName,
    byte[] content
) {

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BinaryData data
                && Objects.equals(fileName, data.fileName) && Arrays.equals(content, data.content);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(fileName) + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "BinaryData[fileName=" + fileName + ", contentLength="
                + (content == null ? "null" : content.length) + "]";
    }
}
