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
import it.pagopa.selfcare.document.controller.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.controller.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.controller.response.ContractSignedReport;
import it.pagopa.selfcare.document.controller.response.DocumentBuilderResponse;
import it.pagopa.selfcare.document.entity.Document;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.onboarding.common.TokenType;
import jakarta.inject.Inject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.bson.types.ObjectId;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class DocumentServiceImpTest {

    private static final String ONBOARDING_ID = "onboardingId";
    private static final String DOCUMENT_ID = new ObjectId().toHexString();

    @Inject
    DocumentService documentService;

    @InjectMock
    AzureBlobClient azureBlobClient;


    @InjectMock
    DocumentRepository documentRepository;

    @InjectMock
    SignatureService signatureService;

    // ---- getDocumentsByOnboardingId ----

    @Test
    void getDocumentsByOnboardingId_shouldReturnDocumentList() {
        Document doc = buildDocument();
        when(documentRepository.findAllByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(List.of(doc)));

        List<Document> result = documentService.getDocumentsByOnboardingId(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ONBOARDING_ID, result.get(0).getOnboardingId());
        verify(documentRepository).findAllByOnboardingId(ONBOARDING_ID);
    }

    @Test
    void getDocumentsByOnboardingId_shouldReturnEmptyList() {
        when(documentRepository.findAllByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(List.of()));

        List<Document> result = documentService.getDocumentsByOnboardingId(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---- getDocumentById ----

    @Test
    void getDocumentById_shouldReturnDocument() {
        Document doc = buildDocument();
        when(documentRepository.findById(any(ObjectId.class)))
                .thenReturn(Uni.createFrom().item(doc));

        Document result = documentService.getDocumentById(DOCUMENT_ID)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(ONBOARDING_ID, result.getOnboardingId());
    }

    @Test
    void getDocumentById_shouldThrowResourceNotFoundWhenDocumentIsNull() {
        when(documentRepository.findById(any(ObjectId.class)))
                .thenReturn(Uni.createFrom().nullItem());

        var awaiter = documentService.getDocumentById(DOCUMENT_ID).await();

        assertThrows(ResourceNotFoundException.class, awaiter::indefinitely);
    }

    // ---- retrieveContract ----

    @Test
    void retrieveContract_notSigned_shouldReturnOkResponse() {
        Document doc = buildDocument();
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(mockFile);

        RestResponse<File> response = documentService.retrieveContract(ONBOARDING_ID, false)
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

        RestResponse<File> response = documentService.retrieveContract(ONBOARDING_ID, true)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    // ---- retrieveTemplateAttachment ----

    @Test
    void retrieveTemplateAttachment_shouldReturnOkResponse() {
        File mockFile = Mockito.mock(File.class);
        String templatePath = "/templates/template.pdf";
        String attachmentName = "template.pdf";

        when(azureBlobClient.getFileAsPdf(templatePath)).thenReturn(mockFile);

        RestResponse<File> response = documentService.retrieveTemplateAttachment(ONBOARDING_ID, templatePath, attachmentName)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveTemplateAttachment_shouldThrowResourceNotFoundWhenFileIsNull() {
        String templatePath = "/templates/missing.pdf";
        String attachmentName = "missing.pdf";

        when(azureBlobClient.getFileAsPdf(templatePath)).thenReturn(null);

        var awaiter = documentService.retrieveTemplateAttachment(ONBOARDING_ID, templatePath, attachmentName).await();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, awaiter::indefinitely);
        assertNotNull(ex);
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

        RestResponse<File> response = documentService.retrieveAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void retrieveAttachment_shouldThrowResourceNotFoundWhenDocumentIsNull() {
        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "missingAttachment"))
                .thenReturn(Uni.createFrom().nullItem());

        Uni<RestResponse<File>> uni = documentService.retrieveAttachment(ONBOARDING_ID, "missingAttachment");
        var awaiter = uni.await();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, awaiter::indefinitely);
        assertNotNull(ex);
    }

    // ---- getAttachments ----

    @Test
    void getAttachments_shouldReturnAttachmentNames() {
        Document doc1 = buildDocument();
        doc1.setType(TokenType.ATTACHMENT);
        doc1.setName("attachment-1");

        Document doc2 = buildDocument();
        doc2.setType(TokenType.ATTACHMENT);
        doc2.setName("attachment-2");

        when(documentRepository.findAttachments(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(List.of(doc1, doc2)));

        List<String> result = documentService.getAttachments(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("attachment-1"));
        assertTrue(result.contains("attachment-2"));
        verify(documentRepository).findAttachments(ONBOARDING_ID);
    }

    @Test
    void getAttachments_shouldReturnEmptyList_whenNoAttachmentsFound() {
        when(documentRepository.findAttachments(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(List.of()));

        List<String> result = documentService.getAttachments(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(documentRepository).findAttachments(ONBOARDING_ID);
    }

    // ---- updateContractSigned ----

    @Test
    void updateContractSigned_shouldReturnUpdatedCount() {
        when(documentRepository.updateContractSignedByOnboardingId(ONBOARDING_ID, "/path/signed.pdf"))
                .thenReturn(Uni.createFrom().item(1L));

        Long result = documentService.updateContractSigned(ONBOARDING_ID, "/path/signed.pdf")
                .await().indefinitely();

        assertEquals(1L, result);
        verify(documentRepository).updateContractSignedByOnboardingId(ONBOARDING_ID, "/path/signed.pdf");
    }

    // ---- reportContractSigned ----

    @Test
    void reportContractSigned_shouldReturnCadesTrue_whenContractFound() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/signed.pdf");
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf(doc.getContractSigned())).thenReturn(mockFile);
        when(signatureService.verifySignature(any(File.class))).thenReturn(true);

        ContractSignedReport report = documentService.reportContractSigned(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(report);
        assertTrue(report.isCades());
    }

    @Test
    void reportContractSigned_shouldReturnCadesFalse_whenDocumentNotFound() {
        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().nullItem());

        ContractSignedReport report = documentService.reportContractSigned(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(report);
        assertFalse(report.isCades());
    }

    @Test
    void reportContractSigned_shouldReturnCadesFalse_whenStorageFails() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/signed.pdf");

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf(anyString()))
                .thenThrow(new SelfcareAzureStorageException("Storage error", "500"));

        ContractSignedReport report = documentService.reportContractSigned(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(report);
        assertFalse(report.isCades());
    }

    // ---- existsAttachment ----

    @Test
    void existsAttachment_shouldReturnTrue_whenAttachmentFoundInStorage() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/attachment.pdf");

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getProperties(doc.getContractSigned()))
                .thenReturn(Mockito.mock(BlobProperties.class));

        Boolean result = documentService.existsAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertTrue(result);
    }

    @Test
    void existsAttachment_shouldReturnFalse_whenAttachmentNotFoundInStorage() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/attachment.pdf");

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(doc));
        Mockito.doThrow(new SelfcareAzureStorageException("Not found", "404"))
                .when(azureBlobClient).getProperties(anyString());

        Boolean result = documentService.existsAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertFalse(result);
    }

    @Test
    void existsAttachment_shouldReturnFalse_whenDocumentIsNull() {
        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().nullItem());

        Boolean result = documentService.existsAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertFalse(result);
    }

    // ---- updateDocumentUpdatedAt ----

    @Test
    void updateDocumentUpdatedAt_shouldCompleteSuccessfully() {
        when(documentRepository.updateUpdatedAt(eq(ONBOARDING_ID), any()))
                .thenReturn(Uni.createFrom().item(1L));

        assertDoesNotThrow(() ->
                documentService.updateDocumentUpdatedAt(ONBOARDING_ID).await().indefinitely());

        verify(documentRepository).updateUpdatedAt(eq(ONBOARDING_ID), any());
    }

    // ---- updateDocumentContractFiles ----

    @Test
    void updateDocumentContractFiles_shouldReturnUpdatedCount() {
        Document doc = buildDocument();
        doc.setId(DOCUMENT_ID);
        doc.setContractSigned("/path/signed.pdf");
        doc.setContractFilename("contract.pdf");

        when(documentRepository.updateContractFiles(doc.getOnboardingId(), doc.getContractSigned(), doc.getContractFilename()))
                .thenReturn(Uni.createFrom().item(1L));

        Long result = documentService.updateDocumentContractFiles(doc)
                .await().indefinitely();

        assertEquals(1L, result);
        verify(documentRepository).updateContractFiles(doc.getOnboardingId(), doc.getContractSigned(), doc.getContractFilename());
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

        RestResponse<File> response = documentService.retrieveSignedFile(ONBOARDING_ID)
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

        RestResponse<File> response = documentService.retrieveSignedFile(ONBOARDING_ID)
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

        RestResponse<File> response = documentService.retrieveSignedFile(ONBOARDING_ID)
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

        RestResponse<File> response = documentService.retrieveSignedFile(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(response);
        assertEquals(RestResponse.Status.OK.getStatusCode(), response.getStatus());
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

        assertDoesNotThrow(() ->
                documentService.uploadAttachment(request, formItem).await().indefinitely());
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

        assertThrows(UpdateNotAllowedException.class, () ->
                documentService.uploadAttachment(request, formItem).await().indefinitely());
    }

    // ---- saveDocument ----

    @Test
    void saveDocument_shouldReturnAlreadyExists_whenContractDocumentAlreadyExists() {
        Document existingDoc = buildDocument();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(TokenType.INSTITUTION)
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .pdfFormatFilename("contract_%s.pdf")
                .productTitle("Product IO")
                .build();

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(existingDoc));

        DocumentBuilderResponse response = documentService.saveDocument(request).await().indefinitely();

        assertNotNull(response);
        assertTrue(response.isAlreadyExists());
        assertEquals(DOCUMENT_ID, response.getDocumentId());
    }

    @Test
    void saveDocument_shouldPersistNewContractDocument_whenDocumentNotExists() throws IOException {
        File tempPdf = createTempPdf();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(TokenType.INSTITUTION)
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .pdfFormatFilename("contract_%s.pdf")
                .productTitle("Product IO")
                .build();

        Document docForContract = buildDocument();
        Document newDoc = buildDocument();
        newDoc.setId("new-doc-id");

        // First call in handleContractDocument → no existing document
        // Second call in retrieveContract → document found to build path
        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().nullItem())
                .thenReturn(Uni.createFrom().item(docForContract));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempPdf);
        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(newDoc));

        DocumentBuilderResponse response = documentService.saveDocument(request).await().indefinitely();

        assertNotNull(response);
        assertFalse(response.isAlreadyExists());
        assertEquals("new-doc-id", response.getDocumentId());
    }

    @Test
    void saveDocument_shouldPersistAttachmentDocument_whenDocumentTypeIsAttachment() throws IOException {
        File tempPdf = createTempPdf();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(TokenType.ATTACHMENT)
                .documentName("myAttachment")
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .productTitle("Product IO")
                .build();

        Document attachDoc = buildDocument();
        attachDoc.setType(TokenType.ATTACHMENT);
        attachDoc.setName("myAttachment");
        attachDoc.setContractSigned("/path/to/attachment.pdf");

        Document newDoc = buildDocument();
        newDoc.setId("new-attach-doc-id");

        when(documentRepository.findAttachment(ONBOARDING_ID, TokenType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(attachDoc));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempPdf);
        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(newDoc));

        DocumentBuilderResponse response = documentService.saveDocument(request).await().indefinitely();

        assertNotNull(response);
        assertFalse(response.isAlreadyExists());
        assertEquals("new-attach-doc-id", response.getDocumentId());
    }

    // ---- persistDocumentForImport ----

    @Test
    void persistDocumentForImport_shouldPersistAndReturnDocument() {
        LocalDateTime now = LocalDateTime.now();

        OnboardingDocumentRequest request = new OnboardingDocumentRequest();
        request.setOnboardingId(ONBOARDING_ID);
        request.setProductId("prod-io");
        request.setContractFilePath("/path/to/contract.pdf");
        request.setContractFileName("contract.pdf");
        request.setContractCreatedAt(now);
        request.setTemplatePath("/templates/template.pdf");
        request.setTemplateVersion("1.0");

        Document persistedDoc = buildDocument();
        persistedDoc.setContractSigned("/path/to/contract.pdf");
        persistedDoc.setContractFilename("contract.pdf");
        persistedDoc.setCreatedAt(now);
        persistedDoc.setUpdatedAt(now);
        persistedDoc.setType(TokenType.INSTITUTION);

        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(persistedDoc));

        Document result = documentService.persistDocumentForImport(request).await().indefinitely();

        assertNotNull(result);
        assertEquals(ONBOARDING_ID, result.getOnboardingId());
        assertEquals("/path/to/contract.pdf", result.getContractSigned());
        assertEquals("contract.pdf", result.getContractFilename());
        assertEquals(TokenType.INSTITUTION, result.getType());
        verify(documentRepository).persist(any(Document.class));
    }

    @Test
    void persistDocumentForImport_shouldSetAllFieldsCorrectly() {
        LocalDateTime contractDate = LocalDateTime.of(2024, 6, 15, 10, 30);

        OnboardingDocumentRequest request = new OnboardingDocumentRequest();
        request.setOnboardingId(ONBOARDING_ID);
        request.setProductId("prod-pagopa");
        request.setContractFilePath("/imports/signed_contract.pdf");
        request.setContractFileName("signed_contract.pdf");
        request.setContractCreatedAt(contractDate);
        request.setTemplatePath("/templates/v2/template.pdf");
        request.setTemplateVersion("2.0");

        when(documentRepository.persist(any(Document.class)))
                .thenAnswer(inv -> Uni.createFrom().item(inv.getArgument(0, Document.class)));

        Document result = documentService.persistDocumentForImport(request).await().indefinitely();

        assertNotNull(result);
        assertEquals(ONBOARDING_ID, result.getOnboardingId());
        assertEquals("prod-pagopa", result.getProductId());
        assertEquals("/imports/signed_contract.pdf", result.getContractSigned());
        assertEquals("signed_contract.pdf", result.getContractFilename());
        assertEquals(contractDate, result.getCreatedAt());
        assertEquals(contractDate, result.getUpdatedAt());
        assertEquals(TokenType.INSTITUTION, result.getType());
        assertEquals("/templates/v2/template.pdf", result.getContractTemplate());
        assertEquals("2.0", result.getContractVersion());
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
