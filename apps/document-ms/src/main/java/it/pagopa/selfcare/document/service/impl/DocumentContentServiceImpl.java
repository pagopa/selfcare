package it.pagopa.selfcare.document.service.impl;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.config.DocumentMsConfig;
import it.pagopa.selfcare.document.exception.InternalException;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.document.service.DocumentContentService;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.document.service.SignatureService;
import it.pagopa.selfcare.document.util.PdfBuilder;
import it.pagopa.selfcare.document.util.PdfMapperData;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.selfcare.document.util.ErrorMessage.*;
import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.document.util.Utils.*;
import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;

/**
 * Implementation of DocumentContentService for creating, retrieving, and managing PDF documents.
 * It integrates with Azure Blob Storage for persistence and DSS for digital signatures.
 * The class uses a reactive approach (Mutiny), isolating blocking I/O and CPU-bound tasks
 * to worker threads to prevent Quarkus Event Loop starvation.
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

    @ConfigProperty(name = "document-ms.blob-storage.path-contracts")
    String pathContracts;

    @Inject
    public DocumentContentServiceImpl(
            AzureBlobClient azureBlobClient,
            DocumentMsConfig documentMsConfig,
            SignatureService signatureService,
            DocumentRepository documentRepository,
            DocumentService documentService) {
        this.azureBlobClient = azureBlobClient;
        this.documentMsConfig = documentMsConfig;
        this.signatureService = signatureService;
        this.documentRepository = documentRepository;
        this.documentService = documentService;
    }

    /**
     * Orchestrates the reactive flow to create, sign, and upload a new contract PDF.
     */
    @Override
    public Uni<CreatePdfResponse> createContractPdf(ContractPdfRequest request) {
        log.info("START - createContractPdf for template: {} with onboardingId: {}",
                sanitize(request.getContractTemplatePath()), sanitize(request.getOnboardingId()));

        return buildContractContextAsync(request)
                // Wait for the document to be signed before proceeding to upload
                .chain(ctx -> signPdfContextAsync(ctx, request))
                .chain(ctx -> uploadAndBuildResponse(ctx, "contract"));
    }

    /**
     * Orchestrates the reactive flow to create and upload a new attachment PDF.
     * Unlike contracts, attachments are not automatically signed during creation.
     */
    @Override
    public Uni<CreatePdfResponse> createAttachmentPdf(AttachmentPdfRequest request) {
        log.info("START - createAttachmentPdf for template: {} with onboardingId: {}",
                sanitize(request.getAttachmentTemplatePath()), sanitize(request.getOnboardingId()));

        return buildAttachmentContextAsync(request)
                .chain(ctx -> uploadAndBuildResponse(ctx, "attachment"));
    }

    /**
     * Retrieves a signed file from Azure, verifying its validity (PDF or P7M).
     */
    @Override
    public Uni<RestResponse<File>> retrieveSignedFile(String onboardingId) {
        return documentRepository.findByOnboardingId(onboardingId)
                .onItem().transformToUni(document ->
                        fetchFileFromAzureAsync(document.getContractSigned())
                                // Offload CPU-bound validation (P7M extraction/PDF parsing) to the worker pool
                                .emitOn(Infrastructure.getDefaultWorkerPool())
                                .onItem().transform(contract -> validateAndExtractSignedFile(contract, document.getContractSigned()))
                                .onItem().transform(processedFile -> buildDownloadResponse(processedFile, document, true))
                )
                .onFailure().recoverWithItem(() -> RestResponse.ResponseBuilder.<File>notFound().build());
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

    private void isP7mValid(File contract) {
        signatureService.verifySignature(contract);
    }

    @Override
    public Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned) {
        return documentRepository.findByOnboardingId(onboardingId)
                .onItem().transformToUni(document ->
                        fetchPdfFromAzureAsync(document, onboardingId, isSigned)
                                .onItem().transform(contractFile -> buildDownloadResponse(contractFile, document, isSigned))
                );
    }

    private String getContractNotSigned(String onboardingId, Document document) {
        return String.format("%s%s/%s", documentMsConfig.getContractPath(), onboardingId,
                document.getContractFilename());
    }

    @Override
    public Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String templatePath,
                                                              String attachmentName, String institutionDescription,
                                                              String productId) {
        return Uni.createFrom()
                .item(() -> azureBlobClient.getFileAsPdf(templatePath))
                // Offloads Azure Blob sync client to avoid blocking the Event Loop
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

    private String buildAttachmentPath(Document document) {
        return Objects.nonNull(document.getContractSigned()) ? document.getContractSigned() : getAttachmentByOnboarding(document.getOnboardingId(), document.getContractFilename());
    }

    private String getAttachmentByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s%s", documentMsConfig.getContractPath(), onboardingId, "/attachments", "/" + filename);
    }

    /**
     * Safely builds and validates the contract file path starting from user-provided input.
     * <p>
     * Rejects path traversal attempts and disallows path separators that could escape
     * the expected blob namespace.
     *
     * @param fileName    user-provided file name or blob path
     * @param absolutePath if true, {@code fileName} is treated as a full blob path; otherwise it is
     *                     appended to the configured contractPath.
     * @return a validated logical path to be used with Azure Blob Storage
     * @throws InvalidRequestException if the provided name is not acceptable
     */
    private String buildAndValidateContractFilePath(String fileName, boolean absolutePath) {
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidRequestException("Invalid fileName");
        }

        String trimmed = fileName.trim();

        // Basic traversal and separator checks
        if (trimmed.contains("..")
                || trimmed.contains("\\")
                || trimmed.startsWith("/")) {
            throw new InvalidRequestException("Invalid fileName");
        }

        String basePath = documentMsConfig.getContractPath();
        if (absolutePath) {
            // Even for absolute paths we keep the same safety checks above.
            return trimmed;
        } else {
            String fullPath = basePath + trimmed;
            // Simple defensive check to ensure the prefix is preserved.
            if (!fullPath.startsWith(basePath)) {
                throw new InvalidRequestException("Invalid fileName");
            }
            return fullPath;
        }
    }

    /**
     * Uploads an externally provided attachment.
     * Validates the file digest, verifies signatures if enabled, saves metadata to DB, and uploads to Azure.
     */
    @Override
    public Uni<Void> uploadAttachment(DocumentBuilderRequest request, FormItem file) {
        log.info("Uploading attachment for onboardingId={}, productId={}, documentName={}",
                sanitize(request.getOnboardingId()),
                sanitize(request.getProductId()),
                sanitize(file.getFileName()));

        return verifyAttachmentDoesNotExist(request)
                // Shift execution to the worker pool to safely perform CPU-intensive security validations
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .chain(() -> {
                    // 1. Synchronous security validations (digest and signatures)
                    String uploadedDigest = performSecurityValidationsAndGetDigest(request, file);

                    // 2. Extension check
                    boolean isP7M = isP7MFile(file);

                    // 3. Persist metadata to database
                    return persistAttachment(request, uploadedDigest, isP7M);
                })
                // 4. Trigger the asynchronous Azure upload
                .call(document -> uploadToAzureReactive(document, file))
                .replaceWithVoid();
    }

    @Override
    public Uni<String> deleteContract(String onboardingId) {
        log.info("START - deleteContract for onboardingId: {}", sanitize(onboardingId));

        return documentService.getDocumentInstitutionByOnboardingId(onboardingId)
                // 1. VIGILE URBANO: Spostiamo il flusso sul Worker Pool per l'I/O bloccante
                .emitOn(Infrastructure.getDefaultWorkerPool())

                // 2. CHAIN: Usiamo chain perché alla fine dobbiamo restituire un altro Uni (quello del DB)
                .chain(document -> {
                    try {
                        // Recupera i percorsi dei file contrattuali
                        String signedContractPath = buildAndValidateContractFilePath(document.getContractSigned(), true);
                        String contractFilePath = buildAndValidateContractFilePath(onboardingId + "/" + document.getContractFilename(), false);

                        // Operazioni I/O Bloccanti (Sicure grazie all'emitOn)
                        String deletedSignedContract = deleteFileFromAzure(signedContractPath);
                        String deletedContractFile = deleteFileFromAzure(contractFilePath);

                        // Aggiorna l'oggetto in memoria
                        document.setContractSigned(deletedSignedContract);
                        document.setContractFilename(deletedContractFile);

                        // 3. RESTITUIAMO L'UNI DEL DB!
                        // Ora Mutiny aspetterà che l'update finisca prima di procedere
                        return documentService.updateDocumentContractFiles(document);

                    } catch (Exception e) {
                        log.error("Error during deleteContract for onboardingId {}: {}", sanitize(onboardingId), e.getMessage());
                        // Nel blocco chain, se c'è un errore, dobbiamo restituire un Uni.failure
                        return Uni.createFrom().failure(new RuntimeException("Error deleting contract files from Azure", e));
                    }
                })
                // 4. MAPPATURA FINALE: Quando l'update su DB ha finito (restituendo un Long),
                // noi ignoriamo il Long e restituiamo la stringa di successo
                .replaceWith("Contract deleted successfully");
    }
 /*
    @Override
    public Uni<String> deleteContract(String onboardingId) {
        log.info("START - deleteContract for onboardingId: {}", sanitize(onboardingId));

        return documentService.getDocumentInstitutionByOnboardingId(onboardingId)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .chain(document -> {
                    // 1. Salviamo i percorsi ORIGINALI prima di modificarli (ci serviranno per il rollback)
                    String originalSignedPath = buildAndValidateContractFilePath(document.getContractSigned(), true);
                    String originalContractPath = buildAndValidateContractFilePath(onboardingId + "/" + document.getContractFilename(), false);

                    String deletedSignedContract;
                    String deletedContractFile;

                    try {
                        // 2. Operazioni I/O su Azure
                        deletedSignedContract = deleteFileFromAzure(originalSignedPath);
                        deletedContractFile = deleteFileFromAzure(originalContractPath);
                    } catch (Exception e) {
                        log.error("Error deleting contract files from Azure for onboardingId {}: {}", sanitize(onboardingId), e.getMessage());
                        return Uni.createFrom().failure(new RuntimeException("Error deleting contract files from Azure", e));
                    }

                    // 3. Aggiorniamo l'oggetto in memoria
                    document.setContractSigned(deletedSignedContract);
                    document.setContractFilename(deletedContractFile);

                    // 4. Eseguiamo l'update su DB
                    return documentService.updateDocumentContractFiles(document)
                            // 🚨 LA MAGIA DEL ROLLBACK: Se il DB fallisce, intercettiamo l'errore!
                            .onFailure().call(dbError -> {
                                log.error("DB update failed for onboardingId {}. Triggering Azure Rollback...", onboardingId);
                                // Chiamiamo il metodo di compensazione asincrono
                                return rollbackAzureFiles(deletedSignedContract, originalSignedPath, deletedContractFile, originalContractPath);
                            });
                })
                .replaceWith("Contract deleted successfully");
    }
*/
// ==========================================
// METODI DI UTILITÀ PER IL ROLLBACK
// ==========================================

    /**
     * Metodo reattivo che orchestra il rollback.
     */

    /*
    private Uni<Void> rollbackAzureFiles(String currentSignedPath, String originalSignedPath,
                                         String currentContractPath, String originalContractPath) {
        return Uni.createFrom().item(() -> {
            try {
                log.info("Rolling back files to original paths...");
                // Spostiamo i file indietro dalla cartella "deleted" a quella "contracts"
                restoreFileInAzure(currentSignedPath, originalSignedPath);
                restoreFileInAzure(currentContractPath, originalContractPath);
                log.info("Rollback completed successfully.");
            } catch (Exception e) {
                // Se fallisce anche il rollback, siamo nel "Worst Case Scenario".
                // Registriamo un ALLARME CRITICO nei log, spesso intercettato da sistemi come Datadog/Kibana.
                log.error("CRITICAL ERROR: Rollback failed! Azure is out of sync with MongoDB.", e);
            }
            return null;
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).replaceWithVoid();
    }

    /**
     * Operazione bloccante che fa l'esatto inverso di deleteFileFromAzure
     */

    /*
    private void restoreFileInAzure(String currentPath, String originalPath) throws IOException {
        if (currentPath == null || originalPath == null) return;

        File temporaryFile = azureBlobClient.retrieveFile(currentPath);
        try {
            azureBlobClient.uploadFilePath(originalPath, Files.readAllBytes(temporaryFile.toPath()));
            azureBlobClient.removeFile(currentPath);
        } finally {
            if (temporaryFile != null && temporaryFile.exists()) {
                if (!temporaryFile.delete()) {
                    log.warn("Impossibile eliminare il file temporaneo locale durante il rollback: {}", temporaryFile.getAbsolutePath());
                }
            }
        }
    }
 */
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
                        azureBlobClient.uploadFile(path, filename, Files.readAllBytes(csvFile.toPath()));
                    } catch (IOException e) {
                        log.error("Error reading from file {} ", sanitize(path), e);
                    }
                })
                .onFailure()
                .transform(e -> {
                    log.error(
                            "Impossible to store csv aggregate for onboardingId: {}, filename: {}. Error: {}",
                            sanitize(request.getOnboardingId()), sanitize(filename), e.getMessage(), e);
                    return new InternalException(
                            GENERIC_ERROR.getCode(),
                            String.format("Error storing csv aggregate for onboardingId: %s", sanitize(request.getOnboardingId())));
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .replaceWithVoid();
    }

    // ==================== Private Reactive I/O isolation methods ====================

    // Handles ONLY the asynchronous retrieval from Azure, isolating the blocking I/O
    private Uni<File> fetchPdfFromAzureAsync(Document document, String onboardingId, boolean isSigned) {
        return Uni.createFrom().item(() -> {
                    String filePath = isSigned
                            ? document.getContractSigned()
                            : getContractNotSigned(onboardingId, document);

                    return azureBlobClient.getFileAsPdf(filePath);
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor()); // Safe for Event Loop
    }

    private Uni<Void> verifyAttachmentDoesNotExist(DocumentBuilderRequest request) {
        return documentService.existsAttachment(request.getOnboardingId(), request.getDocumentName())
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

    // Groups all synchronous security validations and returns the file digest
    private String performSecurityValidationsAndGetDigest(DocumentBuilderRequest request, FormItem file) {
        File fileToUpload = file.getFile();
        validateUploadedFile(fileToUpload);

        if (signatureService.isSignatureVerificationEnabled()) {
            signatureService.verifySignature(fileToUpload);
        }

        String templateDigest = getTemplateDigest(request.getTemplatePath());
        return signatureService.verifyUploadedFileDigest(file, templateDigest, false);
    }

    private boolean isP7MFile(FormItem file) {
        return Optional.ofNullable(file.getFileName())
                .map(name -> name.toLowerCase(Locale.ROOT).endsWith(".p7m"))
                .orElse(false);
    }

    // Encapsulates the blocking Azure upload call in an isolated Uni
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

    private void uploadFileToAzure(String filename, String onboardingId, File signedFile) throws InternalException {
        final String path = String.format("%s%s", pathContracts, onboardingId).concat("/attachments");

        try {
            validateUploadedFile(signedFile);
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(signedFile.toPath()));
        } catch (IOException e) {
            throw new InternalException(GENERIC_ERROR.getCode(),
                    "Error on upload contract for onboarding with id " + onboardingId);
        }
    }

    /**
     * Validate that the uploaded file is a regular file located under the system temporary directory.
     * This helps prevent using attacker-controlled paths for reading local files (Path Traversal prevention).
     *
     * @param file the file to validate
     */
    private void validateUploadedFile(File file) {
        if (file == null) {
            throw new InvalidRequestException("Uploaded file must not be null", "0000");
        }

        Path filePath = file.toPath().toAbsolutePath().normalize();
        Path tempDirPath = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();

        if (!filePath.startsWith(tempDirPath)) {
            throw new InvalidRequestException("Invalid uploaded file location", "0000");
        }

        if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
            throw new InvalidRequestException("Uploaded file does not exist or is not a regular file", "0000");
        }
    }

    @Override
    public Uni<Void> saveVisuraForMerchant(UploadVisuraRequest uploadVisuraRequest) {
        final String filename = uploadVisuraRequest.getFilename();
        final String path = String.format("%s%s/visura", documentMsConfig.getContractPath(), uploadVisuraRequest.getOnboardingId());

        return Uni.createFrom().item(uploadVisuraRequest::getFileContent)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .invoke(file -> {
                    try {
                        azureBlobClient.uploadFile(path, filename, Files.readAllBytes(file.toPath()));
                    } catch (IOException e) {
                        log.error("Error reading from file {} ", sanitize(path), e);
                    }
                })
                .onFailure()
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
    // ==================== Common utility methods ====================

    /**
     * Loads an existing PDF from storage or generates a new one from template.
     */
    private File loadOrGeneratePdf(String templatePath, PdfGenerator generator) throws IOException {
        return isPdfFile(templatePath)
                ? azureBlobClient.getFileAsPdf(templatePath)
                : generator.generate();
    }

    private String buildFilename(String format, String productName, String attachmentName) {
        if (Objects.nonNull(attachmentName)) {
            return CONTRACT_FILENAME_FUNC.apply(String.format("%s_%s.pdf", format, attachmentName), productName);
        }
        return CONTRACT_FILENAME_FUNC.apply(format, productName);
    }

    private String buildContractStoragePath(String onboardingId) {
        return String.format("%s%s", documentMsConfig.getContractPath(), onboardingId);
    }

    private String buildAttachmentStoragePath(String onboardingId) {
        return String.format("%s%s/attachments", documentMsConfig.getContractPath(), onboardingId);
    }

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

    private boolean isPdfFile(String path) {
        return path != null && path.endsWith(".pdf");
    }

    // ==================== Contract-specific methods ====================

    private File createPdfFileContract(ContractPdfRequest request) throws IOException {
        String contractTemplateText = azureBlobClient.getFileAsText(request.getContractTemplatePath());
        Map<String, Object> data = PdfMapperData.setUpCommonData(request);

        // Populate the data map based on specific PagoPA product business rules
        setupProductSpecificData(data, request);

        log.debug("Building PDF template context: dataMap keys={}, size={}", data.keySet(), data.size());
        return PdfBuilder.generateDocument("_contratto_interoperabilita.", contractTemplateText, data);
    }

    /**
     * Routes the PDF data mapping based on the combination of Product ID and Institution Type.
     * Different institution types (e.g., PSP vs EC) or products (e.g., IO vs PagoPA) require
     * different placeholders to be filled in their respective contract templates.
     */
    private void setupProductSpecificData(Map<String, Object> data, ContractPdfRequest request) {
        String productId = request.getProductId();
        InstitutionPdfData institution = request.getInstitution();
        InstitutionType institutionType = institution.getInstitutionType();

        if (isPspAndPagoPaOrDashboard(productId, institutionType)) {
            PdfMapperData.setupPSPData(data, request.getManager(), request);
        } else if (isPrvOrGpuAndPagoPaOrIdpay(productId, institutionType)) {
            PdfMapperData.setupPRVData(data, request);
            if (Objects.nonNull(request.getPayment())) {
                PdfMapperData.setupPaymentData(data, request.getPayment());
            }
        } else if (isEcAndPagoPa(productId, institutionType)) {
            PdfMapperData.setECData(data, institution);
        } else if (isProdIO(productId)) {
            PdfMapperData.setupProdIOData(request, data, request.getManager());
        } else if (PROD_PN.getValue().equalsIgnoreCase(productId)) {
            PdfMapperData.setupProdPNData(data, institution, request.getBilling());
        } else if (PROD_INTEROP.getValue().equalsIgnoreCase(productId)) {
            PdfMapperData.setupSAProdInteropData(data, institution);
        }
    }

    private boolean isPspAndPagoPaOrDashboard(String productId, InstitutionType institutionType) {
        return (PROD_PAGOPA.getValue().equalsIgnoreCase(productId) || PROD_DASHBOARD_PSP.getValue().equalsIgnoreCase(productId))
                && InstitutionType.PSP == institutionType;
    }

    private boolean isPrvOrGpuAndPagoPaOrIdpay(String productId, InstitutionType institutionType) {
        return (PROD_PAGOPA.getValue().equalsIgnoreCase(productId) || PROD_IDPAY_MERCHANT.getValue().equalsIgnoreCase(productId))
                && (InstitutionType.PRV == institutionType || InstitutionType.GPU == institutionType || InstitutionType.PRV_PF == institutionType);
    }

    private boolean isEcAndPagoPa(String productId, InstitutionType institutionType) {
        return PROD_PAGOPA.getValue().equalsIgnoreCase(productId)
                && InstitutionType.PSP != institutionType
                && InstitutionType.PT != institutionType;
    }

    private boolean isProdIO(String productId) {
        return PROD_IO.getValue().equalsIgnoreCase(productId)
                || PROD_IO_PREMIUM.getValue().equalsIgnoreCase(productId)
                || PROD_IO_SIGN.getValue().equalsIgnoreCase(productId);
    }

    private Uni<PdfContext> buildContractContextAsync(ContractPdfRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                File pdfFile = loadOrGeneratePdf(request.getContractTemplatePath(), () -> createPdfFileContract(request));
                String filename = buildFilename(request.getPdfFormatFilename(), request.getProductName(), null);
                String storagePath = buildContractStoragePath(request.getOnboardingId());
                return new PdfContext(pdfFile, filename, storagePath);
            } catch (IOException e) {
                throw new InternalException(String.format("Cannot create contract PDF, message: %s", e.getMessage()), "0031");
            }
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor()); // Safe for Event Loop
    }

    private Uni<PdfContext> buildAttachmentContextAsync(AttachmentPdfRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                File pdfFile = loadOrGeneratePdf(request.getAttachmentTemplatePath(), () -> createPdfFileAttachment(request));
                String filename = CONTRACT_FILENAME_FUNC.apply("%s_" + request.getAttachmentName() + ".pdf", request.getProductName());
                String storagePath = buildAttachmentStoragePath(request.getOnboardingId());
                return new PdfContext(pdfFile, filename, storagePath);
            } catch (IOException e) {
                throw new InternalException(String.format("Cannot create attachment PDF, message: %s", e.getMessage()), "0033");
            }
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor()); // Safe for Event Loop
    }

    private Uni<PdfContext> signPdfContextAsync(PdfContext ctx, ContractPdfRequest request) {
        return signatureService.signDocument(
                ctx.pdfFile,
                request.getInstitution().getDescription(),
                request.getProductId()
        ).map(signedFile -> new PdfContext(signedFile, ctx.filename, ctx.storagePath));
    }

    private String deleteFileFromAzure(String filePath) throws IOException {
        File temporaryFile = azureBlobClient.retrieveFile(filePath);
        String deletedFileName = filePath.replace(documentMsConfig.getContractPath(), documentMsConfig.getDeletePath());

        try {
            // Sposta il file nel percorso di eliminazione
            azureBlobClient.uploadFilePath(deletedFileName, Files.readAllBytes(temporaryFile.toPath()));
            azureBlobClient.removeFile(filePath);

            return deletedFileName;
        } finally {
            // Questa parte viene eseguita SEMPRE, sia in caso di successo che di errore.
            if (temporaryFile != null && temporaryFile.exists()) {
                boolean isDeleted = temporaryFile.delete();
                if (!isDeleted) {
                    log.warn("Unable to delete local temporary file: {}", temporaryFile.getAbsolutePath());
                }
            }
        }
    }

    private File validateAndExtractSignedFile(File contract, String contractSignedPath) {
        // Handle normal PDF files
        if (contractSignedPath.endsWith(".pdf")) {
            isPdfValid(contract);
            return contract;
        } else {
            // Handle PKCS #7 detached signatures (.p7m)
            isP7mValid(contract);
            File extractedFile = signatureService.extractFile(contract);
            isPdfValid(extractedFile);
            return extractedFile;
        }
    }

    // Reusable method for any Azure Blob download
    private Uni<File> fetchFileFromAzureAsync(String filePath) {
        return Uni.createFrom().item(() -> azureBlobClient.retrieveFile(filePath))
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    // Centralizes the creation of HTTP headers for file downloads
    private RestResponse<File> buildDownloadResponse(File file, Document document, boolean isSigned) {
        String filename = getCurrentContractName(document, isSigned);
        return RestResponse.ResponseBuilder.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename)
                .build();
    }

    // ==================== Attachment-specific methods ====================

    private File createPdfFileAttachment(AttachmentPdfRequest request) throws IOException {
        String attachmentTemplateText = azureBlobClient.getFileAsText(request.getAttachmentTemplatePath());
        Map<String, Object> data = PdfMapperData.setUpAttachmentData(request);

        log.debug("Building PDF attachment template context: dataMap keys={}, size={}", data.keySet(), data.size());
        String filename = buildFilename("%s", request.getProductName(), request.getAttachmentName());
        return PdfBuilder.generateDocument(filename, attachmentTemplateText, data);
    }

    // ==================== Inner types ====================

    /**
     * Functional interface for PDF generation.
     */
    @FunctionalInterface
    private interface PdfGenerator {
        File generate() throws IOException;
    }

    /**
     * Internal record to hold PDF context during the reactive processing chain.
     */
    private record PdfContext(File pdfFile, String filename, String storagePath) {}
}
