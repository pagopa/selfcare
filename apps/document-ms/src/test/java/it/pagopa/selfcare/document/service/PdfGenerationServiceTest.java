package it.pagopa.selfcare.document.service;

import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.util.PdfBuilder;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

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

        // Payment (Dati pagamento) - opzionale ma meglio metterlo
        PaymentPdfData payment = new PaymentPdfData();
        payment.setHolder("ClearingHouseTest");
        payment.setIban("IT60X0542811101000000123456");
        request.setPayment(payment);

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