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
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static it.pagopa.selfcare.document.util.ErrorMessage.ATTACHMENT_UPLOAD_ERROR;
import static it.pagopa.selfcare.document.util.ErrorMessage.GENERIC_ERROR;
import static it.pagopa.selfcare.document.util.ErrorMessage.ORIGINAL_DOCUMENT_NOT_FOUND;
import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.document.util.Utils.*;
import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT;

/**
 * Implementation of DocumentContentService for creating PDF documents.
 */
@Slf4j
@ApplicationScoped
public class DocumentContentServiceImpl implements DocumentContentService {

    private static final BiFunction<String, String, String> CONTRACT_FILENAME_FUNC =
            (prefix, productName) -> prefix + productName.replace(" ", "_");
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
    public Uni<CreatePdfResponse> createContractPdf(CreateContractPdfRequest request) {
        log.info("START - createContractPdf for template: {} with onboardingId: {}",
                sanitize(request.getContractTemplatePath()), sanitize(request.getOnboardingId()));

        return Uni.createFrom().item(() -> {
            try {
                File pdfFile = loadOrGeneratePdf(
                        request.getContractTemplatePath(),
                        () -> createPdfFileContract(request)
                );
                String filename = buildFilename(request.getPdfFormatFilename(), request.getProductName());
                String storagePath = buildContractStoragePath(request.getOnboardingId());

                return new PdfContext(pdfFile, filename, storagePath);
            } catch (IOException e) {
                throw new InternalException(
                        String.format("Cannot create contract PDF, message: %s", e.getMessage()), "0031");
            }
        })
        .runSubscriptionOn(Infrastructure.getDefaultExecutor())
        .chain(ctx -> signatureService.signDocument(
                ctx.pdfFile,
                request.getInstitution().getDescription(),
                request.getProductId()
        ).map(signedFile -> new PdfContext(signedFile, ctx.filename, ctx.storagePath)))
        .chain(ctx -> uploadAndBuildResponse(ctx, "contract"));
    }

    @Override
    public Uni<CreatePdfResponse> createAttachmentPdf(CreateAttachmentPdfRequest request) {
        log.info("START - createAttachmentPdf for template: {} with onboardingId: {}",
                sanitize(request.getAttachmentTemplatePath()), sanitize(request.getOnboardingId()));

        return Uni.createFrom().item(() -> {
            try {
                File pdfFile = loadOrGeneratePdf(
                        request.getAttachmentTemplatePath(),
                        () -> createPdfFileAttachment(request)
                );
                String sanitizedProductName = request.getProductName().replace(" ", "_");
                String filename = sanitizedProductName + "_" + request.getAttachmentName() + ".pdf";
                String storagePath = buildAttachmentStoragePath(request.getOnboardingId());

                return new PdfContext(pdfFile, filename, storagePath);
            } catch (IOException e) {
                throw new InternalException(
                        String.format("Cannot create attachment PDF, message: %s", e.getMessage()), "0033");
            }
        })
        .runSubscriptionOn(Infrastructure.getDefaultExecutor())
        .chain(ctx -> uploadAndBuildResponse(ctx, "attachment"));
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
                                fileToSend = signatureService.extractFile(contract);
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

    private void isP7mValid(File contract) {
        signatureService.verifySignature(contract);
    }

    @Override
    public Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String templatePath,
                                                              String attachmentName, String institutionDescription,
                                                              String productId) {
        return Uni.createFrom()
                .item(() -> azureBlobClient.getFileAsPdf(templatePath))
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException(String.format("Template Attachment not found on storage for onboarding: %s", onboardingId)))
                .chain(file -> signatureService.signDocument(file, institutionDescription, productId))
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

    private String buildAttachmentPath(Document document) {
        return Objects.nonNull(document.getContractSigned()) ? document.getContractSigned() : getAttachmentByOnboarding(document.getOnboardingId(), document.getContractFilename());
    }

