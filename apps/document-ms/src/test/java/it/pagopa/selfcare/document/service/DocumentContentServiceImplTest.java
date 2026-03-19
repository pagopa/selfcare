package it.pagopa.selfcare.document.service;

import eu.europa.esig.dss.model.DSSDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.document.config.DocumentMsConfig;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.exception.InternalException;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.document.service.impl.DocumentContentServiceImpl;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.TokenType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.bson.types.ObjectId;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import jakarta.inject.Inject;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
public class DocumentContentServiceImplTest {

    private static final String ONBOARDING_ID = "onboardingId";
    private static final String DOCUMENT_ID = new ObjectId().toHexString();
    private static final String INSTITUTION_DESCRIPTION = "Test Institution";
    private static final String PRODUCT_ID = "Product-123";
    private static final String CONTRACT_TEMPLATE_PATH = "templates/contract.ftl";
    private static final String CONTRACT_TEMPLATE_PDF_PATH = "templates/contract.pdf";
    private static final String ATTACHMENT_TEMPLATE_PATH = "templates/attachment.ftl";
    private static final String PRODUCT_NAME = "PagoPA";
    private static final String PDF_FORMAT_FILENAME = "contract_%s.pdf";
    private static final String ATTACHMENT_NAME = "allegato-1";

    @InjectMock DocumentRepository documentRepository;
    @InjectMock AzureBlobClient azureBlobClient;
    @InjectMock SignatureService signatureService;
    @InjectMock DocumentService documentService;
    @InjectMock DocumentMsConfig documentMsConfig;
    @Inject DocumentContentService documentContentService;

    // ---- retrieveContract ----

    @Test
    void retrieveContract_notSigned_shouldReturnOkResponse() {
        Document doc = buildDocument();
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(mockFile);

        RestResponse<File> response = documentContentService.retrieveContract(ONBOARDING_ID, false)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveContract_signed_shouldReturnOkResponse() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf");
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf(doc.getContractSigned())).thenReturn(mockFile);

        RestResponse<File> response = documentContentService.retrieveContract(ONBOARDING_ID, true)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    // ---- retrieveContract edge cases ----

