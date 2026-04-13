package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.connector.model.UploadedFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openapi.quarkus.onboarding_json.api.InternalV1Api;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingInternalApiClient extends InternalV1Api {

    default void _completeOnboardingUsingPUT(String onboardingId, UploadedFile contract) {
        InternalV1Api.CompleteOnboardingUsingPUTMultipartForm form =
            new InternalV1Api.CompleteOnboardingUsingPUTMultipartForm();
        form.contract = toTempFile(contract);
        completeOnboardingUsingPUT(form, onboardingId).await().indefinitely();
    }

    private static File toTempFile(UploadedFile uploadedFile) {
        try {
            String fileName = uploadedFile.fileName();
            String suffix = fileName == null ? ".bin" : "-" + fileName;
            File file = Files.createTempFile("internal-", suffix).toFile();
            Files.write(file.toPath(), uploadedFile.content());
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }
}
