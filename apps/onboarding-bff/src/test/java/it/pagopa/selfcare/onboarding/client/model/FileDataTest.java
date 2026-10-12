package it.pagopa.selfcare.onboarding.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDataTest {

    @Test
    void binaryDataUsesValueEquality() {
        BinaryData data = new BinaryData("file.pdf", new byte[] {1, 2});
        BinaryData equal = new BinaryData("file.pdf", new byte[] {1, 2});
        assertEquals(data, data);
        assertEquals(data, equal);
        assertEquals(equal, data);
        assertEquals(data.hashCode(), equal.hashCode());
        assertNotEquals(data, null);
        assertNotEquals(data, "file.pdf");
        assertNotEquals(data, new BinaryData("other.pdf", new byte[] {1, 2}));
        assertNotEquals(data, new BinaryData("file.pdf", new byte[] {1, 3}));
        assertNotEquals(data, new BinaryData("file.pdf", null));
    }

    @Test
    void uploadedFileUsesValueEqualityIncludingContentType() {
        // given
        UploadedFile file = new UploadedFile("file.pdf", "application/pdf", new byte[] {1, 2});
        UploadedFile equal = new UploadedFile("file.pdf", "application/pdf", new byte[] {1, 2});

        // when / then
        assertEquals(file, file);
        assertEquals(file, equal);
        assertEquals(equal, file);
        assertEquals(file.hashCode(), equal.hashCode());
        assertNotEquals(file, null);
        assertNotEquals(file, new BinaryData("file.pdf", new byte[] {1, 2}));
        assertNotEquals(file, new UploadedFile("other.pdf", "application/pdf", new byte[] {1, 2}));
        assertNotEquals(file, new UploadedFile("file.pdf", "application/octet-stream", new byte[] {1, 2}));
        assertNotEquals(file, new UploadedFile("file.pdf", "application/pdf", new byte[] {1, 3}));
    }

    @Test
    void absentFieldsRemainSupportedAndDistinctFromEmptyContent() {
        BinaryData data = new BinaryData(null, null);
        assertEquals(data, new BinaryData(null, null));
        assertEquals(data.hashCode(), new BinaryData(null, null).hashCode());
        assertNotEquals(data, new BinaryData(null, new byte[0]));
        UploadedFile file = new UploadedFile(null, null, null);
        assertEquals(file, new UploadedFile(null, null, null));
        assertEquals(file.hashCode(), new UploadedFile(null, null, null).hashCode());
        assertNotEquals(file, new UploadedFile(null, null, new byte[0]));
    }

    @Test
    void diagnosticStringsDescribeLengthWithoutDumpingDocumentBytes() {
        assertEquals("BinaryData[fileName=file.pdf, contentLength=2]",
                new BinaryData("file.pdf", new byte[] {1, 2}).toString());
        assertEquals("BinaryData[fileName=null, contentLength=null]", new BinaryData(null, null).toString());
        assertEquals("UploadedFile[fileName=file.pdf, contentType=application/pdf, contentLength=2]",
                new UploadedFile("file.pdf", "application/pdf", new byte[] {1, 2}).toString());
        assertEquals("UploadedFile[fileName=null, contentType=null, contentLength=null]",
                new UploadedFile(null, null, null).toString());
    }

    @Test
    void accessorsStillReturnTheOriginalPayload() {
        byte[] bytes = {1, 2};
        assertSame(bytes, new BinaryData("file.pdf", bytes).content());
        assertSame(bytes, new UploadedFile("file.pdf", "application/pdf", bytes).content());
    }

    @Test
    void readLoadsTheTemporaryFileTogetherWithTheMetadata(@TempDir Path directory) throws IOException {
        // given
        Path path = directory.resolve("upload.tmp");
        Files.write(path, new byte[] {1, 2});

        // when
        UploadedFile file = UploadedFile.read("file.pdf", "application/pdf", path);

        // then
        assertEquals(new UploadedFile("file.pdf", "application/pdf", new byte[] {1, 2}), file);
    }

    @Test
    void readOfAnUnreadableFileFailsWithTheIoCause(@TempDir Path directory) {
        // given a location that does not exist
        Path missing = directory.resolve("missing.tmp");

        // when
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> UploadedFile.read("file.pdf", "application/pdf", missing));

        // then
        assertEquals("Cannot read uploaded file", error.getMessage());
        assertInstanceOf(IOException.class, error.getCause());
    }
}
