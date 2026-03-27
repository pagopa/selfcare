package it.pagopa.selfcare.onboarding.web.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

class FileValidationUtilsTest {

    private MultipartFile mockFile(String filename, String contentType, boolean empty) {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn(contentType);
        when(file.isEmpty()).thenReturn(empty);
        return file;
    }

    @Test
    void validateAggregatesFileValidCsv() {
        MultipartFile file = mockFile("test.csv", "text/csv", false);
        assertDoesNotThrow(() -> FileValidationUtils.validateAggregatesFile(file));
    }

    @Test
    void validateAggregatesFileError() {
        MultipartFile file = mockFile("test.txt", "text/plain", false);
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> FileValidationUtils.validateAggregatesFile(file));
        assertTrue(ex.getMessage().contains("Formato file non supportato"));
    }

    @Test
    void validatePdfFileSuccess() {
        MultipartFile file = mockFile("doc.pdf", "application/pdf", false);
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfFile(file));
    }

    @Test
    void validatePdfFileError() {
        MultipartFile file = mockFile("doc.csv", "text/csv", false);
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validatePdfFile(file));
    }

    @Test
    void validateP7mFileSuccess() {
        MultipartFile file = mockFile("signed.p7m", "application/pkcs7-mime", false);
        assertDoesNotThrow(() -> FileValidationUtils.validateP7mFile(file));
    }

    @Test
    void validateP7mFileError() {
        MultipartFile file = mockFile("file.pdf", "application/pdf", false);
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validateP7mFile(file));
    }

    @Test
    void validatePdfOrP7mSuccess() {
        MultipartFile file = mockFile("contract.pdf", "application/pdf", false);
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validateP7mSuccess() {
        MultipartFile file = mockFile("signed.p7m", "application/x-pkcs7-mime", false);
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validatePdfOrP7Error() {
        MultipartFile file = mockFile("file.txt", "text/plain", false);
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validateFileNullFile() {
        final List<String> extensions = List.of(".pdf");
        final List<String> mimeTypes = List.of("application/pdf");

        assertThrows(InvalidRequestException.class,
                () -> FileValidationUtils.validateFile(null, extensions, mimeTypes));
    }

    @Test
    void validateFileEmptyFile() {
        MultipartFile file = mockFile("empty.pdf", "application/pdf", true);
        assertThrows(InvalidRequestException.class,
                () -> FileValidationUtils.validateFile(file, List.of(".pdf"), List.of("application/pdf")));
    }

    @Test
    void validateFileSuccessExtension() {
        MultipartFile file = mockFile("data.xlsx", "unknown/type", false);
        assertDoesNotThrow(() ->
                FileValidationUtils.validateFile(file, List.of(".xls", ".xlsx"), List.of("text/csv")));
    }

    @Test
    void validateFileSuccessMime() {
        MultipartFile file = mockFile("data.unknown", "text/csv", false);
        assertDoesNotThrow(() ->
                FileValidationUtils.validateFile(file, List.of(".xls"), List.of("text/csv")));
    }

    @Test
    void validateFileErrorMime() {
        MultipartFile file = mockFile("data.txt", "text/plain", false);
        assertThrows(InvalidRequestException.class, () ->
                FileValidationUtils.validateFile(file, List.of(".pdf"), List.of("application/pdf")));
    }
}
