package it.pagopa.selfcare.document.service;

import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.util.PdfBuilder;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_CED;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_RTP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;

class PdfGenerationServiceTest {

    private PdfGenerationService pdfGenerationService;

    @BeforeEach
    void setUp() {
        pdfGenerationService = new PdfGenerationService();
    }

    @Test
    void generateContractPdf_shouldRouteTo_PspData_whenProductIsPagoPa_andInstitutionIsPsp() throws IOException {
        // Arrange
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId(PROD_PAGOPA.getValue());
        request.getInstitution().setInstitutionType(InstitutionType.PSP);

        File expectedFile = new File("dummy.pdf");
        String templateText = "<html>Template PSP</html>";

        try (MockedStatic<PdfBuilder> pdfBuilderMock = Mockito.mockStatic(PdfBuilder.class)) {
            pdfBuilderMock.when(() -> PdfBuilder.generateDocument(anyString(), anyString(), anyMap()))
                    .thenReturn(expectedFile);

            // Act
            File result = pdfGenerationService.generateContractPdf(templateText, request);

            // Assert
            assertNotNull(result);
            assertEquals(expectedFile, result);

            pdfBuilderMock.verify(() -> PdfBuilder.generateDocument(
                    eq("_contratto_interoperabilita."),
                    eq(templateText),
                    anyMap()
            ));
        }
    }

    @Test
    void generateContractPdf_shouldRouteTo_ProdIoData_whenProductIsIo() throws IOException {
        // Arrange
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId(PROD_IO.getValue());
        request.getInstitution().setInstitutionType(InstitutionType.PA);

        File expectedFile = new File("dummy_io.pdf");
        String templateText = "<html>Template IO</html>";

        try (MockedStatic<PdfBuilder> pdfBuilderMock = Mockito.mockStatic(PdfBuilder.class)) {
            pdfBuilderMock.when(() -> PdfBuilder.generateDocument(anyString(), anyString(), anyMap()))
                    .thenReturn(expectedFile);

            // Act
            File result = pdfGenerationService.generateContractPdf(templateText, request);

            // Assert
            assertEquals(expectedFile, result);
            pdfBuilderMock.verify(() -> PdfBuilder.generateDocument(anyString(), eq(templateText), anyMap()));
        }
    }

    @Test
    void generateContractPdf_shouldProvideCompanyRegisterData_whenProductIsCed() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId(PROD_CED.getValue());
        request.getInstitution().setInstitutionType(InstitutionType.PA);
        request.getInstitution().setRea("RM-123456");

