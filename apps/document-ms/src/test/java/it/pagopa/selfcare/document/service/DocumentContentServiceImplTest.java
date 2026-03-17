package it.pagopa.selfcare.document.service;

import com.azure.storage.blob.models.BlobProperties;
import eu.europa.esig.dss.model.DSSDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.document.service.impl.DocumentContentServiceImpl;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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

    @InjectMock DocumentRepository documentRepository;
    @InjectMock AzureBlobClient azureBlobClient;
    @InjectMock SignatureService signatureService;
    @Inject DocumentContentService documentContentService;

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

        // existsAttachment → document not found in DB
        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().nullItem());
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

        Document existingDoc = buildDocument();
        existingDoc.setType(TokenType.ATTACHMENT);
        existingDoc.setContractSigned("/path/to/attachment.pdf");

        // existsAttachment → document found in DB AND in storage
        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(existingDoc));
        when(azureBlobClient.getProperties(existingDoc.getContractSigned()))
                .thenReturn(Mockito.mock(BlobProperties.class));

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

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().nullItem());
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

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().nullItem());
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

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().nullItem());
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
}
