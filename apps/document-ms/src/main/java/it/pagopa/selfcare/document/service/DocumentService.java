package it.pagopa.selfcare.document.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.controller.response.ContractSignedReport;
import it.pagopa.selfcare.document.entity.Document;
import java.io.File;
import java.util.List;

import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import org.jboss.resteasy.reactive.RestResponse;

public interface DocumentService {

    Uni<List<Document>> getToken(String onboardingId);

    Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned);

    Uni<RestResponse<File>> retrieveSignedFile(String onboardingId);

    Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String attachmentName);

    Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName);

    Uni<Void> uploadAttachment(String onboardingId, FormItem file, String attachmentName);

    Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath);

    Uni<List<String>> getAttachments(String onboardingId);

    Uni<ContractSignedReport> reportContractSigned(String onboardingId);

    String getAndVerifyDigest(FormItem file, ContractTemplate contract, boolean skipDigestCheck);

    String getTemplateAndVerifyDigest(FormItem file, String contractTemplatePath, boolean skipDigestCheck);

    String getContractPathByOnboarding(String onboardingId, String filename);

    Uni<Boolean> existsAttachment(String onboardingId, String attachmentName);

    Uni<Document> retrieveToken(String onboardingId);

    Uni<Document> retrieveToken(Onboarding onboarding, FormItem formItem, Product product);

    Uni<String> updateTokenWithFilePath(String filepath, Document document);

    Uni<Void> updateTokenUpdatedAt(String onboardingId);

    Uni<Void> persistTokenForImport(Onboarding onboardingPersisted, Product product, OnboardingImportContract contractImported);
}
