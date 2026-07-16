package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class FileValidationUtilsTest {

    @Test
    void validateAggregatesFile_csvFile_noException() {
        // given
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", new byte[]{1, 2, 3});

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validateAggregatesFile(file));
    }

    @Test
    void validateAggregatesFile_xlsxFile_noException() {
        // given
        UploadedFile file = new UploadedFile(
                "aggregates.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validateAggregatesFile(file));
    }

    @Test
    void validateAggregatesFile_nullFile_throwsInvalidRequestException() {
        // given / when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validateAggregatesFile(null));
    }

    @Test
    void validateAggregatesFile_emptyContent_throwsInvalidRequestException() {
        // given
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", new byte[0]);

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validateAggregatesFile(file));
    }

    @Test
    void validateAggregatesFile_unsupportedExtension_throwsInvalidRequestException() {
        // given
        UploadedFile file = new UploadedFile("document.pdf", "application/pdf", new byte[]{1, 2, 3});

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validateAggregatesFile(file));
    }

    @Test
    void validatePdfFile_pdfByContentType_noException() {
        // given
        UploadedFile file = new UploadedFile("contract.pdf", "application/pdf", new byte[]{1, 2, 3});

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfFile(file));
    }

    @Test
    void validatePdfFile_pdfByExtension_noException() {
        // given
        UploadedFile file = new UploadedFile("contract.pdf", "application/octet-stream", new byte[]{1, 2, 3});

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfFile(file));
    }

    @Test
    void validatePdfFile_nonPdfFile_throwsInvalidRequestException() {
        // given
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", new byte[]{1, 2, 3});

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validatePdfFile(file));
    }

    @Test
    void validateP7mFile_p7mByContentType_noException() {
        // given
        UploadedFile file = new UploadedFile("contract.p7m", "application/pkcs7-mime", new byte[]{1, 2, 3});

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validateP7mFile(file));
    }

    @Test
    void validateP7mFile_p7mByExtension_noException() {
        // given
        UploadedFile file = new UploadedFile("contract.p7m", "application/octet-stream", new byte[]{1, 2, 3});

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validateP7mFile(file));
    }

    @Test
    void validateP7mFile_nonP7mFile_throwsInvalidRequestException() {
        // given
        UploadedFile file = new UploadedFile("document.pdf", "application/pdf", new byte[]{1, 2, 3});

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validateP7mFile(file));
    }

    @Test
    void validatePdfOrP7m_pdfFile_noException() {
        // given
        UploadedFile file = new UploadedFile("contract.pdf", "application/pdf", new byte[]{1, 2, 3});

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validatePdfOrP7m_p7mFile_noException() {
        // given
        UploadedFile file = new UploadedFile("contract.p7m", "application/pkcs7-mime", new byte[]{1, 2, 3});

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validatePdfOrP7m_csvFile_throwsInvalidRequestException() {
        // given
        UploadedFile file = new UploadedFile("file.csv", "text/csv", new byte[]{1, 2, 3});

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validateFile_nullContent_throwsInvalidRequestException() {
        // given
        UploadedFile file = new UploadedFile("file.csv", "text/csv", null);

        // when / then
        assertThrows(InvalidRequestException.class,
                () -> FileValidationUtils.validateAggregatesFile(file));
    }
}