    @Test
    void retrieveContract_shouldUseSignedPath_whenIsSignedTrue() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract_signed.pdf");
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf("/path/to/signed/contract_signed.pdf")).thenReturn(mockFile);

        RestResponse<File> response = documentContentService.retrieveContract(ONBOARDING_ID, true)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
        verify(azureBlobClient).getFileAsPdf("/path/to/signed/contract_signed.pdf");
    }

    // ---- retrieveAttachment with buildAttachmentPath ----

    @Test
    void retrieveAttachment_shouldUseContractSignedPath_whenContractSignedIsNotNull() {
        Document doc = buildDocument();
        doc.setType(TokenType.ATTACHMENT);
        doc.setName("myAttachment");
        doc.setContractSigned("/path/to/signed/attachment.pdf");
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf("/path/to/signed/attachment.pdf")).thenReturn(mockFile);

        RestResponse<File> response = documentContentService.retrieveAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
        verify(azureBlobClient).getFileAsPdf("/path/to/signed/attachment.pdf");
    }

    @Test
    void retrieveAttachment_shouldBuildPath_whenContractSignedIsNull() {
        Document doc = buildDocument();
        doc.setType(TokenType.ATTACHMENT);
        doc.setName("myAttachment");
        doc.setContractSigned(null);
        doc.setContractFilename("attachment.pdf");
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(mockFile);

        RestResponse<File> response = documentContentService.retrieveAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    // ---- retrieveAttachment ----

    @Test
    void retrieveAttachment_shouldReturnOkResponse() {
        Document doc = buildDocument();
        doc.setType(TokenType.ATTACHMENT);
        doc.setName("myAttachment");
        doc.setContractSigned("/path/to/attachment.pdf");
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(mockFile);

        RestResponse<File> response = documentContentService.retrieveAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveAttachment_shouldThrowResourceNotFoundWhenDocumentIsNull() {
        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "missingAttachment"))
                .thenReturn(Uni.createFrom().nullItem());

        Uni<RestResponse<File>> uni = documentContentService.retrieveAttachment(ONBOARDING_ID, "missingAttachment");
        var awaiter = uni.await();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, awaiter::indefinitely);
        assertNotNull(ex);
    }

    // ---- retrieveTemplateAttachment ----

    @Test
    void retrieveTemplateAttachment_shouldReturnOkResponse() {
        File mockFile = Mockito.mock(File.class);
        String templatePath = "/templates/template.pdf";
        String attachmentName = "template.pdf";

        when(azureBlobClient.getFileAsPdf(templatePath)).thenReturn(mockFile);

        RestResponse<File> response =
            documentContentService
                .retrieveTemplateAttachment(
                    ONBOARDING_ID, templatePath, attachmentName, INSTITUTION_DESCRIPTION, PRODUCT_ID)
                .await()
                .indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveTemplateAttachment_shouldThrowResourceNotFoundWhenFileIsNull() {
        String templatePath = "/templates/missing.pdf";
        String attachmentName = "missing.pdf";

        when(azureBlobClient.getFileAsPdf(templatePath)).thenReturn(null);

        var awaiter =
            documentContentService
                .retrieveTemplateAttachment(
                    ONBOARDING_ID, templatePath, attachmentName, INSTITUTION_DESCRIPTION, PRODUCT_ID)
                .await();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, awaiter::indefinitely);
        assertNotNull(ex);
    }

    // ---- retrieveTemplateAttachment with signDocument ----

    @Test
    void retrieveTemplateAttachment_shouldCallSignDocument() {
        File mockFile = Mockito.mock(File.class);
        File signedFile = Mockito.mock(File.class);
        String templatePath = "/templates/template.pdf";
        String attachmentName = "template.pdf";

        when(azureBlobClient.getFileAsPdf(templatePath)).thenReturn(mockFile);
        when(signatureService.signDocument(mockFile, INSTITUTION_DESCRIPTION, PRODUCT_ID))
                .thenReturn(Uni.createFrom().item(signedFile));

        RestResponse<File> response = documentContentService
                .retrieveTemplateAttachment(ONBOARDING_ID, templatePath, attachmentName, INSTITUTION_DESCRIPTION, PRODUCT_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
        verify(signatureService).signDocument(mockFile, INSTITUTION_DESCRIPTION, PRODUCT_ID);
    }

    @Test
    void retrieveTemplateAttachment_shouldPropagateError_whenSignDocumentFails() {
        File mockFile = Mockito.mock(File.class);
        String templatePath = "/templates/template.pdf";
        String attachmentName = "template.pdf";

        when(azureBlobClient.getFileAsPdf(templatePath)).thenReturn(mockFile);
        when(signatureService.signDocument(mockFile, INSTITUTION_DESCRIPTION, PRODUCT_ID))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Sign failed")));

        var awaiter = documentContentService
                .retrieveTemplateAttachment(ONBOARDING_ID, templatePath, attachmentName, INSTITUTION_DESCRIPTION, PRODUCT_ID)
                .await();

        assertThrows(RuntimeException.class, awaiter::indefinitely);
    }

    // ---- retrieveSignedFile ----

    @Test
    void retrieveSignedFile_shouldReturnOkResponse_whenContractSignedIsPdf() throws IOException {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf");
        File tempPdf = createTempPdf();

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.retrieveFile(doc.getContractSigned())).thenReturn(tempPdf);

        RestResponse<File> response = documentContentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveSignedFile_shouldReturnNotFoundResponse_whenAzureStorageThrowsException() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf");

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.retrieveFile(anyString()))
                .thenThrow(new SelfcareAzureStorageException("Storage error", "500"));

        RestResponse<File> response = documentContentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveSignedFile_shouldReturnNotFoundResponse_whenPdfIsInvalid() throws IOException {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf");
        File invalidFile = Files.createTempFile("invalid", ".pdf").toFile();
        Files.write(invalidFile.toPath(), "not-a-pdf-content".getBytes());

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.retrieveFile(anyString())).thenReturn(invalidFile);

        RestResponse<File> response = documentContentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveSignedFile_shouldReturnOkResponse_whenContractSignedIsP7m() throws IOException {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf.p7m");
        File tempPdf = createTempPdf();

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.retrieveFile(doc.getContractSigned())).thenReturn(tempPdf);
        when(signatureService.verifySignature(any(File.class))).thenReturn(true);
        when(signatureService.extractFile(any(File.class))).thenReturn(tempPdf);

        RestResponse<File> response = documentContentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    // ---- isPdfValid (static method) ----

    @Test
    void isPdfValid_shouldNotThrow_whenPdfIsValid() throws IOException {
        File validPdf = createTempPdf();
        assertDoesNotThrow(() -> DocumentContentServiceImpl.isPdfValid(validPdf));
    }

    @Test
    void isPdfValid_shouldThrowInvalidRequestException_whenPdfHasNoPages() throws IOException {
        File emptyPdf = Files.createTempFile("empty", ".pdf").toFile();
        emptyPdf.deleteOnExit();
        try (PDDocument pdf = new PDDocument()) {
            pdf.save(emptyPdf);
        }
        assertThrows(InvalidRequestException.class, () -> DocumentContentServiceImpl.isPdfValid(emptyPdf));
    }

    @Test
    void isPdfValid_shouldThrowInvalidRequestException_whenFileIsNotPdf() throws IOException {
        File invalidFile = Files.createTempFile("invalid", ".pdf").toFile();
        invalidFile.deleteOnExit();
        Files.write(invalidFile.toPath(), "this is not a pdf".getBytes());
        assertThrows(InvalidRequestException.class, () -> DocumentContentServiceImpl.isPdfValid(invalidFile));
    }

    @Test
    void isPdfValid_shouldThrowInvalidRequestException_whenFileDoesNotExist() {
        File nonExistentFile = new File("/non/existent/path/file.pdf");
        assertThrows(InvalidRequestException.class, () -> DocumentContentServiceImpl.isPdfValid(nonExistentFile));
    }

    // ---- retrieveSignedFile edge cases ----

    @Test
    void retrieveSignedFile_shouldReturnNotFound_whenP7mExtractionFails() throws IOException {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf.p7m");
        File tempP7m = createTempPdf();

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.retrieveFile(doc.getContractSigned())).thenReturn(tempP7m);
        when(signatureService.verifySignature(any(File.class))).thenReturn(true);
        when(signatureService.extractFile(any(File.class)))
                .thenThrow(new RuntimeException("Extraction failed"));

        RestResponse<File> response = documentContentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveSignedFile_shouldReturnNotFound_whenExtractedPdfFromP7mIsInvalid() throws IOException {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf.p7m");
        File tempP7m = createTempPdf();
        File invalidPdf = Files.createTempFile("invalid", ".pdf").toFile();
        Files.write(invalidPdf.toPath(), "not-a-pdf".getBytes());

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.retrieveFile(doc.getContractSigned())).thenReturn(tempP7m);
        when(signatureService.verifySignature(any(File.class))).thenReturn(true);
        when(signatureService.extractFile(any(File.class))).thenReturn(invalidPdf);

        RestResponse<File> response = documentContentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveSignedFile_shouldReturnNotFound_whenP7mSignatureVerificationFails() throws IOException {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf.p7m");
        File tempP7m = createTempPdf();

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.retrieveFile(doc.getContractSigned())).thenReturn(tempP7m);
        when(signatureService.verifySignature(any(File.class)))
                .thenThrow(new RuntimeException("Signature verification failed"));

        RestResponse<File> response = documentContentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    // ---- retrieveSignedFile with PDF validation ----

    @Test
    void retrieveSignedFile_shouldReturnNotFound_whenPdfIsInvalid() throws IOException {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/signed/contract.pdf");
        File invalidPdf = Files.createTempFile("invalid", ".pdf").toFile();
        Files.write(invalidPdf.toPath(), "not-a-valid-pdf".getBytes());

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.retrieveFile(doc.getContractSigned())).thenReturn(invalidPdf);

        RestResponse<File> response = documentContentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    // ---- uploadAttachment ----

    @Test
    void uploadAttachment_shouldCompleteSuccessfully_whenAttachmentDoesNotExist() throws IOException {
        File tempFile = createTempPdf();
        FormItem formItem = FormItem.builder()
                .file(tempFile)
                .fileName("attachment.pdf")
                .build();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(TokenType.ATTACHMENT)
                .documentName("myAttachment")
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .build();

        Document persistedDoc = buildDocument();
        persistedDoc.setType(TokenType.ATTACHMENT);
        persistedDoc.setName("myAttachment");
        persistedDoc.setContractFilename("signed_template.pdf");
        persistedDoc.setContractSigned("/parties/docs/" + ONBOARDING_ID + "/attachments/signed_template.pdf");

        // existsAttachment → returns false (attachment does not exist)
        when(documentService.existsAttachment(ONBOARDING_ID, "myAttachment"))
                .thenReturn(Uni.createFrom().item(false));
        // getTemplateAndVerifyDigest → template file from Azure
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempFile);
        // persist attachment
        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(persistedDoc));
        // signatureService stubs
        when(signatureService.verifySignature(any(File.class))).thenReturn(true);
        when(signatureService.extractPdfFromSignedContainer(any(), any()))
                .thenAnswer(inv -> inv.getArgument(1, DSSDocument.class));
        when(signatureService.computeDigestOfSignedRevision(any(), any()))
                .thenReturn("same-digest");

        var awaiter = documentContentService.uploadAttachment(request, formItem).await();
        assertDoesNotThrow(awaiter::indefinitely);
    }

    @Test
    void uploadAttachment_shouldThrowUpdateNotAllowedException_whenAttachmentAlreadyExists() throws IOException {
        File tempFile = createTempPdf();
        FormItem formItem = FormItem.builder()
                .file(tempFile)
                .fileName("attachment.pdf")
                .build();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(TokenType.ATTACHMENT)
                .documentName("myAttachment")
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .build();

        // existsAttachment → returns true (attachment already exists)
        when(documentService.existsAttachment(ONBOARDING_ID, "myAttachment"))
                .thenReturn(Uni.createFrom().item(true));

        var awaiter = documentContentService.uploadAttachment(request, formItem).await();
        assertThrows(UpdateNotAllowedException.class, awaiter::indefinitely);
    }

    // ---- uploadAttachment with P7M file ----

    @Test
    void uploadAttachment_shouldCompleteSuccessfully_withP7MFile() throws IOException {
        File tempFile = createTempPdf();
        FormItem formItem = FormItem.builder()
                .file(tempFile)
                .fileName("attachment.pdf.p7m")
                .build();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(TokenType.ATTACHMENT)
                .documentName("myAttachment")
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .build();

        Document persistedDoc = buildDocument();
        persistedDoc.setType(TokenType.ATTACHMENT);
        persistedDoc.setName("myAttachment");
        persistedDoc.setContractFilename("signed_template.pdf.p7m");
        persistedDoc.setContractSigned("/parties/docs/" + ONBOARDING_ID + "/attachments/signed_template.pdf.p7m");

        when(documentService.existsAttachment(ONBOARDING_ID, "myAttachment"))
                .thenReturn(Uni.createFrom().item(false));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempFile);
        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(persistedDoc));
        when(signatureService.isSignatureVerificationEnabled()).thenReturn(false);
        when(signatureService.extractPdfFromSignedContainer(any(), any()))
                .thenAnswer(inv -> inv.getArgument(1, DSSDocument.class));
        when(signatureService.computeDigestOfSignedRevision(any(), any()))
                .thenReturn("same-digest");
        when(signatureService.verifyUploadedFileDigest(any(), any(), anyBoolean()))
                .thenReturn("uploaded-digest");

        var awaiter = documentContentService.uploadAttachment(request, formItem).await();
        assertDoesNotThrow(awaiter::indefinitely);

        // Verify the document filename ends with .p7m
        assertTrue(persistedDoc.getContractFilename().endsWith(".p7m"));
    }

    @Test
    void uploadAttachment_shouldVerifySignature_whenSignatureVerificationEnabled() throws IOException {
        File tempFile = createTempPdf();
        FormItem formItem = FormItem.builder()
                .file(tempFile)
                .fileName("attachment.pdf")
                .build();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(TokenType.ATTACHMENT)
                .documentName("myAttachment")
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .build();

        Document persistedDoc = buildDocument();
        persistedDoc.setType(TokenType.ATTACHMENT);
        persistedDoc.setName("myAttachment");

        when(documentService.existsAttachment(ONBOARDING_ID, "myAttachment"))
                .thenReturn(Uni.createFrom().item(false));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempFile);
        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(persistedDoc));
        when(signatureService.isSignatureVerificationEnabled()).thenReturn(true);
        when(signatureService.verifySignature(any(File.class))).thenReturn(true);
        when(signatureService.extractPdfFromSignedContainer(any(), any()))
                .thenAnswer(inv -> inv.getArgument(1, DSSDocument.class));
        when(signatureService.computeDigestOfSignedRevision(any(), any()))
                .thenReturn("same-digest");
        when(signatureService.verifyUploadedFileDigest(any(), any(), anyBoolean()))
                .thenReturn("uploaded-digest");

        var awaiter = documentContentService.uploadAttachment(request, formItem).await();
        assertDoesNotThrow(awaiter::indefinitely);

        verify(signatureService).verifySignature(tempFile);
    }

    // ---- getTemplateDigest ----

    @Test
    void getTemplateDigest_shouldThrowNullPointerException_whenTemplatePathIsNull() {
        assertThrows(NullPointerException.class, () ->
                ((DocumentContentServiceImpl) documentContentService).getTemplateDigest(null));
    }

    @Test
    void getTemplateDigest_shouldReturnDigest_whenTemplateExists() throws IOException {
        File tempPdf = createTempPdf();
        String templatePath = "/templates/template.pdf";

        when(azureBlobClient.getFileAsPdf(templatePath)).thenReturn(tempPdf);
        when(signatureService.extractPdfFromSignedContainer(any(), any()))
                .thenAnswer(inv -> inv.getArgument(1, DSSDocument.class));
        when(signatureService.computeDigestOfSignedRevision(any(), any()))
                .thenReturn("computed-digest-value");

        String digest = ((DocumentContentServiceImpl) documentContentService).getTemplateDigest(templatePath);

        assertNotNull(digest);
        assertEquals("computed-digest-value", digest);
        verify(azureBlobClient).getFileAsPdf(templatePath);
        verify(signatureService).computeDigestOfSignedRevision(any(), any());
    }



    // ---- uploadAttachment when upload to Azure fails ----

    @Test
    void uploadAttachment_shouldThrowInternalException_whenAzureUploadFails() throws IOException {
        File tempFile = createTempPdf();
        FormItem formItem = FormItem.builder()
                .file(tempFile)
                .fileName("attachment.pdf")
                .build();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(TokenType.ATTACHMENT)
                .documentName("myAttachment")
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .build();

        Document persistedDoc = buildDocument();
        persistedDoc.setType(TokenType.ATTACHMENT);
        persistedDoc.setName("myAttachment");
        persistedDoc.setContractFilename("signed_template.pdf");
        persistedDoc.setContractSigned("/parties/docs/" + ONBOARDING_ID + "/attachments/signed_template.pdf");

        when(documentService.existsAttachment(ONBOARDING_ID, "myAttachment"))
                .thenReturn(Uni.createFrom().item(false));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempFile);
        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(persistedDoc));
        when(signatureService.isSignatureVerificationEnabled()).thenReturn(false);
        when(signatureService.extractPdfFromSignedContainer(any(), any()))
                .thenAnswer(inv -> inv.getArgument(1, DSSDocument.class));
        when(signatureService.computeDigestOfSignedRevision(any(), any()))
                .thenReturn("same-digest");
        when(signatureService.verifyUploadedFileDigest(any(), any(), anyBoolean()))
                .thenReturn("uploaded-digest");
        doThrow(new SelfcareAzureStorageException("Upload failed", "500"))
                .when(azureBlobClient).uploadFile(anyString(), anyString(), any(byte[].class));

        var awaiter = documentContentService.uploadAttachment(request, formItem).await();
        assertThrows(SelfcareAzureStorageException.class, awaiter::indefinitely);
    }

    // ---- saveVisuraForMerchant ----

    @Test
    void saveVisuraForMerchant_shouldUploadVisuraSuccessfully() {
        byte[] content = new byte[]{1, 2, 3};
        UploadVisuraRequest request = UploadVisuraRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .filename("VISURA_test.xml")
                .fileContent(content)
                .build();

        var awaiter = documentContentService.saveVisuraForMerchant(request).await();

        assertDoesNotThrow(awaiter::indefinitely);
        verify(azureBlobClient).uploadFile(anyString(), eq("VISURA_test.xml"), eq(content));
    }

    @Test
    void saveVisuraForMerchant_shouldThrowInternalException_whenUploadFails() {
        byte[] content = new byte[]{4, 5, 6};
        UploadVisuraRequest request = UploadVisuraRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .filename("VISURA_test.xml")
                .fileContent(content)
                .build();

        doThrow(new RuntimeException("upload error"))
                .when(azureBlobClient)
                .uploadFile(anyString(), anyString(), any());

        var awaiter = documentContentService.saveVisuraForMerchant(request).await();

        assertThrows(InternalException.class, awaiter::indefinitely);
    }

    // ---- helper ----

    private Document buildDocument() {
        Document doc = new Document();
        doc.setId(DOCUMENT_ID);
        doc.setOnboardingId(ONBOARDING_ID);
        doc.setProductId("prod-io");
        doc.setContractFilename("contract.pdf");
        doc.setType(TokenType.INSTITUTION);
        return doc;
    }

    private File createTempPdf() throws IOException {
        File tempFile = Files.createTempFile("test-contract", ".pdf").toFile();
        tempFile.deleteOnExit();
        try (PDDocument pdf = new PDDocument()) {
            pdf.addPage(new PDPage());
            pdf.save(tempFile);
        }
        return tempFile;
    }

    // ============================================
    // createContractPdf - Success scenarios
    // ============================================

    @Test
    void createContractPdf_shouldReturnSuccess_whenValidRequestWithHtmlTemplate() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq(PRODUCT_ID)))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
        assertNotNull(response.getStoragePath());
        assertNotNull(response.getFilename());
        assertTrue(response.getFilename().contains(PRODUCT_NAME.replace(" ", "_")));
        verify(azureBlobClient).uploadFile(anyString(), anyString(), any(byte[].class));
    }

    @Test
    void createContractPdf_shouldReturnSuccess_whenValidRequestWithPdfTemplate() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setContractTemplatePath(CONTRACT_TEMPLATE_PDF_PATH);
        File pdfTemplate = createTempPdf();
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsPdf(CONTRACT_TEMPLATE_PDF_PATH)).thenReturn(pdfTemplate);
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq(PRODUCT_ID)))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
        assertNotNull(response.getStoragePath());
        verify(azureBlobClient).getFileAsPdf(CONTRACT_TEMPLATE_PDF_PATH);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withPSPInstitutionType() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-pagopa");
        request.getInstitution().setInstitutionType(InstitutionType.PSP);
        request.getInstitution().setPaymentServiceProvider(buildPaymentServiceProviderData());
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-pagopa")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withPRVInstitutionType() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-pagopa");
        request.getInstitution().setInstitutionType(InstitutionType.PRV);
        request.setPayment(PaymentPdfData.builder().holder("Holder").iban("IT123").build());
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-pagopa")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withGPUInstitutionType() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-pagopa");
        request.getInstitution().setInstitutionType(InstitutionType.GPU);
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-pagopa")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withPRV_PFInstitutionType() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-pagopa");
        request.getInstitution().setInstitutionType(InstitutionType.PRV_PF);
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-pagopa")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withECInstitutionType() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-pagopa");
        request.getInstitution().setInstitutionType(InstitutionType.PA);
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-pagopa")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withProdIO() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-io");
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-io")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withProdIOPremium() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-io-premium");
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-io-premium")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withProdIOSign() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-io-sign");
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-io-sign")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withProdPN() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-pn");
        request.setBilling(BillingPdfData.builder().vatNumber("123").recipientCode("ABC").build());
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-pn")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withProdInterop() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-interop");
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-interop")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withDashboardPSP() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-dashboard-psp");
        request.getInstitution().setInstitutionType(InstitutionType.PSP);
        request.getInstitution().setPaymentServiceProvider(buildPaymentServiceProviderData());
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-dashboard-psp")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withIdpayMerchant() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId("prod-idpay-merchant");
        request.getInstitution().setInstitutionType(InstitutionType.PRV);
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq("prod-idpay-merchant")))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withDelegates() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setDelegates(List.of(
                buildValidUserPdfData("delegate-1", "DLGTAX001"),
                buildValidUserPdfData("delegate-2", "DLGTAX002")
        ));
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq(PRODUCT_ID)))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withAggregator() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setIsAggregator(true);
        request.setAggregatesCsvBaseUrl("https://example.com/aggregates");
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq(PRODUCT_ID)))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_withPricingPlan() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        request.setPricingPlan("C1");
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq(PRODUCT_ID)))
                .thenReturn(Uni.createFrom().item(signedPdf));

        CreatePdfResponse response = documentContentService.createContractPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    // ============================================
    // createContractPdf - Error scenarios
    // ============================================

    @Test
    void createContractPdf_shouldThrowInternalException_whenTemplateLoadFails() {
        ContractPdfRequest request = buildValidContractRequest();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH))
                .thenThrow(new SelfcareAzureStorageException("Template not found", "404"));

        var awaiter = documentContentService.createContractPdf(request).await();
        assertThrows(SelfcareAzureStorageException.class, awaiter::indefinitely);
    }

    @Test
    void createContractPdf_shouldThrowInternalException_whenSignatureFails() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq(PRODUCT_ID)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Signature failed")));

        var awaiter = documentContentService.createContractPdf(request).await();
        assertThrows(RuntimeException.class, awaiter::indefinitely);
    }

    @Test
    void createContractPdf_shouldThrowException_whenUploadFails() throws IOException {
        ContractPdfRequest request = buildValidContractRequest();
        File signedPdf = createTempPdf();

        when(azureBlobClient.getFileAsText(CONTRACT_TEMPLATE_PATH)).thenReturn("<html><body>Contract</body></html>");
        when(signatureService.signDocument(any(File.class), eq(INSTITUTION_DESCRIPTION), eq(PRODUCT_ID)))
                .thenReturn(Uni.createFrom().item(signedPdf));
        doThrow(new SelfcareAzureStorageException("Upload failed", "500"))
                .when(azureBlobClient).uploadFile(anyString(), anyString(), any(byte[].class));

        var awaiter = documentContentService.createContractPdf(request).await();
        assertThrows(SelfcareAzureStorageException.class, awaiter::indefinitely);
    }

    // ============================================
    // createAttachmentPdf - Success scenarios
    // ============================================

    @Test
    void createAttachmentPdf_shouldReturnSuccess_whenValidRequestWithHtmlTemplate() throws IOException {
        AttachmentPdfRequest request = buildValidAttachmentRequest();

        when(azureBlobClient.getFileAsText(ATTACHMENT_TEMPLATE_PATH)).thenReturn("<html><body>Attachment</body></html>");

        CreatePdfResponse response = documentContentService.createAttachmentPdf(request)
                .await().indefinitely();

        assertNotNull(response);
        assertNotNull(response.getStoragePath());
        assertNotNull(response.getFilename());
        assertTrue(response.getFilename().contains(ATTACHMENT_NAME));
        assertTrue(response.getStoragePath().contains("/attachments"));
        verify(azureBlobClient).uploadFile(anyString(), anyString(), any(byte[].class));
    }

    @Test
    void createAttachmentPdf_shouldReturnSuccess_whenValidRequestWithPdfTemplate() throws IOException {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setAttachmentTemplatePath("templates/attachment.pdf");
        File pdfTemplate = createTempPdf();

        when(azureBlobClient.getFileAsPdf("templates/attachment.pdf")).thenReturn(pdfTemplate);

        CreatePdfResponse response = documentContentService.createAttachmentPdf(request)
                .await().indefinitely();

        assertNotNull(response);
        assertNotNull(response.getStoragePath());
        verify(azureBlobClient).getFileAsPdf("templates/attachment.pdf");
    }

    @Test
    void createAttachmentPdf_shouldReturnSuccess_withGpuData() throws IOException {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.getInstitution().setGpuData(GpuDataPdfData.builder()
                .businessRegisterNumber("BR123")
                .legalRegisterNumber("LR456")
                .legalRegisterName("Legal Register")
                .build());

        when(azureBlobClient.getFileAsText(ATTACHMENT_TEMPLATE_PATH)).thenReturn("<html><body>Attachment</body></html>");

        CreatePdfResponse response = documentContentService.createAttachmentPdf(request)
                .await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void createAttachmentPdf_shouldReturnSuccess_withProductNameContainingSpaces() throws IOException {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setProductName("Product With Spaces");

        when(azureBlobClient.getFileAsText(ATTACHMENT_TEMPLATE_PATH)).thenReturn("<html><body>Attachment</body></html>");

        CreatePdfResponse response = documentContentService.createAttachmentPdf(request)
                .await().indefinitely();

        assertNotNull(response);
        assertTrue(response.getFilename().contains("Product_With_Spaces"));
    }

    // ============================================
    // createAttachmentPdf - Error scenarios
    // ============================================

    @Test
    void createAttachmentPdf_shouldThrowInternalException_whenTemplateLoadFails() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();

        when(azureBlobClient.getFileAsText(ATTACHMENT_TEMPLATE_PATH))
                .thenThrow(new SelfcareAzureStorageException("Template not found", "404"));

        var awaiter = documentContentService.createAttachmentPdf(request).await();
        assertThrows(SelfcareAzureStorageException.class, awaiter::indefinitely);
    }

    @Test
    void createAttachmentPdf_shouldThrowException_whenUploadFails() throws IOException {
        AttachmentPdfRequest request = buildValidAttachmentRequest();

        when(azureBlobClient.getFileAsText(ATTACHMENT_TEMPLATE_PATH)).thenReturn("<html><body>Attachment</body></html>");
        doThrow(new SelfcareAzureStorageException("Upload failed", "500"))
                .when(azureBlobClient).uploadFile(anyString(), anyString(), any(byte[].class));

        var awaiter = documentContentService.createAttachmentPdf(request).await();
        assertThrows(SelfcareAzureStorageException.class, awaiter::indefinitely);
    }

    @Test
    void deleteContract_withAbsolutePath_shouldMoveFileAndReturnDeletedPath() throws IOException {
        // Arrange
        String fileName = "contracts/onboardingId/contract.pdf";
        boolean absolutePath = true;
        File tempFile = createTempPdf();

        when(documentMsConfig.getContractPath()).thenReturn("contracts/");
        when(documentMsConfig.getDeletePath()).thenReturn("deleted/");
        when(azureBlobClient.retrieveFile(fileName)).thenReturn(tempFile);

        // Act
        String result = documentContentService.deleteContract(fileName, absolutePath)
                .await().indefinitely();

        // Assert
        assertNotNull(result);
        assertEquals("deleted/onboardingId/contract.pdf", result);

        verify(azureBlobClient).retrieveFile(fileName);
        verify(azureBlobClient).uploadFilePath(eq("deleted/onboardingId/contract.pdf"), any(byte[].class));
        verify(azureBlobClient).removeFile(fileName);
    }

    @Test
    void deleteContract_withRelativePath_shouldMoveFileAndReturnDeletedPath() throws IOException {
        // Arrange
        String fileName = "onboardingId/contract.pdf";
        boolean absolutePath = false;
        String expectedOriginalPath = "/contracts/onboardingId/contract.pdf";
        File tempFile = createTempPdf();

        when(documentMsConfig.getContractPath()).thenReturn("/contracts/");
        when(documentMsConfig.getDeletePath()).thenReturn("/deleted/");
        when(azureBlobClient.retrieveFile(expectedOriginalPath)).thenReturn(tempFile);

        // Act
        String result = documentContentService.deleteContract(fileName, absolutePath)
                .await().indefinitely();

        // Assert
        assertNotNull(result);
        assertEquals("/deleted/onboardingId/contract.pdf", result);

        verify(azureBlobClient).retrieveFile(expectedOriginalPath);
        verify(azureBlobClient).uploadFilePath(eq("/deleted/onboardingId/contract.pdf"), any(byte[].class));
        verify(azureBlobClient).removeFile(expectedOriginalPath);
    }

    @Test
    void deleteContract_shouldCatchIOException_andReturnOriginalPath() throws IOException {
        // Arrange
        String fileName = "onboardingId/contract.pdf";
        boolean absolutePath = false;
        String expectedOriginalPath = "/contracts/onboardingId/contract.pdf";

        File phantomFile = Files.createTempFile("phantom", ".pdf").toFile();
        phantomFile.delete();

        when(documentMsConfig.getContractPath()).thenReturn("/contracts/");
        when(documentMsConfig.getDeletePath()).thenReturn("/deleted/");
        when(azureBlobClient.retrieveFile(expectedOriginalPath)).thenReturn(phantomFile);

        // Act
        String result = documentContentService.deleteContract(fileName, absolutePath)
                .await().indefinitely();

        // Assert
        assertNotNull(result);
        assertEquals(expectedOriginalPath, result);

        verify(azureBlobClient).retrieveFile(expectedOriginalPath);
        // Assicuriamoci che l'upload non sia mai stato chiamato a causa dell'eccezione
        verify(azureBlobClient, Mockito.never()).uploadFilePath(anyString(), any(byte[].class));
        verify(azureBlobClient, Mockito.never()).removeFile(anyString());
    }

    // ============================================
    // Helper methods for building test objects
    // ============================================

    private ContractPdfRequest buildValidContractRequest() {
        return ContractPdfRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .contractTemplatePath(CONTRACT_TEMPLATE_PATH)
                .productId(PRODUCT_ID)
                .productName(PRODUCT_NAME)
                .pdfFormatFilename(PDF_FORMAT_FILENAME)
                .institution(buildValidInstitutionPdfData())
                .manager(buildValidUserPdfData("manager-1", "MNGTAX001"))
                .build();
    }

    private AttachmentPdfRequest buildValidAttachmentRequest() {
        return AttachmentPdfRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .attachmentTemplatePath(ATTACHMENT_TEMPLATE_PATH)
                .productId(PRODUCT_ID)
                .productName(PRODUCT_NAME)
                .attachmentName(ATTACHMENT_NAME)
                .institution(buildValidInstitutionPdfData())
                .manager(buildValidUserPdfData("manager-1", "MNGTAX001"))
                .build();
    }

    private InstitutionPdfData buildValidInstitutionPdfData() {
        return InstitutionPdfData.builder()
                .id("inst-123")
                .taxCode("12345678901")
                .description(INSTITUTION_DESCRIPTION)
                .digitalAddress("pec@test.it")
                .address("Via Test 123")
                .zipCode("00100")
                .city("Roma")
                .county("RM")
                .country("Italia")
                .build();
    }

    private UserPdfData buildValidUserPdfData(String id, String taxCode) {
        return UserPdfData.builder()
                .id(id)
                .taxCode(taxCode)
                .name("Mario")
                .surname("Rossi")
                .email("mario.rossi@test.it")
                .role(PartyRole.MANAGER)
                .build();
    }

    private PaymentServiceProviderPdfData buildPaymentServiceProviderData() {
        return PaymentServiceProviderPdfData.builder()
                .abiCode("12345")
                .businessRegisterNumber("BR123")
                .legalRegisterNumber("LR456")
                .legalRegisterName("Register Name")
                .vatNumberGroup(false)
                .build();
    }
}
