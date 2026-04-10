package it.pagopa.selfcare.onboarding.connector.rest.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openapi.quarkus.onboarding_json.api.InternalV1Api;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.springframework.web.multipart.MultipartFile;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingInternalApiClient extends InternalV1Api {

    default void _completeOnboardingUsingPUT(String onboardingId, MultipartFile contract) {
        InternalV1Api.CompleteOnboardingUsingPUTMultipartForm form =
            new InternalV1Api.CompleteOnboardingUsingPUTMultipartForm();
        form.contract = toTempFile(contract);
        completeOnboardingUsingPUT(form, onboardingId).await().indefinitely();
    }

    private static File toTempFile(MultipartFile multipartFile) {
        try {
            String suffix = multipartFile.getOriginalFilename() == null ? ".bin" : "-" + multipartFile.getOriginalFilename();
            File file = Files.createTempFile("internal-", suffix).toFile();
            multipartFile.transferTo(file);
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }
}
