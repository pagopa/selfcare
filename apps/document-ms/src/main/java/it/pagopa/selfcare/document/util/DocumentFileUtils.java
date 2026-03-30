package it.pagopa.selfcare.document.util;

import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.entity.Document;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.document.util.ErrorMessage.ORIGINAL_DOCUMENT_NOT_FOUND;
import static it.pagopa.selfcare.document.util.Utils.CONTRACT_FILENAME_FUNC;

/**
 * Utility class for document and file validations.
 */
public final class DocumentFileUtils {

    private DocumentFileUtils() {
    }

    /**
     * Validate that the uploaded file is a regular file located under the system temporary directory.
     * Prevents Path Traversal attacks.
     */
    public static void validateUploadedFile(File file) {
        if (file == null) {
            throw new InvalidRequestException("Uploaded file must not be null", "0000");
        }

        Path filePath = file.toPath().toAbsolutePath().normalize();
        Path tempDirPath = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();

        if (!filePath.startsWith(tempDirPath)) {
            throw new InvalidRequestException("Invalid uploaded file location", "0000");
        }

        if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new InvalidRequestException("Uploaded file does not exist or is not a regular file", "0000");
        }
    }

    /**
     * Checks if the given file name has a .p7m extension.
     */
    public static boolean isP7MFile(String fileName) {
        return Optional.ofNullable(fileName)
                .map(name -> name.toLowerCase(Locale.ROOT).endsWith(".p7m"))
                .orElse(false);
    }

    /**
     * Checks if the given path has a .pdf extension.
     */
    public static boolean isPdfFile(String path) {
        return path != null && path.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    /**
     * Validates that the provided file is a valid PDF with at least one page.
     */
    public static void isPdfValid(File contract) {
        try (PDDocument document = Loader.loadPDF(contract)) {
            if (document.getNumberOfPages() == 0) {
                throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
            }
        } catch (IOException e) {
            throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
        }
    }

    /**
     * Safely builds and validates the contract file path starting from user-provided input.
     * Rejects path traversal attempts.
     */
    public static String buildAndValidateContractFilePath(String fileName, String basePath, boolean absolutePath) {
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidRequestException("Invalid fileName");
        }

        String trimmed = fileName.trim();

        if (trimmed.contains("..") || trimmed.contains("\\") || trimmed.startsWith("/")) {
            throw new InvalidRequestException("Invalid fileName");
        }

        if (absolutePath) {
            return trimmed;
        } else {
            String fullPath = basePath + trimmed;
            if (!fullPath.startsWith(basePath)) {
                throw new InvalidRequestException("Invalid fileName");
            }
            return fullPath;
        }
    }

    /**
     * Estrae l'estensione gestendo file con estensioni multiple (es. "file.pdf.p7m" -> "pdf.p7m")
     * Logica migrata da Onboarding MS per retrocompatibilità.
     */
    public static String getFileExtension(String name) {
        if (name == null || name.isBlank() || !name.contains(".")) {
            return "";
        }

        String[] parts = name.split("\\.");
        String ext = "";

        if (parts.length == 2) {
            return parts[1];
        }

        if (parts.length > 2) {
            // Join all parts except the first one
            ext = String.join(".", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        }

        return ext;
    }

    /**
     * Sostituisce l'estensione partendo dall'ultimo punto.
     * Logica migrata da Onboarding MS per retrocompatibilità.
     */
    public static String replaceFileExtension(String baseName, String newExtension) {
        if (baseName == null) {
            return newExtension;
        }

        if (newExtension == null || newExtension.isBlank()) {
            return baseName;
        }

        int lastIndexOf = baseName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return baseName + "." + newExtension;
        } else {
            return baseName.substring(0, lastIndexOf) + "." + newExtension;
        }
    }

    public static String buildContractStoragePath(String onboardingId, String contractPath) {
        return String.format("%s%s", contractPath, onboardingId);
    }

    public static String buildAttachmentStoragePath(String onboardingId, String contractPath) {
        return String.format("%s%s/attachments", contractPath, onboardingId);
    }

    public static String getContractNotSigned(String onboardingId, String contractPath, String contractFilename ) {
        return String.format("%s%s/%s", contractPath, onboardingId,
                contractFilename);
    }

    public static String buildAttachmentPath(Document document, String contractPath) {
        return Objects.nonNull(document.getContractSigned()) ? document.getContractSigned() : getAttachmentByOnboarding(document.getOnboardingId(), contractPath, document.getContractFilename());
    }

    public static String getAttachmentByOnboarding(String onboardingId, String contractPath, String filename) {
        return String.format("%s%s%s%s", contractPath, onboardingId, "/attachments", "/" + filename);
    }

    public static String buildFilename(String format, String productName, String attachmentName) {
        if (Objects.nonNull(attachmentName)) {
            return CONTRACT_FILENAME_FUNC.apply(String.format("%s_%s.pdf", format, attachmentName), productName);
        }
        return CONTRACT_FILENAME_FUNC.apply(format, productName);
    }
}