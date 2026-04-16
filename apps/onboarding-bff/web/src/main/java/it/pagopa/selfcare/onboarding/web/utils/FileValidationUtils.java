package it.pagopa.selfcare.onboarding.web.utils;

import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import org.springframework.web.multipart.MultipartFile;

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

    public static void validateAggregatesFile(MultipartFile file) {
        validateFile(file, ALLOWED_EXTENSIONS_AGGREGATES, ALLOWED_MIME_TYPES_AGGREGATES);
    }

    public static void validatePdfFile(MultipartFile file) {
        validateFile(file, ALLOWED_EXTENSIONS_PDF, ALLOWED_MIME_TYPES_PDF);
    }

    public static void validateP7mFile(MultipartFile file) {
        validateFile(file, ALLOWED_EXTENSIONS_P7M, ALLOWED_MIME_TYPES_P7M);
    }

    public static void validatePdfOrP7m(MultipartFile file) {
        List<String> extensions = Stream.concat(ALLOWED_EXTENSIONS_PDF.stream(), ALLOWED_EXTENSIONS_P7M.stream())
                .toList();
        List<String> mimeTypes = Stream.concat(ALLOWED_MIME_TYPES_PDF.stream(), ALLOWED_MIME_TYPES_P7M.stream())
                .toList();

        validateFile(file, extensions, mimeTypes);
    }

    public static void validateFile(MultipartFile file, List<String> allowedExtensions, List<String> allowedMimeTypes) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Il file è vuoto o mancante.");
        }

        final String filename = file.getOriginalFilename();
        final String contentType = file.getContentType();

        boolean isValid = allowedMimeTypes.contains(contentType) ||
                allowedExtensions.stream().anyMatch(ext -> filename != null && filename.toLowerCase().endsWith(ext));

        if (!isValid) {
            throw new InvalidRequestException("Formato file non supportato. Ammessi: " + allowedExtensions);
        }
    }
}
