package it.pagopa.selfcare.document.util;

import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.FormItem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.function.BinaryOperator;

import it.pagopa.selfcare.document.model.entity.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;

import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;

@Slf4j
public class Utils {

    private static final String DEFAULT_ATTACHMENT_FORM_DATA_NAME = "file";

    private Utils() {
    }

    public static final BinaryOperator<String> CONTRACT_FILENAME_FUNC =
            (filename, productName) -> {
                String normalizedProductName = StringUtils.stripAccents(productName.replaceAll("\\s+", "_"));
                // Treat filename as plain text, not as a format string, to avoid format string vulnerabilities.
                if (filename != null && filename.contains("%s")) {
                    return filename.replace("%s", normalizedProductName);
                }
                return filename;
            };

    public static FormItem retrieveAttachmentFromFormData(FormData formData, File file) {
        Deque<FormValue> deck = formData.get(DEFAULT_ATTACHMENT_FORM_DATA_NAME);
        if (deck.size() > 1) {
            throw new InvalidRequestException("Too many attachments", "0000");
        }

        return FormItem.builder().file(file).fileName(deck.getFirst().getFileName()).build();
    }

    public static String extractFileName(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0
                ? path.substring(lastSlash + 1)
                : path;
    }

    public static String getCurrentContractName(Document document, boolean isSigned) {
        return isSigned ? getContractSignedName(document) : document.getContractFilename();
    }

    private static String getContractSignedName(Document document) {
        File file = new File(document.getContractSigned());
        return file.getName();
    }

    public static Document createBaseDocument(String onboardingId, String productId,
                                        String contractTemplate, String contractVersion) {
        Document document = new Document();
        document.setId(UUID.randomUUID().toString());
        document.setOnboardingId(onboardingId);
        document.setProductId(productId);
        document.setContractTemplate(contractTemplate);
        document.setContractVersion(contractVersion);
        return document;
    }

    public static Path createSafeTempFile(String prefix, String suffix) throws IOException {
        try {
            return createTempFileWithPosix(prefix, suffix);
        } catch (UnsupportedOperationException e) {
            // Fallback per Windows/Non-POSIX
            File f = Files.createTempFile(prefix, suffix).toFile();
            boolean readable = f.setReadable(true, true);
            boolean writable = f.setWritable(true, true);
            boolean executable = f.setExecutable(false); // Importante: NO esecuzione
            if (!readable || !writable || !executable) {
                log.warn("Could not set restricted permissions on temporary file: {}", sanitize(f.getAbsolutePath()));
            }
            return f.toPath();
        }
    }

    private static Path createTempFileWithPosix(String prefix, String suffix) throws IOException {
        FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions.asFileAttribute(
                        PosixFilePermissions.fromString("rw-------")
                );
        return Files.createTempFile(prefix, suffix, attr);
    }
}
