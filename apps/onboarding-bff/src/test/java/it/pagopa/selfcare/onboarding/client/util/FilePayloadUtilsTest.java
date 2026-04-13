package it.pagopa.selfcare.onboarding.client.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class FilePayloadUtilsTest {

    @Test
    void toBinaryDataUsesFallbackName() throws Exception {
        // given
        File file = Files.createTempFile("binary-", ".txt").toFile();
        file.deleteOnExit();
        Files.writeString(file.toPath(), "test-content");

        // when
        BinaryData data = FilePayloadUtils.toBinaryData(file, "fallback-name.txt");

        // then
        assertEquals("fallback-name.txt", data.fileName());
        assertArrayEquals("test-content".getBytes(), data.content());
    }

    @Test
    void toBinaryDataUsesOriginalFileNameWhenFallbackIsBlank() throws Exception {
        // given
        File file = Files.createTempFile("binary-", ".txt").toFile();
        file.deleteOnExit();
        Files.writeString(file.toPath(), "abc");

        // when
        BinaryData data = FilePayloadUtils.toBinaryData(file, " ");

        // then
        assertEquals(file.getName(), data.fileName());
        assertArrayEquals("abc".getBytes(), data.content());
    }

    @Test
    void toTempFileSanitizesNameAndPreservesContent() throws Exception {
        // given
        byte[] content = "payload".getBytes();
        UploadedFile uploadedFile = new UploadedFile("report 1@.pdf", "application/pdf", content);

        // when
        File tempFile = FilePayloadUtils.toTempFile(uploadedFile, "upload-", ".bin");
        tempFile.deleteOnExit();

        // then
        assertTrue(tempFile.getName().contains("report_1_.pdf"));
        assertArrayEquals(content, Files.readAllBytes(tempFile.toPath()));
    }

    @Test
    void toTempFileUsesDefaultExtensionWhenNameIsMissing() {
        // given
        UploadedFile uploadedFile = new UploadedFile(" ", "application/octet-stream", new byte[] {1, 2, 3});

        // when
        File tempFile = FilePayloadUtils.toTempFile(uploadedFile, "upload-", ".bin");
        tempFile.deleteOnExit();

        // then
        assertTrue(tempFile.getName().endsWith(".bin"));
    }
}