    private String getAttachmentByOnboarding(String onboardingId, String filename) {
        return String.format("%s%s%s%s", documentMsConfig.getContractPath(), onboardingId, "/attachments", "/" + filename);
    }

    @Override
    public Uni<Void> uploadAttachment(DocumentBuilderRequest request, FormItem file) {
    log.info(
        "Uploading attachment for onboardingId={}, documentName={}",
        sanitize(request.getOnboardingId()),
        sanitize(request.getDocumentName()));

    return documentService.existsAttachment(request.getOnboardingId(), request.getDocumentName())
        .onItem()
        .transformToUni(
            exists -> {
              if (Boolean.TRUE.equals(exists)) {
                return Uni.createFrom()
                    .failure(
                        new UpdateNotAllowedException(
                            ATTACHMENT_UPLOAD_ERROR.getCode(),
                            ATTACHMENT_UPLOAD_ERROR.getMessage()));
              }
              return Uni.createFrom().voidItem();
            })
        .chain(
            () -> {
              if (signatureService.isSignatureVerificationEnabled()) {
                signatureService.verifySignature(file.getFile());
              }

              String templateDigest = getTemplateDigest(request.getTemplatePath());
              String uploadedDigest =
                  signatureService.verifyUploadedFileDigest(file, templateDigest, false);

              File fileToUpload = file.getFile();

              boolean isP7M =
                  Optional.of(file.getFileName())
                      .map(name -> name.toLowerCase(Locale.ROOT).endsWith(".p7m"))
                      .orElse(false);

              return persistAttachment(request, uploadedDigest, isP7M)
                  .onItem()
                  .invoke(
                      document ->
                          uploadFileToAzure(
                              document.getContractFilename(),
                              document.getOnboardingId(),
                              fileToUpload));
            })
        .replaceWithVoid();
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
            azureBlobClient.uploadFile(path, filename, Files.readAllBytes(signedFile.toPath()));
        } catch (IOException e) {
            throw new InternalException(GENERIC_ERROR.getCode(),
                    "Error on upload contract for onboarding with id " + onboardingId);
        }
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

    /**
     * Builds the filename for the PDF document.
     */
    private String buildFilename(String format, String productName) {
        return CONTRACT_FILENAME_FUNC.apply(format, productName);
    }

    /**
     * Builds the storage path for contract documents.
     */
    private String buildContractStoragePath(String onboardingId) {
        return String.format("%s%s", documentMsConfig.getContractPath(), onboardingId);
    }

    /**
     * Builds the storage path for attachment documents.
     */
    private String buildAttachmentStoragePath(String onboardingId) {
        return String.format("%s%s/attachments", documentMsConfig.getContractPath(), onboardingId);
    }

    /**
     * Uploads the PDF to storage and builds the response.
     */
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

    private File createPdfFileContract(CreateContractPdfRequest request) throws IOException {
        String contractTemplateText = azureBlobClient.getFileAsText(request.getContractTemplatePath());
        Map<String, Object> data = PdfMapperData.setUpCommonData(request);
        setupProductSpecificData(data, request);

        log.debug("Building PDF template context: dataMap keys={}, size={}", data.keySet(), data.size());
        return PdfBuilder.generateDocument("_contratto_interoperabilita.", contractTemplateText, data);
    }

    private void setupProductSpecificData(Map<String, Object> data, CreateContractPdfRequest request) {
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

    // ==================== Attachment-specific methods ====================

    private File createPdfFileAttachment(CreateAttachmentPdfRequest request) throws IOException {
        String attachmentTemplateText = azureBlobClient.getFileAsText(request.getAttachmentTemplatePath());
        Map<String, Object> data = PdfMapperData.setUpAttachmentData(request);

        log.debug("Building PDF attachment template context: dataMap keys={}, size={}", data.keySet(), data.size());
        return PdfBuilder.generateDocument("_allegato_interoperabilita.", attachmentTemplateText, data);
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
     * Internal record to hold PDF context during processing.
     */
    private record PdfContext(File pdfFile, String filename, String storagePath) {}
}
