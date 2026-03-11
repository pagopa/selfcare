package it.pagopa.selfcare.document.service.impl;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.document.config.DocumentMsConfig;
import it.pagopa.selfcare.document.controller.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.controller.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.controller.response.ContractSignedReport;
import it.pagopa.selfcare.document.controller.response.DocumentBuilderResponse;
import it.pagopa.selfcare.document.entity.Document;
import it.pagopa.selfcare.document.exception.InternalException;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.document.service.DocumentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static it.pagopa.selfcare.document.util.ErrorMessage.*;
import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.document.util.Utils.*;
import static it.pagopa.selfcare.onboarding.common.TokenType.*;

@Slf4j
@ApplicationScoped
public class DocumentServiceImp implements DocumentService {

    public static final String HTTP_HEADER_VALUE_ATTACHMENT_FILENAME = "attachment;filename=";

    private final DocumentRepository documentRepository;
    private final AzureBlobClient azureBlobClient;
    private final DocumentMsConfig documentMsConfig;

    @ConfigProperty(name = "onboarding-ms.signature.verify-enabled")
    Boolean isVerifyEnabled;
    @ConfigProperty(name = "onboarding-ms.blob-storage.path-contracts")
    String pathContracts;

    public DocumentServiceImp(DocumentRepository documentRepository,
                              AzureBlobClient azureBlobClient,
                              DocumentMsConfig documentMsConfig) {
        this.documentRepository = documentRepository;
        this.azureBlobClient = azureBlobClient;
        this.documentMsConfig = documentMsConfig;
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
    public Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned) {
        return documentRepository.findByOnboardingId(onboardingId)
                .onItem().transformToUni(document ->
                        Uni.createFrom().item(() -> azureBlobClient.getFileAsPdf(isSigned ? document.getContractSigned() : getContractNotSigned(onboardingId, document)))
                                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                                .onItem().transform(contract -> {
                                    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(contract, MediaType.APPLICATION_OCTET_STREAM);
                                    response.header(HttpHeaders.CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + getCurrentContractName(document, isSigned));
                                    return response.build();
                                }));
    }

    private String getContractNotSigned(String onboardingId, Document document) {
        return String.format("%s%s/%s", documentMsConfig.getContractPath(), onboardingId,
                document.getContractFilename());
    }

    private static String getCurrentContractName(Document document, boolean isSigned) {
        return isSigned ? getContractSignedName(document) : document.getContractFilename();
    }

    private static String getContractSignedName(Document document) {
        File file = new File(document.getContractSigned());
        return file.getName();
    }

    @Override
    public Uni<RestResponse<File>> retrieveSignedFile(String onboardingId) {
        return documentRepository.findByOnboardingId(onboardingId)
                .onItem().transformToUni(document -> Uni.createFrom().item(() -> azureBlobClient.retrieveFile(document.getContractSigned()))
                        .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                        .onItem().transform(contract -> {
                            File fileToSend = contract;
                            if (document.getContractSigned().endsWith(".pdf")) {
                                isPdfValid(contract);
                            } else {
                                isP7mValid(contract);
                                //TODO: implement signature verification logic
                                // fileToSend = signatureService.extractFile(contract);
                                isPdfValid(fileToSend);
                            }
                            RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok(fileToSend, MediaType.APPLICATION_OCTET_STREAM);
                            String filename = getCurrentContractName(document, true);
                            response.header(HttpHeaders.CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename);
                            return response.build();
                        }).onFailure().recoverWithUni(() -> Uni.createFrom().item(RestResponse.ResponseBuilder.<File>notFound().build())));
    }

    public static void isPdfValid(File contract) {
        try (PDDocument document = Loader.loadPDF(contract)) {
            if (document.getNumberOfPages() == 0) {
                throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
            }
        } catch (IOException e) {
            throw new InvalidRequestException(ORIGINAL_DOCUMENT_NOT_FOUND.getMessage(), ORIGINAL_DOCUMENT_NOT_FOUND.getCode());
        }
    }

    public static void isP7mValid(File contract) {
        //TODO: implement signature verification logic
        // signatureService.verifySignature(contract);
    }

