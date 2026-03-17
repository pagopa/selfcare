package it.pagopa.selfcare.document.service.impl;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.document.config.DocumentMsConfig;
import it.pagopa.selfcare.document.exception.InternalException;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import it.pagopa.selfcare.document.service.DocumentContentService;
import it.pagopa.selfcare.document.service.SignatureService;
import it.pagopa.selfcare.document.util.PdfBuilder;
import it.pagopa.selfcare.document.util.PdfMapperData;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import static it.pagopa.selfcare.document.util.LogSanitizer.sanitize;
import static it.pagopa.selfcare.onboarding.common.ProductId.*;

/**
 * Implementation of DocumentContentService for creating PDF documents.
 */
@Slf4j
@ApplicationScoped
public class DocumentContentServiceImpl implements DocumentContentService {

    private static final BiFunction<String, String, String> CONTRACT_FILENAME_FUNC =
            (format, productName) -> String.format(format, productName.replace(" ", "_"));

    private final AzureBlobClient azureBlobClient;
    private final DocumentMsConfig documentMsConfig;
    private final SignatureService signatureService;

    @Inject
    public DocumentContentServiceImpl(
            AzureBlobClient azureBlobClient,
            DocumentMsConfig documentMsConfig,
            SignatureService signatureService) {
        this.azureBlobClient = azureBlobClient;
        this.documentMsConfig = documentMsConfig;
        this.signatureService = signatureService;
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
                String filename = buildFilename("%s_" + request.getAttachmentName() + ".pdf", request.getProductName());
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
