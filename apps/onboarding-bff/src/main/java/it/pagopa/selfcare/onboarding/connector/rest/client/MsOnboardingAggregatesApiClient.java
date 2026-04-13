package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.connector.model.BinaryData;
import it.pagopa.selfcare.onboarding.connector.model.UploadedFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openapi.quarkus.onboarding_json.api.AggregatesControllerApi;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingAggregatesApiClient extends AggregatesControllerApi {

    default VerifyAggregateResponse _verifyAppIoAggregatesCsv(UploadedFile file) {
        AggregatesControllerApi.VerifyAppIoAggregatesCsvMultipartForm form =
            new AggregatesControllerApi.VerifyAppIoAggregatesCsvMultipartForm();
        form.aggregates = toTempFile(file);
        return verifyAppIoAggregatesCsv(form).await().indefinitely();
    }

    default VerifyAggregateResponse _verifyPagoPaAggregatesCsv(UploadedFile file) {
        AggregatesControllerApi.VerifyPagoPaAggregatesCsvMultipartForm form =
            new AggregatesControllerApi.VerifyPagoPaAggregatesCsvMultipartForm();
        form.aggregates = toTempFile(file);
        return verifyPagoPaAggregatesCsv(form).await().indefinitely();
    }

    default VerifyAggregateResponse _verifySendAggregatesCsv(UploadedFile file) {
        AggregatesControllerApi.VerifySendAggregatesCsvMultipartForm form =
            new AggregatesControllerApi.VerifySendAggregatesCsvMultipartForm();
        form.aggregates = toTempFile(file);
        return verifySendAggregatesCsv(form).await().indefinitely();
    }

    default BinaryData _getAggregatesCsv(String onboardingId, String productId) {
        File file = getAggregatesCsv(onboardingId, productId).await().indefinitely();
        return toBinaryData(file, file.getName());
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
            String suffix = fileName == null ? ".csv" : "-" + fileName;
            File file = Files.createTempFile("aggregates-", suffix).toFile();
            Files.write(file.toPath(), uploadedFile.content());
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }
}
