package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.connector.model.BinaryData;
import it.pagopa.selfcare.onboarding.connector.model.UploadedFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openapi.quarkus.onboarding_json.api.TokenControllerApi;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingTokenApiClient extends TokenControllerApi {

    default BinaryData _getContract(String onboardingId) {
        File file = getContract(onboardingId).await().indefinitely();
        return toBinaryData(file, file.getName());
    }

    default BinaryData _getTemplateAttachment(String onboardingId, String name) {
        File file = getTemplateAttachment(onboardingId, name).await().indefinitely();
        return toBinaryData(file, name);
    }

    default BinaryData _getAttachment(String onboardingId, String name) {
        File file = getAttachment(onboardingId, name).await().indefinitely();
        return toBinaryData(file, name);
    }

    default void _uploadAttachment(String onboardingId, String name, UploadedFile file) {
        TokenControllerApi.UploadAttachmentMultipartForm form = new TokenControllerApi.UploadAttachmentMultipartForm();
        form._file = toTempFile(file);
        uploadAttachment(form, onboardingId, name).await().indefinitely();
    }

    default int _headAttachment(String onboardingId, String name) {
        jakarta.ws.rs.core.Response response = headAttachment(onboardingId, name).await().indefinitely();
        return response.getStatus();
    }

    private static BinaryData toBinaryData(File file, String fallbackName) {
        try {
            String fileName = fallbackName == null || fallbackName.isBlank() ? file.getName() : fallbackName;
            return new BinaryData(fileName, Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read downloaded file", e);
        }
    }

    private static File toTempFile(UploadedFile uploadedFile) {
        try {
            String fileName = uploadedFile.fileName();
            String suffix = fileName == null ? ".bin" : "-" + fileName;
            File file = Files.createTempFile("token-", suffix).toFile();
            Files.write(file.toPath(), uploadedFile.content());
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }
}
