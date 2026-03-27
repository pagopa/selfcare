package it.pagopa.selfcare.document.service;

import it.pagopa.selfcare.document.model.dto.request.AttachmentPdfRequest;
import it.pagopa.selfcare.document.model.dto.request.ContractPdfRequest;
import it.pagopa.selfcare.document.model.dto.request.InstitutionPdfData;
import it.pagopa.selfcare.document.util.PdfBuilder;
import it.pagopa.selfcare.document.util.PdfMapperData;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.ProductId.*;

/**
 * Service dedicated to business logic for mapping data and generating PDF documents.
 */
@Slf4j
@ApplicationScoped
public class PdfGenerationService {

    /**
     * Generates a contract PDF by merging the template text with the request data.
     */
    public File generateContractPdf(String contractTemplateText, ContractPdfRequest request) throws IOException {
        Map<String, Object> data = PdfMapperData.setUpCommonData(request);

        // Populate the data map based on specific PagoPA product business rules
        setupProductSpecificData(data, request);

        log.debug("Building PDF template context: dataMap keys={}, size={}", data.keySet(), data.size());
        return PdfBuilder.generateDocument("_contratto_interoperabilita.", contractTemplateText, data);
    }

    /**
     * Generates an attachment PDF.
     */
    public File generateAttachmentPdf(String attachmentTemplateText, AttachmentPdfRequest request, String filename) throws IOException {
        Map<String, Object> data = PdfMapperData.setUpAttachmentData(request);

        log.debug("Building PDF attachment template context: dataMap keys={}, size={}", data.keySet(), data.size());
        return PdfBuilder.generateDocument(filename, attachmentTemplateText, data);
    }

    // ==================== Business Logic Routing ====================

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
}