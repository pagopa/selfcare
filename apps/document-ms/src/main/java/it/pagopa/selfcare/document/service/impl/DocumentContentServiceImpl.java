package it.pagopa.selfcare.document.service.impl;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.config.DocumentMsConfig;
import it.pagopa.selfcare.document.exception.InternalException;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.document.service.DocumentContentService;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.document.service.PdfGenerationService;
import it.pagopa.selfcare.document.service.SignatureService;
import it.pagopa.selfcare.document.util.DocumentFileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.document.config.DocumentMsConfig.PDF_FORMAT_FILENAME;
import static it.pagopa.selfcare.document.util.ErrorMessage.ATTACHMENT_UPLOAD_ERROR;
import static it.pagopa.selfcare.document.util.ErrorMessage.GENERIC_ERROR;
import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.document.util.Utils.*;
import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;

/**
 * Implementation of DocumentContentService for creating, retrieving, and managing PDF documents.
 * Integrates with Azure Blob Storage for persistence and DSS for digital signatures.
 * Includes architectural resilience patterns (Retries with backoff and Compensating Transactions/Rollbacks).
 */
@Slf4j
@ApplicationScoped
public class DocumentContentServiceImpl implements DocumentContentService {
    public static final String HTTP_HEADER_VALUE_ATTACHMENT_FILENAME = "attachment;filename=";

    private final AzureBlobClient azureBlobClient;
    private final DocumentMsConfig documentMsConfig;
    private final SignatureService signatureService;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final PdfGenerationService pdfGenerationService; // <-- NUOVA INIECTION

    @ConfigProperty(name = "document-ms.blob-storage.path-contracts")
    String pathContracts;

    @ConfigProperty(name = "document-ms.retry.min-backoff-ms", defaultValue = "500")
    long retryMinBackoff;

    @ConfigProperty(name = "document-ms.retry.max-backoff-ms", defaultValue = "2000")
    long retryMaxBackoff;

    @ConfigProperty(name = "document-ms.retry.max-attempts", defaultValue = "3")
    int retryMaxAttempts;

    @Inject
    public DocumentContentServiceImpl(
            AzureBlobClient azureBlobClient,
            DocumentMsConfig documentMsConfig,
            SignatureService signatureService,
            DocumentRepository documentRepository,
            DocumentService documentService, PdfGenerationService pdfGenerationService) {
        this.azureBlobClient = azureBlobClient;
        this.documentMsConfig = documentMsConfig;
        this.signatureService = signatureService;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @Override
    public Uni<CreatePdfResponse> createContractPdf(ContractPdfRequest request) {
        log.info("START - createContractPdf for template: {} with onboardingId: {}",
                sanitize(request.getContractTemplatePath()), sanitize(request.getOnboardingId()));

        return buildContractContextAsync(request)
                .chain(ctx -> signPdfContextAsync(ctx, request))
                .chain(ctx -> uploadAndBuildResponse(ctx, "contract"));
    }

    @Override
    public Uni<CreatePdfResponse> createAttachmentPdf(AttachmentPdfRequest request) {
        log.info("START - createAttachmentPdf for template: {} with onboardingId: {}",
                sanitize(request.getAttachmentTemplatePath()), sanitize(request.getOnboardingId()));

        return buildAttachmentContextAsync(request)
                .chain(ctx -> uploadAndBuildResponse(ctx, "attachment"));
    }

    @Override
    public Uni<RestResponse<File>> retrieveSignedFile(String onboardingId) {
        return documentRepository.findByOnboardingId(onboardingId)
                .onFailure().retry().withBackOff(Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff)).atMost(retryMaxAttempts)                .onItem().transformToUni(document ->
                        fetchFileFromAzureAsync(document.getContractSigned())
                                .emitOn(Infrastructure.getDefaultWorkerPool())
                                .onItem().transform(contract -> validateAndExtractSignedFile(contract, document.getContractSigned()))
                                .onItem().transform(processedFile -> buildDownloadResponse(processedFile, document, true))
                )
                .onFailure().recoverWithItem(() -> RestResponse.ResponseBuilder.<File>notFound().build());
    }

