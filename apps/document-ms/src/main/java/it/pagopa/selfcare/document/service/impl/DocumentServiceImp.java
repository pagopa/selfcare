package it.pagopa.selfcare.document.service.impl;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.document.config.DocumentMsConfig;
import it.pagopa.selfcare.document.controller.response.ContractSignedReport;
import it.pagopa.selfcare.document.entity.Document;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;

@Slf4j
@ApplicationScoped
public class DocumentServiceImp implements DocumentService {

    public static final String HTTP_HEADER_VALUE_ATTACHMENT_FILENAME = "attachment;filename=";

    private final DocumentRepository documentRepository;
    private final AzureBlobClient azureBlobClient;
    private final DocumentMsConfig documentMsConfig;


    public DocumentServiceImp(DocumentRepository documentRepository, AzureBlobClient azureBlobClient, DocumentMsConfig documentMsConfig) {
        this.documentRepository = documentRepository;
        this.azureBlobClient = azureBlobClient;
        this.documentMsConfig = documentMsConfig;
    }

    @Override
    public Uni<List<Document>> getToken(String onboardingId) {
        return null;
    }

    @Override
    public Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned) {
        return null;
    }

    @Override
    public Uni<RestResponse<File>> retrieveSignedFile(String onboardingId) {
        return null;
    }

    @Override
    public Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String attachmentName) {
        return null;
    }

    @Override
    public Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName) {
        return documentRepository.findAttachment(onboardingId, ATTACHMENT.name(), attachmentName)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Attachment with id %s not found", onboardingId)))
                .onItem().transformToUni(document ->
                        Uni.createFrom()
                                .item(() -> azureBlobClient.getFileAsPdf(buildAttachmentPath(document)))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                .onItem().transform(contract -> RestResponse.ResponseBuilder
                                        .ok(contract, MediaType.APPLICATION_OCTET_STREAM)
                                        .header(
                                                HttpHeaders.CONTENT_DISPOSITION,
                                                HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + document.getContractFilename()
                                        )
                                        .build())
                );
    }

    @Override
    public Uni<Void> uploadAttachment(String onboardingId, FormItem file, String attachmentName) {
        return null;
    }

    @Override
    public Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath) {
        return null;
    }

    @Override
    public Uni<List<String>> getAttachments(String onboardingId) {
        return null;
    }

    @Override
    public Uni<ContractSignedReport> reportContractSigned(String onboardingId) {
        return documentRepository.findById(onboardingId)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Document with id %s not found", onboardingId)))
                .onItem().transformToUni(document ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(document.getContractSigned()))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                .onItem().transform(contract -> {
                                    //TODO: implement signature verification logic
                                    // signatureService.verifySignature(contract);
                                    return ContractSignedReport.cades(true);
                                }))
                .onFailure().recoverWithUni(() -> Uni.createFrom().item(ContractSignedReport.cades(false)));
    }

    @Override
    public String getAndVerifyDigest(FormItem file, ContractTemplate contract, boolean skipDigestCheck) {
        return "";
    }

    @Override
    public String getTemplateAndVerifyDigest(FormItem file, String contractTemplatePath, boolean skipDigestCheck) {
        return "";
    }

    @Override
    public String getContractPathByOnboarding(String onboardingId, String filename) {
        return "";
    }

    // questo metodo incorporava lato onboarding-ms un controllo sull'onboarding associato al token, lasciare la logica dell'esistenza dell'onboarding lato onboarding-ms
    @Override
    public Uni<Boolean> existsAttachment(String onboardingId, String attachmentName) {
        return documentRepository.findById(onboardingId)
                .onItem().transformToUni(document -> checkAttachmentExists(document, onboardingId, attachmentName));
    }

    @Override
    public Uni<Document> retrieveToken(String onboardingId) {
        return null;
    }

    @Override
    public Uni<String> updateTokenWithFilePath(String filepath, Document document) {
        return null;
    }

    @Override
    public Uni<Void> updateTokenUpdatedAt(String onboardingId) {
        return null;
    }

    @Override
    public Uni<Long> updateDocumentContractFiles(Document document) {
        return documentRepository.updateContractFiles(
                document.getId(),
                document.getContractSigned(),
                document.getContractFilename()
        );
    }

    private String buildAttachmentPath(Document document) {
        return Objects.nonNull(document.getContractSigned()) ? document.getContractSigned() : getAttachmentByOnboarding(document.getOnboardingId(), document.getContractFilename());
    }

    private String getAttachmentByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s%s", documentMsConfig.getContractPath(), onboardingId, "/attachments", "/" + filename);
    }

    private Uni<Boolean> checkAttachmentExists(Document document, String onboardingId, String attachmentName) {
        if (Objects.isNull(document)) {
            log.info("Token not found onboardingId={}, attachmentName={}", onboardingId, attachmentName);
            return Uni.createFrom().item(false);
        }

        return Uni.createFrom()
                .item(() -> verifyAttachmentInStorage(document, onboardingId, attachmentName))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private boolean verifyAttachmentInStorage(Document document, String onboardingId, String attachmentName) {
        try {
            azureBlobClient.getProperties(document.getContractSigned());
            log.info("Attachment found in storage onboardingId={}, attachmentName={}", onboardingId, attachmentName);
            return true;
        } catch (SelfcareAzureStorageException e) {
            log.info("Attachment not found in storage onboardingId={}, attachmentName={}", onboardingId, attachmentName);
            return false;
        }
    }
}
