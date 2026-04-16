package it.pagopa.selfcare.mscore.api;

import it.pagopa.selfcare.mscore.model.onboarding.ResourceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileStorageConnector {

    ResourceResponse getFile(String fileName);

    String getTemplateFile(String templateName);

    String uploadContract(String id, MultipartFile contract);

    void removeContract(String fileName, String tokenId);

    File getFileAsPdf(String contractTemplate);
}
