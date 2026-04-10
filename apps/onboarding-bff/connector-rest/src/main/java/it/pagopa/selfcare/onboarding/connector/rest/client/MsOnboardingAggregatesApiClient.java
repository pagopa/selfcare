package it.pagopa.selfcare.onboarding.connector.rest.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openapi.quarkus.onboarding_json.api.AggregatesControllerApi;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingAggregatesApiClient extends AggregatesControllerApi {

    default ResponseEntity<VerifyAggregateResponse> _verifyAppIoAggregatesCsv(MultipartFile file) {
        AggregatesControllerApi.VerifyAppIoAggregatesCsvMultipartForm form =
            new AggregatesControllerApi.VerifyAppIoAggregatesCsvMultipartForm();
        form.aggregates = toTempFile(file);
        return ResponseEntity.ok(verifyAppIoAggregatesCsv(form).await().indefinitely());
    }

    default ResponseEntity<VerifyAggregateResponse> _verifyPagoPaAggregatesCsv(MultipartFile file) {
        AggregatesControllerApi.VerifyPagoPaAggregatesCsvMultipartForm form =
            new AggregatesControllerApi.VerifyPagoPaAggregatesCsvMultipartForm();
        form.aggregates = toTempFile(file);
        return ResponseEntity.ok(verifyPagoPaAggregatesCsv(form).await().indefinitely());
    }

    default ResponseEntity<VerifyAggregateResponse> _verifySendAggregatesCsv(MultipartFile file) {
        AggregatesControllerApi.VerifySendAggregatesCsvMultipartForm form =
            new AggregatesControllerApi.VerifySendAggregatesCsvMultipartForm();
        form.aggregates = toTempFile(file);
        return ResponseEntity.ok(verifySendAggregatesCsv(form).await().indefinitely());
    }

    default ResponseEntity<Resource> _getAggregatesCsv(String onboardingId, String productId) {
        File file = getAggregatesCsv(onboardingId, productId).await().indefinitely();
        return ResponseEntity.ok(new FileSystemResource(file));
    }

    private static File toTempFile(MultipartFile multipartFile) {
        try {
            String suffix = multipartFile.getOriginalFilename() == null ? ".csv" : "-" + multipartFile.getOriginalFilename();
            File file = Files.createTempFile("aggregates-", suffix).toFile();
            multipartFile.transferTo(file);
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }
}