        File expectedFile = new File("dummy_ced.pdf");
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        try (MockedStatic<PdfBuilder> pdfBuilderMock = Mockito.mockStatic(PdfBuilder.class)) {
            pdfBuilderMock.when(() -> PdfBuilder.generateDocument(anyString(), anyString(), anyMap()))
                    .thenReturn(expectedFile);

            pdfGenerationService.generateContractPdf("<html>Template CED</html>", request);

            pdfBuilderMock.verify(() -> PdfBuilder.generateDocument(anyString(), anyString(), dataCaptor.capture()));
            assertEquals("RM-123456", dataCaptor.getValue().get("institutionREA"));
            assertEquals("_______________", dataCaptor.getValue().get("institutionShareCapital"));
            assertEquals("_______________", dataCaptor.getValue().get("institutionBusinessRegisterPlace"));
        }
    }

    @Test
    void generateContractPdf_shouldResolveEveryRtpTemplatePlaceholder() throws IOException {
        RtpContractPdfRequest request = buildValidRtpContractRequest();
        request.setProductId(PROD_RTP.getValue());
        request.setRtpServiceTypes(List.of(RtpServiceType.RTP, RtpServiceType.REPOSITORY_CONSULTATION));
        request.setBilling(null);
        String template;
        try (var stream = getClass().getResourceAsStream(
                "/contracts/rtp/rtp-accordo_di_adesione-v.1.0.0.html")) {
            assertNotNull(stream);
            template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        try (MockedStatic<PdfBuilder> pdfBuilderMock = Mockito.mockStatic(PdfBuilder.class)) {
            pdfBuilderMock.when(() -> PdfBuilder.generateDocument(anyString(), anyString(), anyMap()))
                    .thenReturn(new File("dummy_rtp.pdf"));

            pdfGenerationService.generateContractPdf(template, request);

            pdfBuilderMock.verify(() -> PdfBuilder.generateDocument(anyString(), eq(template), dataCaptor.capture()));
        }
        Map<String, Object> data = dataCaptor.getValue();
        var placeholders = Pattern.compile("\\$\\{([^}]+)}").matcher(template);
        assertTrue(placeholders.find(), "The fixture must contain placeholders");
        placeholders.reset();
        while (placeholders.find()) {
            String key = placeholders.group(1);
            assertNotNull(data.get(key), () -> "Unresolved placeholder: " + key);
        }
        assertEquals("X", data.get("rtpServiceCheckbox"));
        assertEquals("X", data.get("rtpRepositoryServiceCheckbox"));
        assertEquals("X", data.get("rtpDebtorServiceProviderCheckbox"));
        assertEquals("X", data.get("rtpPagoPaMemberCheckbox"));
        assertEquals("", data.get("rtpCreditorServiceProviderCheckbox"));
        assertEquals("", data.get("rtpPagoPaNonMemberCheckbox"));
        assertFalse(new StringSubstitutor(data).replace(template).contains("${"));
    }

    @Test
    void generateAttachmentPdf_shouldGenerateCorrectly() throws IOException {
        // Arrange
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        String templateText = "<html>Attachment Template</html>";
        String expectedFilename = "attachment_test.pdf";
        File expectedFile = new File("dummy_attachment.pdf");

        try (MockedStatic<PdfBuilder> pdfBuilderMock = Mockito.mockStatic(PdfBuilder.class)) {
            pdfBuilderMock.when(() -> PdfBuilder.generateDocument(anyString(), anyString(), anyMap()))
                    .thenReturn(expectedFile);

            // Act
            File result = pdfGenerationService.generateAttachmentPdf(templateText, request, expectedFilename);

            // Assert
            assertEquals(expectedFile, result);
            pdfBuilderMock.verify(() -> PdfBuilder.generateDocument(eq(expectedFilename), eq(templateText), anyMap()));
        }
    }

    // =========================================================================
    // HELPER METHODS: Creano oggetti completamente popolati per evitare i NullPointerException nei Mapper
    // =========================================================================

    private ContractPdfRequest buildValidContractRequest() {
        ContractPdfRequest request = new ContractPdfRequest();
        request.setOnboardingId("onboarding-123");
        request.setProductId("prod-test");
        request.setProductName("Product Test");

        // Institution
        InstitutionPdfData institution = new InstitutionPdfData();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setDescription("Comune di Test");
        institution.setTaxCode("12345678901");
        institution.setDigitalAddress("pec@pec.test.it");
        institution.setZipCode("00100");
        institution.setAddress("Via Roma 1");
        request.setInstitution(institution);

        // Manager (Legale Rappresentante)
        UserPdfData manager = new UserPdfData();
        manager.setName("Mario");
        manager.setSurname("Rossi");
        manager.setTaxCode("MRORSS80A01H501Z");
        manager.setEmail("mario.rossi@test.it");
        request.setManager(manager);

        // Billing (Dati fatturazione)
        BillingPdfData billing = new BillingPdfData();
        billing.setVatNumber("IT12345678901");
        billing.setRecipientCode("ABCDEF");
        request.setBilling(billing);

        return request;
    }

    private RtpContractPdfRequest buildValidRtpContractRequest() {
        ContractPdfRequest baseRequest = buildValidContractRequest();
        RtpContractPdfRequest request = new RtpContractPdfRequest();
        request.setOnboardingId(baseRequest.getOnboardingId());
        request.setProductId(baseRequest.getProductId());
        request.setProductName(baseRequest.getProductName());
        request.setContractTemplatePath(baseRequest.getContractTemplatePath());
        request.setInstitution(baseRequest.getInstitution());
        request.setManager(baseRequest.getManager());
        request.setDelegates(baseRequest.getDelegates());
        request.setBilling(baseRequest.getBilling());
        request.setIsAggregator(baseRequest.getIsAggregator());
        request.setPricingPlan(baseRequest.getPricingPlan());
        request.setAggregatesCsvBaseUrl(baseRequest.getAggregatesCsvBaseUrl());
        return request;
    }

    private AttachmentPdfRequest buildValidAttachmentRequest() {
        AttachmentPdfRequest request = new AttachmentPdfRequest();
        request.setOnboardingId("onb-attach-123");
        request.setProductName("Product Attach");
        request.setAttachmentName("AllegatoA");

        UserPdfData manager = new UserPdfData();
        manager.setName("Mario");
        manager.setSurname("Rossi");
        manager.setTaxCode("MRORSS80A01H501Z");
        manager.setEmail("mario.rossi@test.it");
        request.setManager(manager);

        InstitutionPdfData institution = new InstitutionPdfData();
        institution.setDescription("Ente Test Allegato");
        request.setInstitution(institution);

        return request;
    }
}
