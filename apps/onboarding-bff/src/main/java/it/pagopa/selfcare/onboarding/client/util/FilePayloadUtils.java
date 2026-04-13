package it.pagopa.selfcare.onboarding.client.util;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@UtilityClass
public class FilePayloadUtils {

    public static BinaryData toBinaryData(File file, String fallbackName) {
        try {
            String fileName = fallbackName == null || fallbackName.isBlank() ? file.getName() : fallbackName;
            return new BinaryData(fileName, Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read downloaded file", e);
        }
    }

    public static File toTempFile(UploadedFile uploadedFile, String prefix, String defaultExtension) {
        try {
            String fileName = uploadedFile.fileName();
            String sanitizedFileName = sanitizeFileName(fileName);
            String suffix = (sanitizedFileName == null || sanitizedFileName.isBlank())
                    ? defaultExtension
                    : "-" + sanitizedFileName;
            File file = Files.createTempFile(prefix, suffix).toFile();
            Files.write(file.toPath(), uploadedFile.content());
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return fileName;
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
