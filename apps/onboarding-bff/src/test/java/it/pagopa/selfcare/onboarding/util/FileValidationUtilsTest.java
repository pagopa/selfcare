package it.pagopa.selfcare.onboarding.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class FileValidationUtilsTest {

    private static UploadedFile uploaded(String fileName, String contentType, int length) {
        return new UploadedFile(fileName, contentType, new byte[length]);
    }

    @Test
    void validateFile_missingContent_throwsInvalidRequestException() {
        // given
        UploadedFile file = new UploadedFile("contract.pdf", "application/pdf", null);

        // when
        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> FileValidationUtils.validatePdfOrP7m(file));

        // then
        assertEquals("Il file è vuoto o mancante.", error.getMessage());
    }

    @Test
    void validateAggregatesFile_csvFile_noException() {
        // given
        UploadedFile file = uploaded("aggregates.csv", "text/csv", 3);

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validateAggregatesFile(file));
    }

    @Test
    void validateAggregatesFile_xlsxFile_noException() {
        // given
        UploadedFile file = uploaded(
                "aggregates.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                3);

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
        UploadedFile file = uploaded("aggregates.csv", "text/csv", 0);

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validateAggregatesFile(file));
    }

    @Test
    void validateAggregatesFile_unsupportedExtension_throwsInvalidRequestException() {
        // given
        UploadedFile file = uploaded("document.pdf", "application/pdf", 3);

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validateAggregatesFile(file));
    }

    @Test
    void validatePdfFile_pdfByContentType_noException() {
        // given
        UploadedFile file = uploaded("contract.pdf", "application/pdf", 3);

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfFile(file));
    }

    @Test
    void validatePdfFile_pdfByExtension_noException() {
        // given
        UploadedFile file = uploaded("contract.pdf", "application/octet-stream", 3);

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfFile(file));
    }

    @Test
    void validatePdfFile_nonPdfFile_throwsInvalidRequestException() {
        // given
        UploadedFile file = uploaded("aggregates.csv", "text/csv", 3);

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validatePdfFile(file));
    }

    @Test
    void validateP7mFile_p7mByContentType_noException() {
        // given
        UploadedFile file = uploaded("contract.p7m", "application/pkcs7-mime", 3);

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validateP7mFile(file));
    }

    @Test
    void validateP7mFile_p7mByExtension_noException() {
        // given
        UploadedFile file = uploaded("contract.p7m", "application/octet-stream", 3);

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validateP7mFile(file));
    }

    @Test
    void validateP7mFile_nonP7mFile_throwsInvalidRequestException() {
        // given
        UploadedFile file = uploaded("document.pdf", "application/pdf", 3);

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validateP7mFile(file));
    }

    @Test
    void validatePdfOrP7m_pdfFile_noException() {
        // given
        UploadedFile file = uploaded("contract.pdf", "application/pdf", 3);

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validatePdfOrP7m_p7mFile_noException() {
        // given
        UploadedFile file = uploaded("contract.p7m", "application/pkcs7-mime", 3);

        // when / then
        assertDoesNotThrow(() -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validatePdfOrP7m_csvFile_throwsInvalidRequestException() {
        // given
        UploadedFile file = uploaded("file.csv", "text/csv", 3);

        // when / then
        assertThrows(InvalidRequestException.class, () -> FileValidationUtils.validatePdfOrP7m(file));
    }

    @Test
    void validatePdfOrP7m_emptyFileWithUnsupportedFormat_reportsTheEmptyFileFirst() {
        // given
        UploadedFile file = uploaded("file.csv", "text/csv", 0);

        // when
        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> FileValidationUtils.validatePdfOrP7m(file));

        // then
        assertEquals("Il file è vuoto o mancante.", error.getMessage());
    }

    @Test
    void validatePdfOrP7m_nonEmptyFileWithUnsupportedFormat_reportsTheFormat() {
        // given
        UploadedFile file = uploaded("file.csv", "text/csv", 3);

        // when
        InvalidRequestException error = assertThrows(InvalidRequestException.class,
                () -> FileValidationUtils.validatePdfOrP7m(file));

        // then
        assertEquals("Formato file non supportato. Ammessi: [.pdf, .p7m]", error.getMessage());
    }
}