    @Override
    public Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String templatePath, String attachmentName) {
        return Uni.createFrom()
                .item(() -> azureBlobClient.getFileAsPdf(templatePath))
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Template Attachment not found on storage for onboarding: %s", onboardingId)))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .onItem().transform(file -> RestResponse.ResponseBuilder
                        .ok(file, MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + attachmentName)
                        .build());
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
    public Uni<Void> uploadAttachment(DocumentBuilderRequest request, FormItem file) {
        log.info(
                "Uploading attachment for onboardingId={}, documentName={}",
                sanitize(request.getOnboardingId()), sanitize(request.getDocumentName())
        );
        return existsAttachment(request.getOnboardingId(), request.getDocumentName())
                .onItem().transformToUni(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Uni.createFrom().failure(new UpdateNotAllowedException(ATTACHMENT_UPLOAD_ERROR.getCode(), ATTACHMENT_UPLOAD_ERROR.getMessage()));
                    }
                    return Uni.createFrom().voidItem();
                })
                .chain(() -> {
                    if (Boolean.TRUE.equals(isVerifyEnabled)) {
                        //TODO: implement signature verification logic
                        // signatureService.verifySignature(file.getFile());
                    }

                    String digest = getTemplateAndVerifyDigest(file, request.getTemplatePath(), false);

                    File fileToUpload = file.getFile();

                    boolean isP7M = Optional.of(file.getFileName())
                            .map(name -> name.toLowerCase(Locale.ROOT).endsWith(".p7m"))
                            .orElse(false);

                    return persistAttachment(request, digest, isP7M)
                            .onItem().invoke(document ->
                                    uploadFileToAzure(
                                            document.getContractFilename(),
                                            document.getOnboardingId(),
                                            fileToUpload
                                    )
                            );
                })
                .replaceWithVoid();
    }

    private Uni<Document> persistAttachment(DocumentBuilderRequest request, String digest, boolean isP7M) {
        Document document = createBaseDocument(
                request.getOnboardingId(),
                request.getProductId(),
                request.getTemplatePath(),
                request.getTemplateVersion()
        );

        document.setChecksum(digest);
        document.setType(ATTACHMENT);
        document.setName(request.getDocumentName());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        String signedContractFileName = extractFileName(request.getTemplatePath());
        String filename = String.format("signed_%s", signedContractFileName);
        if (isP7M) {
            filename = String.format("%s.p7m", filename);
        }
        document.setContractFilename(filename);
        document.setContractSigned(getAttachmentByOnboarding(request.getOnboardingId(), filename));

        return documentRepository.persist(document).replaceWith(document);
    }

    public String getTemplateAndVerifyDigest(FormItem file, String documentTemplatePath, boolean skipDigestCheck) {
        log.info("Start verifying uploaded content against template (templatePath={})", sanitize(documentTemplatePath));
        Objects.requireNonNull(file, "Uploaded file must not be null");
        Objects.requireNonNull(documentTemplatePath, "Document template path must not be null");

        DSSDocument uploadedDocument = new FileDocument(file.getFile());

        File templateFile = azureBlobClient.getFileAsPdf(documentTemplatePath);
        DSSDocument templateDocument = new FileDocument(templateFile);

        //TODO implement signature verification logic
        // DSSDocument uploadedPdf = signatureService.extractPdfFromSignedContainer(SignedDocumentValidator.fromDocument(uploadedDocument), uploadedDocument);
        // DSSDocument templatePdf = signatureService.extractPdfFromSignedContainer(SignedDocumentValidator.fromDocument(templateDocument), templateDocument);

        /*
        SignedDocumentValidator uploadedPdfValidator = SignedDocumentValidator.fromDocument(uploadedPdf);
        SignedDocumentValidator templatePdfValidator = SignedDocumentValidator.fromDocument(templatePdf);

        String templateDigest = signatureService.computeDigestOfSignedRevision(templatePdfValidator, templatePdf);
        String uploadedDigest = signatureService.computeDigestOfSignedRevision(uploadedPdfValidator, uploadedPdf);
        log.debug("Template content digest (base64): {}", templateDigest);
        log.debug("Uploaded  content digest (base64): {}", uploadedDigest);

        if (!templateDigest.equals(uploadedDigest)) {
            log.warn("Content mismatch ignoring signatures. templateDigest={} uploadedDigest={}", templateDigest, uploadedDigest);
            if (!skipDigestCheck) {
                throw new InvalidRequestException("File has been changed. It's not possible to complete upload");
            }
        } else {
            log.info("Content check passed (ignoring signatures).");
        }

        return uploadedDigest;
         */
        return "";
    }

    private void uploadFileToAzure(String filename, String onboardingId, File signedFile) throws InternalException {
        final String path = String.format("%s%s", pathContracts, onboardingId).concat("/attachments");

        try {
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(signedFile.toPath()));
        } catch (IOException e) {
            throw new InternalException(GENERIC_ERROR.getCode(),
                    "Error on upload contract for onboarding with id " + onboardingId);
        }
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
                                    //TODO: implement signature verification logic
                                    // signatureService.verifySignature(contract);
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

                    return retrieveContract(onboardingId, false)
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

        return retrieveAttachment(onboardingId, request.getDocumentName())
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

    private Document createBaseDocument(String onboardingId, String productId,
                                        String contractTemplate, String contractVersion) {
        Document document = new Document();
        document.setId(UUID.randomUUID().toString());
        document.setOnboardingId(onboardingId);
        document.setProductId(productId);
        document.setContractTemplate(contractTemplate);
        document.setContractVersion(contractVersion);
        return document;
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


    private String buildAttachmentPath(Document document) {
        return Objects.nonNull(document.getContractSigned()) ? document.getContractSigned() : getAttachmentByOnboarding(document.getOnboardingId(), document.getContractFilename());
    }

    private String getAttachmentByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s%s", documentMsConfig.getContractPath(), onboardingId, "/attachments", "/" + filename);
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
