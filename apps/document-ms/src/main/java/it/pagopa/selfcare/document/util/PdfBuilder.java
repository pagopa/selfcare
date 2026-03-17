package it.pagopa.selfcare.document.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import it.pagopa.selfcare.document.exception.PdfBuilderException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for generating PDF documents from HTML templates.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PdfBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PDF_CREATION_FAILED_MESSAGE = "Error during PDF creation";

    /**
     * Generates a PDF document from an HTML template with placeholder substitution.
     *
     * @param documentName     Identifier for the document (used in filename)
     * @param documentTemplate HTML template content with placeholders
     * @param content          Map of placeholder values to substitute
     * @return Generated PDF file
     * @throws IOException if file operations fail
     */
    public static File generateDocument(String documentName,
                                         String documentTemplate,
                                         Map<String, Object> content) throws IOException {
        Path temporaryPdfFile = null;
        Path temporaryDirectory = null;
        String nameFile = null;

        try {
            nameFile = DATE_TIME_FORMATTER.format(LocalDateTime.now())
                    + "_" + UUID.randomUUID()
                    + "_" + documentName;

            FileAttribute<Set<PosixFilePermission>> dirAttr =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));

            temporaryDirectory = Files.createTempDirectory("pdfgen-", dirAttr);

            FileAttribute<Set<PosixFilePermission>> fileAttr =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

            temporaryPdfFile = Files.createTempFile(temporaryDirectory, nameFile, ".pdf", fileAttr);

            String htmlContent = StringSubstitutor.replace(documentTemplate, content);

            Document dom = new W3CDom().fromJsoup(Jsoup.parse(htmlContent));

            PdfRendererBuilder builder = buildRenderer(dom);

            try (FileOutputStream out = new FileOutputStream(temporaryPdfFile.toFile())) {
                builder.toStream(out);
                builder.run();
            }

            return temporaryPdfFile.toFile();

        } catch (UnsupportedOperationException e) {
            // Fallback for non-POSIX systems (e.g., Windows)
            return createFallbackTempFile(nameFile);
        } catch (Exception e) {
            log.error("Error while generating PDF", e);
            cleanupTempFiles(temporaryPdfFile, temporaryDirectory);
            throw new PdfBuilderException(PDF_CREATION_FAILED_MESSAGE, "0030");
        }
    }

    private static File createFallbackTempFile(String nameFile) throws IOException {
        File f = Files.createTempFile(nameFile, ".pdf").toFile();

        boolean readable = f.setReadable(true, true);
        boolean writable = f.setWritable(true, true);
        boolean executable = f.setExecutable(false);

        if (!readable || !writable || !executable) {
            log.warn("Could not set restricted permissions on temporary file: {}", f.getAbsolutePath());
        }
        return f;
    }

    private static void cleanupTempFiles(Path pdfFile, Path directory) {
        if (pdfFile != null) {
            try {
                Files.deleteIfExists(pdfFile);
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }
        }
        if (directory != null) {
            try {
                Files.deleteIfExists(directory);
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }
        }
    }

    private static PdfRendererBuilder buildRenderer(Document dom) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useSVGDrawer(new BatikSVGDrawer());
        builder.withW3cDocument(dom, null);
        return builder;
    }
}
