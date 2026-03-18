package it.pagopa.selfcare.document.service.impl;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.model.dto.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.model.dto.response.ContractSignedReport;
import it.pagopa.selfcare.document.model.dto.response.DocumentBuilderResponse;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.document.service.DocumentContentService;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.document.service.SignatureService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.document.util.Utils.*;
import static it.pagopa.selfcare.onboarding.common.TokenType.*;

@Slf4j
@ApplicationScoped
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final AzureBlobClient azureBlobClient;
    private final DocumentContentService documentContentService;

    @Inject
    SignatureService signatureService;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               AzureBlobClient azureBlobClient,
                               DocumentContentService documentContentService) {
        this.documentRepository = documentRepository;
        this.azureBlobClient = azureBlobClient;
        this.documentContentService = documentContentService;
    }

    @Override
    public Uni<List<Document>> getDocumentsByOnboardingId(String onboardingId) {
        return documentRepository.findAllByOnboardingId(onboardingId);
    }

    @Override
    public Uni<Document> getDocumentById(String documentId) {
        return documentRepository.findById(new ObjectId(documentId))
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Document with id %s not found", documentId)));
    }

    @Override
    public Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath) {
        return documentRepository.updateContractSignedByOnboardingId(onboardingId, documentSignedPath);
    }

    @Override
    public Uni<List<String>> getAttachments(String onboardingId) {
        return documentRepository.findAttachments(onboardingId)
                .onItem().transform(attachments -> attachments.stream()
                        .map(Document::getName)
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
    public Uni<DocumentBuilderResponse> saveDocument(DocumentBuilderRequest request) {
        log.info("Saving document for onboarding: {}, documentType: {}",
                sanitize(request.getOnboardingId()), sanitize(String.valueOf(request.getDocumentType())));

        if (request.isAttachment()) {
            return handleAttachmentDocument(request);
        }
        // INSTITUTION and USER share the same logic
        return handleContractDocument(request);
    }

    private Uni<DocumentBuilderResponse> handleContractDocument(DocumentBuilderRequest request) {
        String onboardingId = request.getOnboardingId();

        return documentRepository.findByOnboardingId(onboardingId)
                .onItem().transformToUni(existingDoc -> {
                    if (Objects.nonNull(existingDoc)) {
                        return Uni.createFrom().item(DocumentBuilderResponse.builder()
                                .documentId(existingDoc.getId())
                                .alreadyExists(true)
                                .build());
                    }

                    return documentContentService.retrieveContract(onboardingId, false)
                            .onItem().transform(restResponse -> {
                                File contract = restResponse.getEntity();
                                DSSDocument dssDocument = new FileDocument(contract);
                                return dssDocument.getDigest(DigestAlgorithm.SHA256).getBase64Value();
                            })
                            .onItem().transformToUni(digest -> persistDocument(request, digest));
                });
    }

    private Uni<DocumentBuilderResponse> handleAttachmentDocument(DocumentBuilderRequest request) {
        String onboardingId = request.getOnboardingId();

        return documentContentService.retrieveAttachment(onboardingId, request.getDocumentName())
                .onItem().transform(restResponse -> {
                    File attachment = restResponse.getEntity();
                    DSSDocument dssDocument = new FileDocument(attachment);
                    return dssDocument.getDigest(DigestAlgorithm.SHA256).getBase64Value();
                })
                .onItem().transformToUni(digest -> persistDocument(request, digest));
    }

    private Uni<DocumentBuilderResponse> persistDocument(DocumentBuilderRequest request, String digest) {
        Document document = buildDocument(request, digest);

        return documentRepository.persist(document)
                .onItem().transform(persisted -> DocumentBuilderResponse.builder()
                        .documentId(persisted.getId())
                        .checksum(digest)
                        .alreadyExists(false)
                        .build());
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
        document.setType(INSTITUTION);

        return documentRepository.persist(document)
                .replaceWith(document)
                .onItem().invoke(() ->
                        log.info("Document persisted for onboardingId: {}", sanitize(request.getOnboardingId())));
    }

    private Document buildDocument(DocumentBuilderRequest request, String digest) {
        log.debug("Creating Document for onboarding {} ...", sanitize(request.getOnboardingId()));

        Document document = createBaseDocument(
                request.getOnboardingId(),
                request.getProductId(),
                request.getTemplatePath(),
                request.getTemplateVersion()
        );
        document.setChecksum(digest);
        document.setType(request.getDocumentType());
        document.setName(request.getDocumentName());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        setContractFileName(request, document);

        return document;
    }

    private static void setContractFileName(DocumentBuilderRequest request, Document document) {
        String filenamePattern = ATTACHMENT.equals(request.getDocumentType())
                ? "%s_" + request.getDocumentName() + ".pdf"
                : request.getPdfFormatFilename();

        document.setContractFilename(CONTRACT_FILENAME_FUNC.apply(filenamePattern, request.getProductTitle()));
    }

    private Uni<Boolean> checkAttachmentExists(Document document, String onboardingId, String attachmentName) {
        if (Objects.isNull(document)) {
            log.info("Document not found onboardingId={}, documentName={}", sanitize(onboardingId), sanitize(attachmentName));
            return Uni.createFrom().item(false);
        }

        return Uni.createFrom()
                .item(() -> verifyAttachmentInStorage(document, onboardingId, attachmentName))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private boolean verifyAttachmentInStorage(Document document, String onboardingId, String attachmentName) {
        try {
            azureBlobClient.getProperties(document.getContractSigned());
            log.info("Attachment found in storage onboardingId={}, documentName={}", sanitize(onboardingId), sanitize(attachmentName));
            return true;
        } catch (SelfcareAzureStorageException e) {
            log.info("Attachment not found in storage onboardingId={}, documentName={}", sanitize(onboardingId), sanitize(attachmentName));
            return false;
        }
    }
}
