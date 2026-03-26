package it.pagopa.selfcare.document.util;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DocumentFileUtilsTest {

    @TempDir
    Path sharedTempDir; // JUnit 5 gestisce in automatico la pulizia di questa cartella!

    @Test
    void isP7MFile_shouldReturnTrue_whenExtensionIsP7m() {
        assertTrue(DocumentFileUtils.isP7MFile("contract.p7m"));
        assertTrue(DocumentFileUtils.isP7MFile("contract.P7M")); // case insensitive
    }

    @Test
    void isP7MFile_shouldReturnFalse_whenExtensionIsPdfOrNull() {
        assertFalse(DocumentFileUtils.isP7MFile("contract.pdf"));
        assertFalse(DocumentFileUtils.isP7MFile(null));
    }

    @Test
    void isPdfFile_shouldReturnTrue_whenExtensionIsPdf() {
        assertTrue(DocumentFileUtils.isPdfFile("document.pdf"));
        assertTrue(DocumentFileUtils.isPdfFile("DOCUMENT.PDF"));
    }

    @Test
    void isPdfFile_shouldReturnFalse_whenExtensionIsNotPdfOrNull() {
        assertFalse(DocumentFileUtils.isPdfFile("document.txt"));
        assertFalse(DocumentFileUtils.isPdfFile(null));
    }

    @Test
    void validateUploadedFile_shouldPass_whenFileIsValidAndInTempDir() throws IOException {
        // Arrange
        // createTempFile usa di default la cartella temporanea di sistema (java.io.tmpdir)
        File validTempFile = Files.createTempFile("test-upload", ".pdf").toFile();

        // Act & Assert
        assertDoesNotThrow(() -> DocumentFileUtils.validateUploadedFile(validTempFile));

        // Clean up
        validTempFile.delete();
    }

    @Test
    void validateUploadedFile_shouldThrow_whenFileIsNull() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> DocumentFileUtils.validateUploadedFile(null));
        assertEquals("Uploaded file must not be null", ex.getMessage());
    }

    @Test
    void validateUploadedFile_shouldThrow_whenFileDoesNotExist() {
        File phantomFile = new File(System.getProperty("java.io.tmpdir"), "phantom.pdf");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> DocumentFileUtils.validateUploadedFile(phantomFile));
        assertEquals("Uploaded file does not exist or is not a regular file", ex.getMessage());
    }

    @Test
    void buildAndValidateContractFilePath_shouldReturnRelativePath() {
        String result = DocumentFileUtils.buildAndValidateContractFilePath("my-contract.pdf", "/base/path/", false);
        assertEquals("/base/path/my-contract.pdf", result);
    }

    @Test
    void buildAndValidateContractFilePath_shouldReturnAbsolutePath() {
        String result = DocumentFileUtils.buildAndValidateContractFilePath("absolute-contract.pdf", "/base/path/", true);
        assertEquals("absolute-contract.pdf", result); // Non aggiunge il basePath
    }

    @Test
    void buildAndValidateContractFilePath_shouldThrow_onPathTraversal() {
        assertThrows(InvalidRequestException.class,
                () -> DocumentFileUtils.buildAndValidateContractFilePath("../secret.txt", "/base/", false));

        assertThrows(InvalidRequestException.class,
                () -> DocumentFileUtils.buildAndValidateContractFilePath("/etc/passwd", "/base/", false));

        assertThrows(InvalidRequestException.class,
                () -> DocumentFileUtils.buildAndValidateContractFilePath("cartella\\file.pdf", "/base/", false));
    }

    @Test
    void isPdfValid_shouldPass_forValidPdf() throws IOException {
        // Arrange: creiamo un VERO pdf vuoto con una pagina tramite PDFBox
        File validPdf = sharedTempDir.resolve("valid.pdf").toFile();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(validPdf);
        }

        // Act & Assert
        assertDoesNotThrow(() -> DocumentFileUtils.isPdfValid(validPdf));
    }

    @Test
    void isPdfValid_shouldThrow_forInvalidOrEmptyPdf() throws IOException {
        // Arrange: file di testo camuffato da PDF
        File invalidPdf = sharedTempDir.resolve("invalid.pdf").toFile();
        Files.writeString(invalidPdf.toPath(), "Questo non è un PDF");

        // Act & Assert
        assertThrows(InvalidRequestException.class, () -> DocumentFileUtils.isPdfValid(invalidPdf));
    }
}