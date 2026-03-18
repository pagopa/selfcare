package it.pagopa.selfcare.document.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.model.dto.request.OnboardingDocumentRequest;
import it.pagopa.selfcare.document.model.dto.response.ContractSignedReport;
import it.pagopa.selfcare.document.model.dto.response.DocumentBuilderResponse;
import it.pagopa.selfcare.document.model.dto.response.DocumentResponse;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.mapper.DocumentMapper;
import it.pagopa.selfcare.document.service.DocumentService;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
class DocumentControllerTest {

  private static final String ONBOARDING_ID = "onboarding-123";
  private static final String DOCUMENT_ID = "doc-456";
  private static final String ATTACHMENT_NAME = "attachment.pdf";
  private static final String CONTRACT_SIGNED_PATH = "contracts/signed/contract-signed.pdf";

  @InjectMock DocumentService documentService;
  @InjectMock DocumentMapper documentMapper;

  @Test
  void getDocumentByOnboardingId_shouldReturnDocuments_whenFound() {
    Document document = new Document();
    document.setId(DOCUMENT_ID);
    document.setOnboardingId(ONBOARDING_ID);

    DocumentResponse response = new DocumentResponse();
    response.setId(DOCUMENT_ID);

    Mockito.when(documentService.getDocumentsByOnboardingId(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().item(List.of(document)));
    Mockito.when(documentMapper.toResponse(document)).thenReturn(response);

    given()
        .when()
        .get("/v1/documents/onboarding/" + ONBOARDING_ID)
        .then()
        .statusCode(200)
        .body("$.size()", is(1))
        .body("[0].id", equalTo(DOCUMENT_ID));
  }

  @Test
  void getDocumentByOnboardingId_shouldReturnEmptyList_whenNoDocumentsFound() {
    Mockito.when(documentService.getDocumentsByOnboardingId(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().item(Collections.emptyList()));

    given()
        .when()
        .get("/v1/documents/onboarding/" + ONBOARDING_ID)
        .then()
        .statusCode(200)
        .body("$.size()", is(0));
  }

  @Test
  void getDocumentByOnboardingId_shouldReturnMultipleDocuments_whenMultipleExist() {
    Document doc1 = new Document();
    doc1.setId("doc-1");
    Document doc2 = new Document();
    doc2.setId("doc-2");

    DocumentResponse resp1 = new DocumentResponse();
    resp1.setId("doc-1");
    DocumentResponse resp2 = new DocumentResponse();
    resp2.setId("doc-2");

    Mockito.when(documentService.getDocumentsByOnboardingId(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().item(Arrays.asList(doc1, doc2)));
    Mockito.when(documentMapper.toResponse(doc1)).thenReturn(resp1);
    Mockito.when(documentMapper.toResponse(doc2)).thenReturn(resp2);

    given()
        .when()
        .get("/v1/documents/onboarding/" + ONBOARDING_ID)
        .then()
        .statusCode(200)
        .body("$.size()", is(2));
  }

  @Test
  void getDocumentById_shouldReturnDocument_whenFound() {
    Document document = new Document();
    document.setId(DOCUMENT_ID);

    DocumentResponse response = new DocumentResponse();
    response.setId(DOCUMENT_ID);

    Mockito.when(documentService.getDocumentById(DOCUMENT_ID))
        .thenReturn(Uni.createFrom().item(document));
    Mockito.when(documentMapper.toResponse(document)).thenReturn(response);

    given()
        .when()
        .get("/v1/documents/" + DOCUMENT_ID)
        .then()
        .statusCode(200)
        .body("id", equalTo(DOCUMENT_ID));
  }

  @Test
  void updateContractSigned_shouldReturnNoContent_whenUpdateSuccessful() {
    Mockito.when(documentService.updateContractSigned(ONBOARDING_ID, CONTRACT_SIGNED_PATH))
        .thenReturn(Uni.createFrom().item(1L));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .queryParam("contractSigned", CONTRACT_SIGNED_PATH)
        .put("/v1/documents/" + ONBOARDING_ID + "/contract-signed")
        .then()
        .statusCode(204);
  }

  @Test
  void updateContractSigned_shouldReturnNotFound_whenDocumentNotFound() {
    Mockito.when(documentService.updateContractSigned(ONBOARDING_ID, CONTRACT_SIGNED_PATH))
        .thenReturn(Uni.createFrom().item(0L));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .queryParam("contractSigned", CONTRACT_SIGNED_PATH)
        .put("/v1/documents/" + ONBOARDING_ID + "/contract-signed")
        .then()
        .statusCode(404);
  }

  @Test
  void updateContractSigned_shouldReturnBadRequest_whenContractSignedParamMissing() {
    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .put("/v1/documents/" + ONBOARDING_ID + "/contract-signed")
        .then()
        .statusCode(400);
  }



  @Test
  void reportContractSigned_shouldReturnReport_whenReportAvailable() {
    ContractSignedReport report = new ContractSignedReport();
    report.setCades(true);

    Mockito.when(documentService.reportContractSigned(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().item(report));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .queryParam("onboardingId", ONBOARDING_ID)
        .get("/v1/documents/contract-report")
        .then()
        .statusCode(200)
        .body("cades", equalTo(true));
  }

  @Test
  void reportContractSigned_shouldReturnBadRequest_whenOnboardingIdMissing() {
    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .get("/v1/documents/contract-report")
        .then()
        .statusCode(400);
  }

  @Test
  void headAttachment_shouldReturnNoContent_whenAttachmentExists() {
    Mockito.when(documentService.existsAttachment(ONBOARDING_ID, ATTACHMENT_NAME))
        .thenReturn(Uni.createFrom().item(true));

    given()
        .when()
        .queryParam("name", ATTACHMENT_NAME)
        .head("/v1/documents/" + ONBOARDING_ID + "/attachment/status")
        .then()
        .statusCode(204);
  }

  @Test
  void headAttachment_shouldReturnNotFound_whenAttachmentDoesNotExist() {
    Mockito.when(documentService.existsAttachment(ONBOARDING_ID, ATTACHMENT_NAME))
        .thenReturn(Uni.createFrom().item(false));

    given()
        .when()
        .queryParam("name", ATTACHMENT_NAME)
        .head("/v1/documents/" + ONBOARDING_ID + "/attachment/status")
        .then()
        .statusCode(404);
  }

  @Test
  void headAttachment_shouldReturnBadRequest_whenAttachmentNameMissing() {
    given()
        .when()
        .head("/v1/documents/" + ONBOARDING_ID + "/attachment/status")
        .then()
        .statusCode(400);
  }

  @Test
  void updateDocumentContractFiles_shouldReturnNoContent_whenUpdateSuccessful() {
    Document document = new Document();
    document.setId(DOCUMENT_ID);
    document.setContractSigned(CONTRACT_SIGNED_PATH);

    Mockito.when(documentService.updateDocumentContractFiles(any(Document.class)))
        .thenReturn(Uni.createFrom().item(1L));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(document)
        .when()
        .put("/v1/documents/contract-files")
        .then()
        .statusCode(204);
  }

  @Test
  void updateDocumentContractFiles_shouldReturnNotFound_whenDocumentNotFound() {
    Document document = new Document();
    document.setId(DOCUMENT_ID);

    Mockito.when(documentService.updateDocumentContractFiles(any(Document.class)))
        .thenReturn(Uni.createFrom().item(0L));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(document)
        .when()
        .put("/v1/documents/contract-files")
        .then()
        .statusCode(404);
  }

  @Test
  void saveDocument_shouldReturnCreated_whenDocumentSavedSuccessfully() {
    DocumentBuilderRequest request = DocumentBuilderRequest.builder()
            .onboardingId(ONBOARDING_ID)
            .productId("prod-123")
            .documentType(it.pagopa.selfcare.onboarding.common.TokenType.INSTITUTION)
            .build();

    DocumentBuilderResponse response = new DocumentBuilderResponse();
    response.setDocumentId(DOCUMENT_ID);
    response.setAlreadyExists(false);

    Mockito.when(documentService.saveDocument(any(DocumentBuilderRequest.class)))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .post("/v1/documents")
        .then()
        .statusCode(201)
        .body("documentId", equalTo(DOCUMENT_ID))
        .body("alreadyExists", equalTo(false));
  }

  @Test
  void saveDocument_shouldReturnCreated_whenDocumentAlreadyExists() {
    DocumentBuilderRequest request = DocumentBuilderRequest.builder()
            .onboardingId(ONBOARDING_ID)
            .productId("prod-123")
            .documentType(it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT)
            .build();

    DocumentBuilderResponse response = new DocumentBuilderResponse();
    response.setDocumentId(DOCUMENT_ID);
    response.setAlreadyExists(true);

    Mockito.when(documentService.saveDocument(any(DocumentBuilderRequest.class)))
        .thenReturn(Uni.createFrom().item(response));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .post("/v1/documents")
        .then()
        .statusCode(201)
        .body("alreadyExists", equalTo(true));
  }

  @Test
  void persistDocumentForImport_shouldReturnCreated_whenDocumentPersistedSuccessfully() {
    OnboardingDocumentRequest request = new OnboardingDocumentRequest();
    request.setOnboardingId(ONBOARDING_ID);
    request.setProductId("prod-123");
    request.setContractFilePath("/contracts/contract.pdf");

    Document document = new Document();
    document.setId(DOCUMENT_ID);
    document.setOnboardingId(ONBOARDING_ID);

    Mockito.when(documentService.persistDocumentForImport(any(OnboardingDocumentRequest.class)))
        .thenReturn(Uni.createFrom().item(document));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .post("/v1/documents/import")
        .then()
        .statusCode(201);
  }

  @Test
  void updateDocumentUpdatedAt_shouldReturnNoContent_whenUpdateSuccessful() {
    Mockito.when(documentService.updateDocumentUpdatedAt(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .put("/v1/documents/" + ONBOARDING_ID + "/updated-at")
        .then()
        .statusCode(204);
  }

  @Test
  void updateDocumentUpdatedAt_shouldHandleNonExistentOnboarding() {
    Mockito.when(documentService.updateDocumentUpdatedAt("non-existent"))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .put("/v1/documents/non-existent/updated-at")
        .then()
        .statusCode(204);
  }

  @Test
  void getDocumentByOnboardingId_shouldReturnInternalServerError_whenServiceFails() {
    Mockito.when(documentService.getDocumentsByOnboardingId(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Database connection error")));

    given()
        .when()
        .get("/v1/documents/onboarding/" + ONBOARDING_ID)
        .then()
        .statusCode(500);
  }

  @Test
  void getDocumentById_shouldReturnInternalServerError_whenServiceFails() {
    Mockito.when(documentService.getDocumentById(DOCUMENT_ID))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

    given()
        .when()
        .get("/v1/documents/" + DOCUMENT_ID)
        .then()
        .statusCode(500);
  }

  @Test
  void updateContractSigned_shouldReturnInternalServerError_whenServiceFails() {
    Mockito.when(documentService.updateContractSigned(ONBOARDING_ID, CONTRACT_SIGNED_PATH))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .queryParam("contractSigned", CONTRACT_SIGNED_PATH)
        .put("/v1/documents/" + ONBOARDING_ID + "/contract-signed")
        .then()
        .statusCode(500);
  }

  @Test
  void reportContractSigned_shouldReturnInternalServerError_whenServiceFails() {
    Mockito.when(documentService.reportContractSigned(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Report generation error")));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .queryParam("onboardingId", ONBOARDING_ID)
        .get("/v1/documents/contract-report")
        .then()
        .statusCode(500);
  }

  @Test
  void headAttachment_shouldReturnInternalServerError_whenServiceFails() {
    Mockito.when(documentService.existsAttachment(ONBOARDING_ID, ATTACHMENT_NAME))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

    given()
        .when()
        .queryParam("name", ATTACHMENT_NAME)
        .head("/v1/documents/" + ONBOARDING_ID + "/attachment/status")
        .then()
        .statusCode(500);
  }

  @Test
  void updateDocumentContractFiles_shouldReturnInternalServerError_whenServiceFails() {
    Document document = new Document();
    document.setId(DOCUMENT_ID);

    Mockito.when(documentService.updateDocumentContractFiles(any(Document.class)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(document)
        .when()
        .put("/v1/documents/contract-files")
        .then()
        .statusCode(500);
  }

  @Test
  void saveDocument_shouldReturnBadRequest_whenRequestIsInvalid() {
    DocumentBuilderRequest invalidRequest = DocumentBuilderRequest.builder()
            .productId("prod-123")
            .documentType(it.pagopa.selfcare.onboarding.common.TokenType.INSTITUTION)
            .build();

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(invalidRequest)
        .when()
        .post("/v1/documents")
        .then()
        .statusCode(400);
  }

  @Test
  void saveDocument_shouldReturnInternalServerError_whenServiceFails() {
    DocumentBuilderRequest request = DocumentBuilderRequest.builder()
            .onboardingId(ONBOARDING_ID)
            .productId("prod-123")
            .documentType(it.pagopa.selfcare.onboarding.common.TokenType.INSTITUTION)
            .build();

    Mockito.when(documentService.saveDocument(any(DocumentBuilderRequest.class)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .post("/v1/documents")
        .then()
        .statusCode(500);
  }

  @Test
  void persistDocumentForImport_shouldReturnBadRequest_whenRequestIsInvalid() {
    OnboardingDocumentRequest invalidRequest = new OnboardingDocumentRequest();
    invalidRequest.setProductId("prod-123");

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(invalidRequest)
        .when()
        .post("/v1/documents/import")
        .then()
        .statusCode(400);
  }

  @Test
  void persistDocumentForImport_shouldReturnInternalServerError_whenServiceFails() {
    OnboardingDocumentRequest request = new OnboardingDocumentRequest();
    request.setOnboardingId(ONBOARDING_ID);
    request.setProductId("prod-123");
    request.setContractFilePath("/contracts/contract.pdf");

    Mockito.when(documentService.persistDocumentForImport(any(OnboardingDocumentRequest.class)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .post("/v1/documents/import")
        .then()
        .statusCode(500);
  }

  @Test
  void updateDocumentUpdatedAt_shouldReturnInternalServerError_whenServiceFails() {
    Mockito.when(documentService.updateDocumentUpdatedAt(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .put("/v1/documents/" + ONBOARDING_ID + "/updated-at")
        .then()
        .statusCode(500);
  }

  @Test
  void saveDocument_shouldReturnBadRequest_whenDocumentTypeIsNull() {
    DocumentBuilderRequest invalidRequest = DocumentBuilderRequest.builder()
            .onboardingId(ONBOARDING_ID)
            .productId("prod-123")
            .build();

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(invalidRequest)
        .when()
        .post("/v1/documents")
        .then()
        .statusCode(400);
  }

  @Test
  void saveDocument_shouldReturnBadRequest_whenProductIdIsNull() {
    DocumentBuilderRequest invalidRequest = DocumentBuilderRequest.builder()
            .onboardingId(ONBOARDING_ID)
            .documentType(it.pagopa.selfcare.onboarding.common.TokenType.INSTITUTION)
            .build();

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(invalidRequest)
        .when()
        .post("/v1/documents")
        .then()
        .statusCode(400);
  }

  @Test
  void persistDocumentForImport_shouldReturnBadRequest_whenContractFilePathIsNull() {
    OnboardingDocumentRequest invalidRequest = new OnboardingDocumentRequest();
    invalidRequest.setOnboardingId(ONBOARDING_ID);
    invalidRequest.setProductId("prod-123");

    given()
        .contentType(MediaType.APPLICATION_JSON)
        .body(invalidRequest)
        .when()
        .post("/v1/documents/import")
        .then()
        .statusCode(400);
  }

  @Test
  void getAttachments_shouldReturnAttachmentList_whenAttachmentsExist() {
    List<String> attachments = Arrays.asList("attachment1.pdf", "attachment2.pdf");

    Mockito.when(documentService.getAttachments(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().item(attachments));

    given()
        .when()
        .get("/v1/documents/" + ONBOARDING_ID + "/attachment-list")
        .then()
        .statusCode(200)
        .body("size()", is(2))
        .body("[0]", equalTo("attachment1.pdf"))
        .body("[1]", equalTo("attachment2.pdf"));
  }

  @Test
  void getAttachments_shouldReturnEmptyList_whenNoAttachmentsExist() {
    Mockito.when(documentService.getAttachments(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().item(Collections.emptyList()));

    given()
        .when()
        .get("/v1/documents/" + ONBOARDING_ID + "/attachment-list")
        .then()
        .statusCode(200)
        .body("size()", is(0));
  }

  @Test
  void getAttachments_shouldReturnInternalServerError_whenServiceFails() {
    Mockito.when(documentService.getAttachments(ONBOARDING_ID))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

    given()
        .when()
        .get("/v1/documents/" + ONBOARDING_ID + "/attachment-list")
        .then()
        .statusCode(500);
  }
}
