package it.pagopa.selfcare.document.service;

import com.azure.storage.blob.models.BlobProperties;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.azurestorage.error.SelfcareAzureStorageException;
import it.pagopa.selfcare.document.config.DocumentMsConfig;
import it.pagopa.selfcare.document.exception.ResourceNotFoundException;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.model.dto.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.model.dto.response.ContractSignedReport;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.repository.DocumentRepository;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import jakarta.inject.Inject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class DocumentServiceImplTest {

    private static final String ONBOARDING_ID = "onboardingId";
    private static final String DOCUMENT_ID = new ObjectId().toHexString();

    @Inject
    DocumentService documentService;

    @InjectMock
    AzureBlobClient azureBlobClient;

    @InjectMock
    DocumentMsConfig documentMsConfig;

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
        when(documentRepository.findById(anyString()))
                .thenReturn(Uni.createFrom().item(doc));

        Document result = documentService.getDocumentById(DOCUMENT_ID)
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(ONBOARDING_ID, result.getOnboardingId());
    }

    @Test
    void getDocumentById_shouldThrowResourceNotFoundWhenDocumentIsNull() {
        when(documentRepository.findById(anyString()))
                .thenReturn(Uni.createFrom().nullItem());

        var awaiter = documentService.getDocumentById(DOCUMENT_ID).await();

        assertThrows(ResourceNotFoundException.class, awaiter::indefinitely);
    }

    // ---- getAttachments ----

    @Test
    void getAttachments_shouldReturnAttachmentNames() {
        Document doc1 = buildDocument();
        doc1.setType(DocumentType.ATTACHMENT);
        doc1.setAttachmentName("attachment-1");

        Document doc2 = buildDocument();
        doc2.setType(DocumentType.ATTACHMENT);
        doc2.setAttachmentName("attachment-2");

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

        when(documentRepository.findAttachment(ONBOARDING_ID, DocumentType.ATTACHMENT.name(), "myAttachment"))
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

        when(documentRepository.findAttachment(ONBOARDING_ID, DocumentType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(doc));
        Mockito.doThrow(new SelfcareAzureStorageException("Not found", "404"))
                .when(azureBlobClient).getProperties(anyString());

        Boolean result = documentService.existsAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertFalse(result);
    }

    @Test
    void existsAttachment_shouldReturnFalse_whenDocumentIsNull() {
        when(documentRepository.findAttachment(ONBOARDING_ID, DocumentType.ATTACHMENT.name(), "myAttachment"))
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

    // ---- saveDocument ----

    @Test
    void saveDocument_shouldReturnAlreadyExists_whenContractDocumentAlreadyExists() {
        Document existingDoc = buildDocument();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(DocumentType.INSTITUTION)
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .productTitle("Product IO")
                .build();

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(existingDoc));

        Document response = documentService.saveDocument(request).await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void saveDocument_shouldPersistNewContractDocument_whenDocumentNotExists() throws IOException {
        File tempPdf = createTempPdf();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(DocumentType.INSTITUTION)
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
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

        Document response = documentService.saveDocument(request).await().indefinitely();

        assertNotNull(response);
    }

    @Test
    void saveDocument_shouldPersistAttachmentDocument_whenDocumentTypeIsAttachment() throws IOException {
        File tempPdf = createTempPdf();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(DocumentType.ATTACHMENT)
                .attachmentName("myAttachment")
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .productTitle("Product IO")
                .build();

        Document attachDoc = buildDocument();
        attachDoc.setType(DocumentType.ATTACHMENT);
        attachDoc.setAttachmentName("myAttachment");
        attachDoc.setContractSigned("/path/to/attachment.pdf");

        Document newDoc = buildDocument();
        newDoc.setId("new-attach-doc-id");

        when(documentRepository.findAttachment(ONBOARDING_ID, DocumentType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(attachDoc));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempPdf);
        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(newDoc));

        Document response = documentService.saveDocument(request).await().indefinitely();

        assertNotNull(response);
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
        persistedDoc.setType(DocumentType.INSTITUTION);

        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(persistedDoc));

        Document result = documentService.persistDocumentForImport(request).await().indefinitely();

        assertNotNull(result);
        assertEquals(ONBOARDING_ID, result.getOnboardingId());
        assertEquals("/path/to/contract.pdf", result.getContractSigned());
        assertEquals("contract.pdf", result.getContractFilename());
        assertEquals(DocumentType.INSTITUTION, result.getType());
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
        assertEquals(DocumentType.INSTITUTION, result.getType());
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
        doc.setType(DocumentType.INSTITUTION);
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

    // ---- saveDocument for USER type ----

    @Test
    void saveDocument_shouldPersistNewUserDocument_whenDocumentTypeIsUser() throws IOException {
        File tempPdf = createTempPdf();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(DocumentType.USER)
                .templatePath("/templates/template.pdf")
                .templateVersion("1.0")
                .productTitle("Product IO")
                .build();

        Document docForContract = buildDocument();
        Document newDoc = buildDocument();
        newDoc.setId("new-user-doc-id");
        newDoc.setType(DocumentType.USER);

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().nullItem())
                .thenReturn(Uni.createFrom().item(docForContract));
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempPdf);
        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(newDoc));

        Document response = documentService.saveDocument(request).await().indefinitely();

        assertNotNull(response);
    }

    // ---- saveDocument attachment not found ----

    @Test
    void saveDocument_shouldCreateAttachment_whenAttachmentNotFoundInDb() throws Exception {
        // Arrange
        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-io")
                .documentType(DocumentType.ATTACHMENT)
                .attachmentName("missingAttachment")
                .templatePath("templates/template.pdf")
                .templateVersion("1.0")
                .productTitle("Product IO")
                .build();

        when(documentRepository.findAttachment(ONBOARDING_ID, DocumentType.ATTACHMENT.name(), "missingAttachment"))
                .thenReturn(Uni.createFrom().nullItem());

        when(documentMsConfig.getContractPath()).thenReturn("/contracts/");

        File tempPdf = createTempPdf();
        when(azureBlobClient.getFileAsPdf(anyString())).thenReturn(tempPdf);

        Document mockPersistedDoc = new Document();
        mockPersistedDoc.setOnboardingId(ONBOARDING_ID);
        mockPersistedDoc.setChecksum("calculated-digest-mock");

        when(documentRepository.persist(any(Document.class)))
                .thenReturn(Uni.createFrom().item(mockPersistedDoc));

        // Act
        Document result = documentService.saveDocument(request).await().indefinitely();

        // Assert
        assertNotNull(result);
        assertEquals(ONBOARDING_ID, result.getOnboardingId());
        verify(azureBlobClient).getFileAsPdf(anyString());
        verify(documentRepository).persist(any(Document.class));
    }

    // ---- reportContractSigned edge cases ----

    @Test
    void reportContractSigned_shouldReturnCadesFalse_whenSignatureVerificationFails() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/signed.pdf");
        File mockFile = Mockito.mock(File.class);

        when(documentRepository.findByOnboardingId(ONBOARDING_ID))
                .thenReturn(Uni.createFrom().item(doc));
        when(azureBlobClient.getFileAsPdf(doc.getContractSigned())).thenReturn(mockFile);
        when(signatureService.verifySignature(any(File.class)))
                .thenThrow(new RuntimeException("Signature verification failed"));

        ContractSignedReport report = documentService.reportContractSigned(ONBOARDING_ID)
                .await().indefinitely();

        assertNotNull(report);
        assertFalse(report.isCades());
    }

    // ---- existsAttachment when storage throws exception ----

    @Test
    void existsAttachment_shouldReturnFalse_whenStorageThrowsException() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/to/attachment.pdf");

        when(documentRepository.findAttachment(ONBOARDING_ID, DocumentType.ATTACHMENT.name(), "myAttachment"))
                .thenReturn(Uni.createFrom().item(doc));
        doThrow(new SelfcareAzureStorageException("Storage error", "500"))
                .when(azureBlobClient).getProperties(anyString());

        Boolean result = documentService.existsAttachment(ONBOARDING_ID, "myAttachment")
                .await().indefinitely();

        assertFalse(result);
    }

    // ---- updateContractSigned with different paths ----

    @Test
    void updateContractSigned_shouldReturnZero_whenNoDocumentUpdated() {
        when(documentRepository.updateContractSignedByOnboardingId(ONBOARDING_ID, "/new/path/signed.pdf"))
                .thenReturn(Uni.createFrom().item(0L));

        Long result = documentService.updateContractSigned(ONBOARDING_ID, "/new/path/signed.pdf")
                .await().indefinitely();

        assertEquals(0L, result);
    }

    // ---- updateDocumentContractFiles ----

    @Test
    void updateDocumentContractFiles_shouldReturnZero_whenNoDocumentUpdated() {
        Document doc = buildDocument();
        doc.setContractSigned("/path/signed.pdf");
        doc.setContractFilename("signed.pdf");

        when(documentRepository.updateContractFiles(doc.getOnboardingId(), doc.getContractSigned(), doc.getContractFilename()))
                .thenReturn(Uni.createFrom().item(0L));

        Long result = documentService.updateDocumentContractFiles(doc)
                .await().indefinitely();

        assertEquals(0L, result);
    }
}
