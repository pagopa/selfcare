package it.pagopa.selfcare.onboarding.connector.rest.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openapi.quarkus.onboarding_json.api.TokenControllerApi;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingTokenApiClient extends TokenControllerApi {

    default ResponseEntity<Resource> _getContract(String onboardingId) {
        File file = getContract(onboardingId).await().indefinitely();
        return ResponseEntity.ok(new FileSystemResource(file));
    }

    default ResponseEntity<Resource> _getTemplateAttachment(String onboardingId, String name) {
        File file = getTemplateAttachment(onboardingId, name).await().indefinitely();
        return ResponseEntity.ok(new FileSystemResource(file));
    }

    default ResponseEntity<Resource> _getAttachment(String onboardingId, String name) {
        File file = getAttachment(onboardingId, name).await().indefinitely();
        return ResponseEntity.ok(new FileSystemResource(file));
    }

    default void _uploadAttachment(String onboardingId, String name, MultipartFile file) {
        TokenControllerApi.UploadAttachmentMultipartForm form = new TokenControllerApi.UploadAttachmentMultipartForm();
        form._file = toTempFile(file);
        uploadAttachment(form, onboardingId, name).await().indefinitely();
    }

    default ResponseEntity<Void> _headAttachment(String onboardingId, String name) {
        jakarta.ws.rs.core.Response response = headAttachment(onboardingId, name).await().indefinitely();
        HttpStatus httpStatus = HttpStatus.resolve(response.getStatus());
        return ResponseEntity.status(httpStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : httpStatus).build();
    }

    private static File toTempFile(MultipartFile multipartFile) {
        try {
            String suffix = multipartFile.getOriginalFilename() == null ? ".bin" : "-" + multipartFile.getOriginalFilename();
            File file = Files.createTempFile("token-", suffix).toFile();
            multipartFile.transferTo(file);
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }
}
