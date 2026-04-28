package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;

import java.util.List;
import java.util.stream.Stream;

public class FileValidationUtils {
    private static final List<String> ALLOWED_EXTENSIONS_AGGREGATES = List.of(".csv", ".xls", ".xlsx");
    private static final List<String> ALLOWED_MIME_TYPES_AGGREGATES = List.of(
            "text/csv",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final List<String> ALLOWED_EXTENSIONS_PDF = List.of(".pdf");
    private static final List<String> ALLOWED_MIME_TYPES_PDF = List.of("application/pdf");

    private static final List<String> ALLOWED_EXTENSIONS_P7M = List.of(".p7m");
    private static final List<String> ALLOWED_MIME_TYPES_P7M = List.of(
            "application/pkcs7-mime",
            "application/x-pkcs7-mime"
    );


    private FileValidationUtils() {
    }

    public static void validateAggregatesFile(UploadedFile file) {
        validateFile(file, ALLOWED_EXTENSIONS_AGGREGATES, ALLOWED_MIME_TYPES_AGGREGATES);
    }

    public static void validatePdfFile(UploadedFile file) {
        validateFile(file, ALLOWED_EXTENSIONS_PDF, ALLOWED_MIME_TYPES_PDF);
    }

    public static void validateP7mFile(UploadedFile file) {
        validateFile(file, ALLOWED_EXTENSIONS_P7M, ALLOWED_MIME_TYPES_P7M);
    }

    public static void validatePdfOrP7m(UploadedFile file) {
        List<String> extensions = Stream.concat(ALLOWED_EXTENSIONS_PDF.stream(), ALLOWED_EXTENSIONS_P7M.stream())
                .toList();
        List<String> mimeTypes = Stream.concat(ALLOWED_MIME_TYPES_PDF.stream(), ALLOWED_MIME_TYPES_P7M.stream())
                .toList();

        validateFile(file, extensions, mimeTypes);
    }

    public static void validateFile(UploadedFile file, List<String> allowedExtensions, List<String> allowedMimeTypes) {
        if (file == null || file.content() == null || file.content().length == 0) {
            throw new InvalidRequestException("Il file è vuoto o mancante.");
        }

        final String filename = file.fileName();
        final String contentType = file.contentType();

        boolean isValid = allowedMimeTypes.contains(contentType) ||
                allowedExtensions.stream().anyMatch(ext -> filename != null && filename.toLowerCase().endsWith(ext));

        if (!isValid) {
            throw new InvalidRequestException("Formato file non supportato. Ammessi: " + allowedExtensions);
        }
    }
}
