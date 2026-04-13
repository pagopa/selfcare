package it.pagopa.selfcare.document.service.impl;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.document.config.DocumentMsConfig;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.model.dto.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.model.dto.response.ContractSignedReport;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.document.service.SignatureService;
import it.pagopa.selfcare.document.util.DocumentFileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.selfcare.document.config.DocumentMsConfig.PDF_FORMAT_FILENAME;
import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.document.util.Utils.CONTRACT_FILENAME_FUNC;
import static it.pagopa.selfcare.document.util.Utils.createBaseDocument;
import static it.pagopa.selfcare.onboarding.common.DocumentType.ATTACHMENT;
import static it.pagopa.selfcare.onboarding.common.DocumentType.INSTITUTION;

@Slf4j
@ApplicationScoped
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMsConfig documentMsConfig;
    private final AzureBlobClient azureBlobClient;

    @Inject
    SignatureService signatureService;

    public DocumentServiceImpl(DocumentRepository documentRepository, DocumentMsConfig documentMsConfig,
                               AzureBlobClient azureBlobClient) {
        this.documentRepository = documentRepository;
        this.documentMsConfig = documentMsConfig;
        this.azureBlobClient = azureBlobClient;
    }

    @Override
    public Uni<List<Document>> getDocumentsByOnboardingId(String onboardingId) {
        return documentRepository.findAllByOnboardingId(onboardingId);
    }

    @Override
    public Uni<Document> getDocumentInstitutionByOnboardingId(String onboardingId) {
        return documentRepository.findDocumentInstitutionByOnboardingId(onboardingId);
    }

    @Override
    public Uni<Document> getDocumentById(String documentId) {
        return documentRepository.findById(documentId)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Document with id %s not found", documentId)));
    }

    @Override
    public Uni<Document> getDocumentByOnboardingId(String onboardingId) {
        return documentRepository.findByOnboardingId(onboardingId);
    }

    @Override
    public Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath) {
        return documentRepository.updateContractSignedByOnboardingId(onboardingId, documentSignedPath);
    }

    @Override
    public Uni<List<String>> getAttachments(String onboardingId) {
        return documentRepository.findAttachments(onboardingId)
                .onItem().transform(attachments -> attachments.stream()
                        .map(Document::getAttachmentName)
                        .toList());
    }

    @Override
    public Uni<ContractSignedReport> reportContractSigned(String onboardingId) {
        return documentRepository.findByOnboardingId(onboardingId)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Document with id %s not found", onboardingId)))
                .onItem().transformToUni(document ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(document.getContractSigned()))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                .onItem().transform(contract -> {
                                    signatureService.verifySignature(contract);
                                    return ContractSignedReport.cades(true);
                                }))
                .onFailure().recoverWithUni(() -> Uni.createFrom().item(ContractSignedReport.cades(false)));
    }

    // questo metodo incorporava lato onboarding-ms un controllo sull'onboarding associato al document, lasciare la logica dell'esistenza dell'onboarding lato onboarding-ms
    @Override
    public Uni<Boolean> existsAttachment(String onboardingId, String attachmentName) {
        return documentRepository.findAttachment(onboardingId, ATTACHMENT.name(), attachmentName)
                .onItem().transformToUni(document -> checkAttachmentExists(document, onboardingId, attachmentName));
    }

    @Override
    public Uni<Void> updateDocumentUpdatedAt(String onboardingId) {
        log.info("Updating document 'updatedAt' for onboardingId={}", sanitize(onboardingId));
        LocalDateTime updatedAt = LocalDateTime.now();
        return documentRepository.updateUpdatedAt(onboardingId, updatedAt)
                .onItem().transform(ignore -> null);
    }

    @Override
    public Uni<Long> updateDocumentContractFiles(Document document) {
        return documentRepository.updateContractFiles(
                document.getOnboardingId(),
                document.getContractSigned(),
                document.getContractFilename()
        );
    }

    @Override
    public Uni<Document> saveDocument(DocumentBuilderRequest request) {
        log.info("Saving document for onboarding: {}, documentType: {}",
                sanitize(request.getOnboardingId()), sanitize(String.valueOf(request.getDocumentType())));

        if (request.isAttachment()) {
            return handleAttachmentDocument(request);
        }
        // INSTITUTION and USER share the same logic
        return handleContractDocument(request);
    }

    public Uni<Document> handleContractDocument(DocumentBuilderRequest request) {
        String onboardingId = request.getOnboardingId();

        return documentRepository.findByOnboardingId(onboardingId)
                .onItem().ifNull().switchTo(() -> {
                    Document document = new Document();
                    setContractFileName(request, document);
                    String contractNotSignedPath = DocumentFileUtils.getContractNotSigned(
                            onboardingId, documentMsConfig.getContractPath(), document.getContractFilename());

                    return calculateDigestFromAzureFile(contractNotSignedPath, onboardingId, "Contract")
                            .chain(digest -> persistDocument(request, digest));
                });
    }

    private Uni<Document> handleAttachmentDocument(DocumentBuilderRequest request) {
        String onboardingId = request.getOnboardingId();

        return documentRepository.findAttachment(onboardingId, ATTACHMENT.name(), request.getAttachmentName())
                .onItem().ifNull().switchTo(() -> {
                    Document document = new Document();
                    document.setOnboardingId(onboardingId);
                    setContractFileName(request, document);
                    String attachmentPath = DocumentFileUtils.buildAttachmentPath(
                            document, documentMsConfig.getContractPath());

                    return calculateDigestFromAzureFile(attachmentPath, onboardingId, "Attachment")
                            .chain(digest -> persistDocument(request, digest));
                });
    }

    private Uni<Document> persistDocument(DocumentBuilderRequest request, String digest) {
        Document document = buildDocument(request, digest);

        return documentRepository.persist(document)
                .replaceWith(document)
                .onItem().invoke(() ->
                        log.info("Document persisted for onboardingId: {}, documentType: {}",
                                sanitize(request.getOnboardingId()), sanitize(String.valueOf(request.getDocumentType()))));
    }

    @Override
    public Uni<Document> persistDocumentForImport(OnboardingDocumentRequest request) {
        log.info("Creating document for import, onboardingId: {}", sanitize(request.getOnboardingId()));

        Document document = createBaseDocument(request.getOnboardingId(),
                request.getProductId(),
                request.getTemplatePath(),
                request.getTemplateVersion()
        );
        document.setContractSigned(request.getContractFilePath());
        document.setContractFilename(request.getContractFileName());
        document.setCreatedAt(request.getContractCreatedAt());
        document.setUpdatedAt(request.getContractCreatedAt());
        document.setRootOnboardingId(request.getOnboardingId());
        document.setType(INSTITUTION);

        return documentRepository.persist(document)
                .replaceWith(document)
                .onItem().invoke(() ->
                        log.info("Document persisted for onboardingId: {}", sanitize(request.getOnboardingId())));
    }

    private Document buildDocument(DocumentBuilderRequest request, String digest) {
        log.debug("Creating Document for onboarding {} ...", sanitize(request.getOnboardingId()));

        Document document = new Document();
        // 1. Chiave Surrogata universale
        document.setId(UUID.randomUUID().toString());

        // 2. Dati di base
        document.setType(request.getDocumentType());
        document.setProductId(request.getProductId());
        document.setContractTemplate(request.getTemplatePath());
        document.setContractVersion(request.getTemplateVersion());
        document.setChecksum(digest);
        document.setAttachmentName(request.getAttachmentName());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        setContractFileName(request, document);

        switch (request.getDocumentType()) {
            case INSTITUTION, ATTACHMENT -> {
                document.setOnboardingId(request.getOnboardingId());
                document.setRootOnboardingId(request.getOnboardingId());
            }
            case USER -> {
                document.setOnboardingId(request.getOnboardingId());
                document.setRootOnboardingId(request.getRootOnboardingId());
            }
        }

        return document;
    }

    private static void setContractFileName(DocumentBuilderRequest request, Document document) {
        String filenamePattern = ATTACHMENT.equals(request.getDocumentType())
                ? "%s_" + request.getAttachmentName() + ".pdf"
                : PDF_FORMAT_FILENAME;

        document.setContractFilename(CONTRACT_FILENAME_FUNC.apply(filenamePattern, request.getProductTitle()));
    }

    private Uni<Boolean> checkAttachmentExists(Document document, String onboardingId, String attachmentName) {
        if (Objects.isNull(document)) {
            log.info("Document not found onboardingId={}, attachmentName={}", sanitize(onboardingId), sanitize(attachmentName));
            return Uni.createFrom().item(false);
        }

        return Uni.createFrom()
                .item(() -> verifyAttachmentInStorage(document, onboardingId, attachmentName))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private boolean verifyAttachmentInStorage(Document document, String onboardingId, String attachmentName) {
        try {
            azureBlobClient.getProperties(document.getContractSigned());
            log.info("Attachment found in storage onboardingId={}, attachmentName={}", sanitize(onboardingId), sanitize(attachmentName));
            return true;
        } catch (SelfcareAzureStorageException e) {
            log.info("Attachment not found in storage onboardingId={}, attachmentName={}", sanitize(onboardingId), sanitize(attachmentName));
            return false;
        }
    }

    private Uni<String> calculateDigestFromAzureFile(String azureFilePath, String onboardingId, String logDocType) {
        log.info("{} not found in DB for onboarding {}. Calculating digest from original template: {}",
                logDocType, sanitize(onboardingId), sanitize(azureFilePath));

        return Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(azureFilePath))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform(file -> {
                    DSSDocument dssDocument = new FileDocument(file);
                    String digest = dssDocument.getDigest(DigestAlgorithm.SHA256).getBase64Value();
                    log.info("{} digest calculated successfully for onboarding {}: {}",
                            logDocType, sanitize(onboardingId), digest);
                    return digest;
                });
    }
}
