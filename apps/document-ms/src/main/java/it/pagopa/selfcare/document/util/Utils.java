package it.pagopa.selfcare.document.util;

import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.model.FormItem;
import java.io.File;
import java.util.Deque;
import java.util.UUID;
import java.util.function.BinaryOperator;

import it.pagopa.selfcare.document.model.entity.Document;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;

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
}