    private void isP7mValid(File contract) {
        signatureService.verifySignature(contract);
    }

    @Override
    public Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned) {
        return documentRepository.findByOnboardingId(onboardingId)
                .onFailure().retry().withBackOff(Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff)).atMost(retryMaxAttempts)                .onItem().transformToUni(document ->
                        fetchPdfFromAzureAsync(document, onboardingId, isSigned)
                                .onItem().transform(contractFile -> buildDownloadResponse(contractFile, document, isSigned))
                );
    }

    @Override
    public Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String templatePath,
                                                              String attachmentName, String institutionDescription,
                                                              String productId) {
        return Uni.createFrom()
                .item(() -> azureBlobClient.getFileAsPdf(templatePath))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Template Attachment not found on storage for onboarding: %s", onboardingId)))
                .chain(file -> signatureService.signDocument(file, institutionDescription, productId))
                .onItem().transform(file -> RestResponse.ResponseBuilder
                        .ok(file, MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + attachmentName)
                        .build());
    }

    @Override
    public Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName) {
        return documentRepository.findAttachment(onboardingId, ATTACHMENT.name(), attachmentName)
                .onFailure().retry().withBackOff(Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff)).atMost(retryMaxAttempts)                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Attachment with id %s not found", onboardingId)))
                .onItem().transformToUni(document ->
                        Uni.createFrom()
                                .item(() -> azureBlobClient.getFileAsPdf(DocumentFileUtils.buildAttachmentPath(document, documentMsConfig.getContractPath())))
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
        log.info("Uploading attachment for onboardingId={}, productId={}, attachmentName={}",
                sanitize(request.getOnboardingId()),
                sanitize(request.getProductId()),
                sanitize(file.getFileName()));

        return verifyAttachmentDoesNotExist(request)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .chain(() -> {
                    String uploadedDigest = performSecurityValidationsAndGetDigest(request, file);
                    boolean isP7M = DocumentFileUtils.isP7MFile(file.getFileName());

                    // 1. Salvataggio su DB
                    return persistAttachment(request, uploadedDigest, isP7M);
                })
                // 2. Upload asincrono su Azure
                .call(document -> uploadToAzureReactive(document, file)
                        .onFailure().call(azureError -> {
                            log.error("Upload to Azure failed for attachment {}. Rolling back DB record...", sanitize(request.getAttachmentName()));
                            return documentRepository.delete(document)
                                    .onFailure().invoke(e -> log.error("CRITICAL: Rollback failed for DB document {}", document.getId(), e));
                        })
                )
                .replaceWithVoid();
    }

    @Override
    public Uni<String> deleteContract(String onboardingId) {
        log.info("START - deleteContract for onboardingId: {}", sanitize(onboardingId));

        return documentService.getDocumentInstitutionByOnboardingId(onboardingId)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .chain(document -> {
                    String basePath = documentMsConfig.getContractPath();
                    String originalSignedPath = DocumentFileUtils.buildAndValidateContractFilePath(document.getContractSigned(), basePath, true);
                    String originalContractPath = DocumentFileUtils.buildAndValidateContractFilePath(onboardingId + "/" + document.getContractFilename(), basePath, false);

                    String deletedSignedContract;
                    String deletedContractFile;

                    try {
                        deletedSignedContract = deleteFileFromAzure(originalSignedPath, basePath);
                        deletedContractFile = deleteFileFromAzure(originalContractPath, basePath);
                    } catch (Exception e) {
                        log.error("Error deleting contract files from Azure for onboardingId {}: {}", sanitize(onboardingId), e.getMessage());
                        return Uni.createFrom().failure(new RuntimeException("Error deleting contract files from Azure", e));
                    }

                    document.setContractSigned(deletedSignedContract);
                    document.setContractFilename(deletedContractFile);

                    return documentService.updateDocumentContractFiles(document)
                            .onFailure().retry().withBackOff(Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff)).atMost(retryMaxAttempts)                            .onFailure().call(dbError -> {
                                log.error("DB update failed for onboardingId {}. Triggering Azure Rollback...", onboardingId);
                                return rollbackDeletedAzureFiles(deletedSignedContract, originalSignedPath, deletedContractFile, originalContractPath);
                            });
                })
                .replaceWith("Contract deleted successfully");
    }

    @Override
    public Uni<Void> uploadAggregatesCsv(UploadAggregateCsvRequest request) {
        log.info("Uploading aggregates CSV for onboardingId: {}, productId: {}",
                sanitize(request.getOnboardingId()), sanitize(request.getProductId()));
        final String filename = "aggregates.csv";

        return Uni.createFrom().item(request::getCsv)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .invoke(csvFile -> {
                    final String path = String.format("%s%s/%s",
                            documentMsConfig.getAggregatesPath(), request.getOnboardingId(), request.getProductId());
                    try {
                        azureBlobClient.uploadFile(path, filename, csvFile.readAllBytes());
                    } catch (IOException e) {
                        log.error("Error reading from file {} ", sanitize(path), e);
                        // Rilanciamo l'errore per far scattare il Retry
                        throw new RuntimeException("Error during Azure upload", e);
                    }
                })
                .onFailure().retry().withBackOff(Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff)).atMost(retryMaxAttempts)                .onFailure()
                .transform(e -> {
                    log.error(
                            "Impossible to store csv aggregate for onboardingId: {}, filename: {}. Error: {}",
                            sanitize(request.getOnboardingId()), sanitize(filename), e.getMessage(), e);
                    return new InternalException(
                            GENERIC_ERROR.getCode(),
                            String.format("Error storing csv aggregate for onboardingId: %s", sanitize(request.getOnboardingId())));
                })
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> saveVisuraForMerchant(UploadVisuraRequest uploadVisuraRequest) {
        final String filename = uploadVisuraRequest.getFilename();
        final String path = String.format("%s%s/visura", documentMsConfig.getContractPath(), uploadVisuraRequest.getOnboardingId());

        return Uni.createFrom().item(uploadVisuraRequest::getFileContent)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .invoke(file -> {
                    try {
                        azureBlobClient.uploadFile(path, filename, file.readAllBytes());
                    } catch (IOException e) {
                        log.error("Error reading from file {} ", sanitize(path), e);
                        // Rilanciamo l'errore per far scattare il Retry
                        throw new RuntimeException("Error during Azure upload", e);
                    }
                })
                .onFailure().retry().withBackOff(Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff)).atMost(retryMaxAttempts)                .onFailure()
                .transform(e -> {
                    log.error(
                            "Impossible to store visura document for onboardingId: {}, filename: {}. Error: {}",
                            sanitize(uploadVisuraRequest.getOnboardingId()), sanitize(filename), e.getMessage(), e);
                    return new InternalException(
                            GENERIC_ERROR.getCode(),
                            String.format("Error storing visura document for onboardingId: %s", sanitize(uploadVisuraRequest.getOnboardingId())));
                })
                .replaceWithVoid();
    }

  @Override
  public Uni<String> uploadSignedContract(String onboardingId, DocumentBuilderRequest request, boolean skipSignatureVerification,
      InputStream fileUpload, String fileName) {

    log.info(
        "START - Uploading and verifying signed contract for onboardingId={}, productId={}", sanitize(onboardingId), sanitize(request.getProductId()));

    return Uni.createFrom()
        .item(
            () -> {
              try (fileUpload) {
                Path tempPath = Files.createTempFile("signed-", "-" + fileName);
                Files.copy(fileUpload, tempPath, StandardCopyOption.REPLACE_EXISTING);
                return tempPath.toFile();
              } catch (IOException e) {
                throw new RuntimeException("Error during signed file creation", e);
              }
            })
        .runSubscriptionOn(
            Infrastructure.getDefaultWorkerPool())
        .chain(
            physicalFile ->
                documentService
                    .handleContractDocument(request)
                    .onFailure()
                    .retry()
                    .withBackOff(
                        Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff))
                    .atMost(retryMaxAttempts)
                    .call(
                        document ->
                            signatureService.verifyContractSignature(
                                onboardingId,
                                physicalFile,
                                request.getFiscalCodes(),
                                skipSignatureVerification))
                    .chain(document -> uploadToAzureAndUpdateDb(document, physicalFile, fileName))
                    .onTermination()
                    .invoke(physicalFile::delete));
  }

  // ==================== Private Reactive I/O isolation methods ====================

  private Uni<File> fetchPdfFromAzureAsync(
      Document document, String onboardingId, boolean isSigned) {
        return Uni.createFrom().item(() -> {
                    String filePath = isSigned
                            ? document.getContractSigned()
                            : DocumentFileUtils.getContractNotSigned(onboardingId, documentMsConfig.getContractPath(), document.getContractFilename());

                    return azureBlobClient.getFileAsPdf(filePath);
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private Uni<Void> verifyAttachmentDoesNotExist(DocumentBuilderRequest request) {
        return documentService.existsAttachment(request.getOnboardingId(), request.getAttachmentName())
                .onItem().transformToUni(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Uni.createFrom().failure(
                                new UpdateNotAllowedException(
                                        ATTACHMENT_UPLOAD_ERROR.getCode(),
                                        ATTACHMENT_UPLOAD_ERROR.getMessage()));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private String performSecurityValidationsAndGetDigest(DocumentBuilderRequest request, FormItem file) {
        File fileToUpload = file.getFile();
        DocumentFileUtils.validateUploadedFile(fileToUpload);

        if (signatureService.isSignatureVerificationEnabled()) {
            signatureService.verifySignature(fileToUpload);
        }

        String templateDigest = getTemplateDigest(request.getTemplatePath());
        return signatureService.verifyUploadedFileDigest(file, templateDigest, false);
    }

    private Uni<Void> uploadToAzureReactive(Document document, FormItem file) {
        return Uni.createFrom().voidItem()
                .invoke(() -> uploadFileToAzure(
                        document.getContractFilename(),
                        document.getOnboardingId(),
                        file.getFile()
                ))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public String getTemplateDigest(String documentTemplatePath) {
        log.info("Retrieving template and computing digest (templatePath={})", sanitize(documentTemplatePath));
        Objects.requireNonNull(documentTemplatePath, "Document template path must not be null");

        File templateFile = azureBlobClient.getFileAsPdf(documentTemplatePath);
        DSSDocument templateDocument = new FileDocument(templateFile);

        DSSDocument templatePdf = signatureService.extractPdfFromSignedContainer(
                SignedDocumentValidator.fromDocument(templateDocument),
                templateDocument
        );

        SignedDocumentValidator templatePdfValidator = SignedDocumentValidator.fromDocument(templatePdf);
        String templateDigest = signatureService.computeDigestOfSignedRevision(templatePdfValidator, templatePdf);

        log.debug("Template content digest (base64): {}", templateDigest);
        return templateDigest;
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
        document.setAttachmentName(request.getAttachmentName());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        String signedContractFileName = extractFileName(request.getTemplatePath());
        String filename = String.format("signed_%s", signedContractFileName);
        if (isP7M) {
            filename = String.format("%s.p7m", filename);
        }
        document.setContractFilename(filename);
        document.setContractSigned(DocumentFileUtils.getAttachmentByOnboarding(request.getOnboardingId(), documentMsConfig.getContractPath(), filename));

        return documentRepository.persist(document).replaceWith(document);
    }

    private void uploadFileToAzure(String filename, String onboardingId, File signedFile) throws InternalException {
        final String path = String.format("%s%s", pathContracts, onboardingId).concat("/attachments");

        try {
            DocumentFileUtils.validateUploadedFile(signedFile);
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(signedFile.toPath()));
        } catch (IOException e) {
            throw new InternalException(GENERIC_ERROR.getCode(),
                    "Error on upload contract for onboarding with id " + onboardingId);
        }
    }

    // ==================== Common utility methods ====================

    private Uni<CreatePdfResponse> uploadAndBuildResponse(PdfContext ctx, String documentType) {
        return Uni.createFrom().item(() -> {
            try {
                azureBlobClient.uploadFile(ctx.storagePath, ctx.filename, Files.readAllBytes(ctx.pdfFile.toPath()));
                return CreatePdfResponse.builder()
                        .storagePath(ctx.storagePath + "/" + ctx.filename)
                        .filename(ctx.filename)
                        .build();
            } catch (IOException e) {
                throw new InternalException(
                        String.format("Cannot upload %s PDF, message: %s", documentType, e.getMessage()), "0032");
            }
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    // ==================== Contract-specific methods ====================


    private Uni<PdfContext> buildContractContextAsync(ContractPdfRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                File pdfFile = DocumentFileUtils.isPdfFile(request.getContractTemplatePath())
                        ? azureBlobClient.getFileAsPdf(request.getContractTemplatePath())
                        : pdfGenerationService.generateContractPdf(
                        azureBlobClient.getFileAsText(request.getContractTemplatePath()),
                        request);

                String filename = DocumentFileUtils.buildFilename(PDF_FORMAT_FILENAME, request.getProductName(), null);
                String storagePath = DocumentFileUtils.buildContractStoragePath(request.getOnboardingId(), documentMsConfig.getContractPath());
                return new PdfContext(pdfFile, filename, storagePath);
            } catch (IOException e) {
                throw new InternalException(String.format("Cannot create contract PDF, message: %s", e.getMessage()), "0031");
            }
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private Uni<PdfContext> buildAttachmentContextAsync(AttachmentPdfRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                String filename = DocumentFileUtils.buildFilename("%s", request.getProductName(), request.getAttachmentName());
                File pdfFile = DocumentFileUtils.isPdfFile(request.getAttachmentTemplatePath())
                        ? azureBlobClient.getFileAsPdf(request.getAttachmentTemplatePath())
                        : pdfGenerationService.generateAttachmentPdf(
                        azureBlobClient.getFileAsText(request.getAttachmentTemplatePath()),
                        request,
                        filename);

                String storagePath = DocumentFileUtils.buildAttachmentStoragePath(request.getOnboardingId(), documentMsConfig.getContractPath());
                return new PdfContext(pdfFile, filename, storagePath);
            } catch (IOException e) {
                throw new InternalException(String.format("Cannot create attachment PDF, message: %s", e.getMessage()), "0033");
            }
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private Uni<PdfContext> signPdfContextAsync(PdfContext ctx, ContractPdfRequest request) {
        return signatureService.signDocument(
                ctx.pdfFile,
                request.getInstitution().getDescription(),
                request.getProductId()
        ).map(signedFile -> new PdfContext(signedFile, ctx.filename, ctx.storagePath));
    }

    private String deleteFileFromAzure(String filePath, String basePath) throws IOException {
        File temporaryFile = azureBlobClient.retrieveFile(filePath);
        String deletedFileName = filePath.replace(basePath, documentMsConfig.getDeletePath());

        try {
            azureBlobClient.uploadFilePath(deletedFileName, Files.readAllBytes(temporaryFile.toPath()));
            azureBlobClient.removeFile(filePath);
            return deletedFileName;
        } finally {
            if (temporaryFile != null && temporaryFile.exists()) {
                boolean isDeleted = temporaryFile.delete();
                if (!isDeleted) {
                    log.warn("Unable to delete local temporary file: {}", sanitize(temporaryFile.getAbsolutePath()));
                }
            }
        }
    }

    private File validateAndExtractSignedFile(File contract, String contractSignedPath) {
        if (contractSignedPath.endsWith(".pdf")) {
            DocumentFileUtils.isPdfValid(contract);
            return contract;
        } else {
            isP7mValid(contract);
            File extractedFile = signatureService.extractFile(contract);
            DocumentFileUtils.isPdfValid(extractedFile);
            return extractedFile;
        }
    }

    private Uni<File> fetchFileFromAzureAsync(String filePath) {
        return Uni.createFrom().item(() -> azureBlobClient.retrieveFile(filePath))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private RestResponse<File> buildDownloadResponse(File file, Document document, boolean isSigned) {
        String filename = getCurrentContractName(document, isSigned);
        return RestResponse.ResponseBuilder.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename)
                .build();
    }

    private record PdfContext(File pdfFile, String filename, String storagePath) {
    }

    // ==========================================
    // METODI DI UTILITÀ PER IL ROLLBACK
    // ==========================================

    private Uni<Void> rollbackDeletedAzureFiles(String currentSignedPath, String originalSignedPath,
                                                String currentContractPath, String originalContractPath) {
        return Uni.createFrom().item(() -> {
            try {
                log.info("Rolling back files to original paths...");
                restoreFileInAzure(currentSignedPath, originalSignedPath);
                restoreFileInAzure(currentContractPath, originalContractPath);
                log.info("Rollback completed successfully.");
            } catch (Exception e) {
                log.error("CRITICAL ERROR: Rollback failed! Azure is out of sync with MongoDB.", e);
            }
            return null;
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).replaceWithVoid();
    }

    /**
     * Helper per gestire l'upload fisico su Azure, l'aggiornamento su Mongo e l'eventuale Rollback.
     */
    private Uni<String> uploadToAzureAndUpdateDb(Document document, File physicalFile, String originalFileName) {
        String onboardingId = document.getOnboardingId();

        DocumentFileUtils.validateUploadedFile(physicalFile);

        String extension = DocumentFileUtils.getFileExtension(originalFileName);
        String baseName = Optional.ofNullable(document.getContractFilename()).orElse(onboardingId);
        String signedName = "signed_" + DocumentFileUtils.replaceFileExtension(baseName, extension);
        String azurePath = documentMsConfig.getContractPath() + onboardingId;

        return Uni.createFrom().item(() -> {
                    try {
                        return azureBlobClient.uploadFile(azurePath, signedName, Files.readAllBytes(physicalFile.toPath()));
                    } catch (IOException e) {
                        throw new RuntimeException("Error uploading signed contract to Azure", e);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onFailure().retry().withBackOff(Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff)).atMost(retryMaxAttempts)

                .chain(uploadedPath -> {
                    document.setContractSigned(uploadedPath);
                    document.setContractFilename(baseName);

                    return documentService.updateDocumentContractFiles(document)
                            .onFailure().retry().withBackOff(Duration.ofMillis(retryMinBackoff), Duration.ofMillis(retryMaxBackoff)).atMost(retryMaxAttempts)
                            .onFailure().call(dbError -> {
                                log.error("DB update failed for onboardingId {}. Rolling back Azure upload: {}",
                                        sanitize(onboardingId), uploadedPath);
                                return rollbackAzureUpload(uploadedPath);
                            })
                            .replaceWith(uploadedPath);
                });
    }

    /**
     * Transazione di compensazione (Saga Pattern) per l'UPLOAD:
     * elimina il file orfano da Azure se il DB va in errore.
     */
    private Uni<Void> rollbackAzureUpload(String uploadedPath) {
        return Uni.createFrom().item(() -> {
            try {
                azureBlobClient.removeFile(uploadedPath);
                log.info("Rollback completed successfully: deleted orphan file {}", uploadedPath);
            } catch (Exception e) {
                log.error("CRITICAL ERROR: Rollback failed! Orphan file left in Azure at path: {}", uploadedPath, e);
            }
            return null;
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).replaceWithVoid();
    }

    private void restoreFileInAzure(String currentPath, String originalPath) throws IOException {
        if (currentPath == null || originalPath == null) return;

        File temporaryFile = azureBlobClient.retrieveFile(currentPath);
        try {
            azureBlobClient.uploadFilePath(originalPath, Files.readAllBytes(temporaryFile.toPath()));
            azureBlobClient.removeFile(currentPath);
        } finally {
            if (temporaryFile != null && temporaryFile.exists()) {
                if (!temporaryFile.delete()) {
                    log.warn("Unable to delete temporary local file during rollback: {}", sanitize(temporaryFile.getAbsolutePath()));
                }
            }
        }
    }
}
