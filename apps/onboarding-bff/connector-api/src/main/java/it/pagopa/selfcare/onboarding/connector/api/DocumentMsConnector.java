package it.pagopa.selfcare.onboarding.connector.api;

import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentMsConnector {
    Resource getContract(String onboardingId);

    Resource getTemplateAttachment(OnboardingData onboardingId, String filename, String templatePath);

    Resource getAttachment(String onboardingId, String filename);

    HttpStatusCode headAttachment(String onboardingId, String filename);

    void uploadAttachment(String onboardingId, MultipartFile attachment, String attachmentName, String id, AttachmentTemplate template);

    Resource getAggregatesCsv(String onboardingId, String productId);

}
