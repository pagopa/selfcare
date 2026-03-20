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
import java.time.Duration;
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
                .onFailure().retry().withBackOff(Duration.ofMillis(500), Duration.ofSeconds(2)).atMost(3)
                .onItem().transformToUni(document ->
                        fetchFileFromAzureAsync(document.getContractSigned())
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
                .onFailure().retry().withBackOff(Duration.ofMillis(500), Duration.ofSeconds(2)).atMost(3)
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
                .onFailure().retry().withBackOff(Duration.ofMillis(500), Duration.ofSeconds(2)).atMost(3)
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

    private String buildAndValidateContractFilePath(String fileName, boolean absolutePath) {
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidRequestException("Invalid fileName");
        }

        String trimmed = fileName.trim();

        if (trimmed.contains("..") || trimmed.contains("\\") || trimmed.startsWith("/")) {
            throw new InvalidRequestException("Invalid fileName");
        }

        String basePath = documentMsConfig.getContractPath();
        if (absolutePath) {
            return trimmed;
        } else {
            String fullPath = basePath + trimmed;
            if (!fullPath.startsWith(basePath)) {
                throw new InvalidRequestException("Invalid fileName");
            }
            return fullPath;
        }
    }

    @Override
    public Uni<Void> uploadAttachment(DocumentBuilderRequest request, FormItem file) {
        log.info("Uploading attachment for onboardingId={}, productId={}, documentName={}",
                sanitize(request.getOnboardingId()),
                sanitize(request.getProductId()),
                sanitize(file.getFileName()));

        return verifyAttachmentDoesNotExist(request)
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .chain(() -> {
                    String uploadedDigest = performSecurityValidationsAndGetDigest(request, file);
                    boolean isP7M = isP7MFile(file);

                    // 1. Salvataggio su DB
                    return persistAttachment(request, uploadedDigest, isP7M);
                })
                // 2. Upload asincrono su Azure
                .call(document -> uploadToAzureReactive(document, file)
                        .onFailure().call(azureError -> {
                            log.error("Upload to Azure failed for attachment {}. Rolling back DB record...", request.getDocumentName());
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
                    String originalSignedPath = buildAndValidateContractFilePath(document.getContractSigned(), true);
                    String originalContractPath = buildAndValidateContractFilePath(onboardingId + "/" + document.getContractFilename(), false);

                    String deletedSignedContract;
                    String deletedContractFile;

                    try {
                        deletedSignedContract = deleteFileFromAzure(originalSignedPath);
                        deletedContractFile = deleteFileFromAzure(originalContractPath);
                    } catch (Exception e) {
                        log.error("Error deleting contract files from Azure for onboardingId {}: {}", sanitize(onboardingId), e.getMessage());
                        return Uni.createFrom().failure(new RuntimeException("Error deleting contract files from Azure", e));
                    }

                    document.setContractSigned(deletedSignedContract);
                    document.setContractFilename(deletedContractFile);

                    return documentService.updateDocumentContractFiles(document)
                            .onFailure().retry().withBackOff(Duration.ofMillis(500), Duration.ofSeconds(2)).atMost(3)
                            .onFailure().call(dbError -> {
                                log.error("DB update failed for onboardingId {}. Triggering Azure Rollback...", onboardingId);
                                return rollbackAzureFiles(deletedSignedContract, originalSignedPath, deletedContractFile, originalContractPath);
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
                        azureBlobClient.uploadFile(path, filename, Files.readAllBytes(csvFile.toPath()));
                    } catch (IOException e) {
                        log.error("Error reading from file {} ", sanitize(path), e);
                        // Rilanciamo l'errore per far scattare il Retry
                        throw new RuntimeException("Error during Azure upload", e);
                    }
                })
                .onFailure().retry().withBackOff(Duration.ofMillis(500), Duration.ofSeconds(2)).atMost(3)
                .onFailure()
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
                        azureBlobClient.uploadFile(path, filename, Files.readAllBytes(file.toPath()));
                    } catch (IOException e) {
                        log.error("Error reading from file {} ", sanitize(path), e);
                        // Rilanciamo l'errore per far scattare il Retry
                        throw new RuntimeException("Error during Azure upload", e);
                    }
                })
                .onFailure().retry().withBackOff(Duration.ofMillis(500), Duration.ofSeconds(2)).atMost(3)
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

    // ==================== Private Reactive I/O isolation methods ====================

    private Uni<File> fetchPdfFromAzureAsync(Document document, String onboardingId, boolean isSigned) {
        return Uni.createFrom().item(() -> {
                    String filePath = isSigned
                            ? document.getContractSigned()
                            : getContractNotSigned(onboardingId, document);

                    return azureBlobClient.getFileAsPdf(filePath);
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());
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

    // ==================== Common utility methods ====================

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

        setupProductSpecificData(data, request);

        log.debug("Building PDF template context: dataMap keys={}, size={}", data.keySet(), data.size());
        return PdfBuilder.generateDocument("_contratto_interoperabilita.", contractTemplateText, data);
    }

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
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
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
        }).runSubscriptionOn(Infrastructure.getDefaultExecutor());
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
            isPdfValid(contract);
            return contract;
        } else {
            isP7mValid(contract);
            File extractedFile = signatureService.extractFile(contract);
            isPdfValid(extractedFile);
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

    private File createPdfFileAttachment(AttachmentPdfRequest request) throws IOException {
        String attachmentTemplateText = azureBlobClient.getFileAsText(request.getAttachmentTemplatePath());
        Map<String, Object> data = PdfMapperData.setUpAttachmentData(request);

        log.debug("Building PDF attachment template context: dataMap keys={}, size={}", data.keySet(), data.size());
        String filename = buildFilename("%s", request.getProductName(), request.getAttachmentName());
        return PdfBuilder.generateDocument(filename, attachmentTemplateText, data);
    }

    @FunctionalInterface
    private interface PdfGenerator {
        File generate() throws IOException;
    }

    private record PdfContext(File pdfFile, String filename, String storagePath) {
    }

    // ==========================================
    // METODI DI UTILITÀ PER IL ROLLBACK
    // ==========================================

    private Uni<Void> rollbackAzureFiles(String currentSignedPath, String originalSignedPath,
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
}