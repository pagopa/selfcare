package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import java.io.File;
import java.util.Arrays;
import java.util.Deque;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.FormValue;

public class Utils {
    private static final String DEFAULT_CONTRACT_FORM_DATA_NAME = "contract";
    private static final String DEFAULT_ATTACHMENT_FORM_DATA_NAME = "file";

    private Utils() {
    }

    public static String extractSurnamePart(String surname) {
        String consonants = surname.replaceAll("[AEIOUaeiou]", "");
        String vowels = surname.replaceAll("[^AEIOUaeiou]", "");
        String part = (consonants + vowels).toUpperCase();
        return part.length() >= 3 ? part.substring(0, 3) : padWithX(part);
    }

    public static FormItem retrieveContractFromFormData(FormData formData, File file) {
        Deque<FormValue> deck = formData.get(DEFAULT_CONTRACT_FORM_DATA_NAME);
        if (deck.size() > 1) {
            throw new InvalidRequestException(
                    CustomError.TOO_MANY_CONTRACTS.getMessage(), CustomError.TOO_MANY_CONTRACTS.getCode());
        }

        return FormItem.builder().file(file).fileName(deck.getFirst().getFileName()).build();
    }

    public static FormItem retrieveAttachmentFromFormData(FormData formData, File file) {
        Deque<FormValue> deck = formData.get(DEFAULT_ATTACHMENT_FORM_DATA_NAME);
        if (deck.size() > 1) {
            throw new InvalidRequestException(
                    CustomError.TOO_MANY_CONTRACTS.getMessage(), CustomError.TOO_MANY_CONTRACTS.getCode());
        }

        return FormItem.builder().file(file).fileName(deck.getFirst().getFileName()).build();
    }

    public static String extractNamePart(String name) {
        String consonants = name.replaceAll("[AEIOUaeiou]", "");
        String vowels = name.replaceAll("[^AEIOUaeiou]", "");
        if (consonants.length() > 3) {
            consonants = String.format("%s%s%s", consonants.charAt(0), consonants.charAt(2), consonants.charAt(3));
        }
        String part = (consonants + vowels).toUpperCase();
        return part.length() >= 3 ? part.substring(0, 3) : padWithX(part);
    }

    private static String padWithX(String input) {
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < 3) {
            sb.append("X");
        }
        return sb.toString();
    }

    public static String getFileExtension(String name) {
        String[] parts = name.split("\\.");
        String ext = "";

        if (parts.length == 2) {
            return parts[1];
        }

        if (parts.length > 2) {
            // join all parts except the first one
            ext = String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
        }

        return ext;
    }

    public static String replaceFileExtension(String originalFilename, String newExtension) {
        int lastIndexOf = originalFilename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return originalFilename + newExtension;
        } else {
            return originalFilename.substring(0, lastIndexOf) + "." + newExtension;
        }
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

}
