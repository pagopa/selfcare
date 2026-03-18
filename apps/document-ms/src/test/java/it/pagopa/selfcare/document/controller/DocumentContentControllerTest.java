package it.pagopa.selfcare.document.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import it.pagopa.selfcare.document.service.DocumentContentService;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class DocumentContentControllerTest {

    private static final String DOCUMENT_ID = "doc-456";
    private static final String ONBOARDING_ID = "onboarding-123";
    private static final String ATTACHMENT_NAME = "attachment.pdf";
    private static final String TEMPLATE_PATH = "templates/contract.ftl";
    private static final String INSTITUTION_DESCRIPTION = "Test Institution";
    private static final String PRODUCT_ID = "Product-123";
    private static final String PRODUCT_NAME = "PagoPA";
    private static final String CONTRACT_TEMPLATE_PATH = "templates/contract.ftl";
    private static final String ATTACHMENT_TEMPLATE_PATH = "templates/attachment.ftl";
    private static final String PDF_FORMAT_FILENAME = "contract-%s.pdf";
    private static final String STORAGE_PATH = "contracts/signed/contract-123.pdf";
    private static final String FILENAME = "contract-123.pdf";

    @InjectMock
    DocumentContentService documentContentService;

    @Test
    void getContractSigned_shouldReturnFile_whenSignedContractExists() throws Exception {
        File tempFile = Files.createTempFile("signed", ".pdf").toFile();
        tempFile.deleteOnExit();

        Mockito.when(documentContentService.retrieveSignedFile(DOCUMENT_ID))
                .thenReturn(Uni.createFrom().item(RestResponse.ok(tempFile)));

        given()
                .when()
                .get("/v1/document-content/" + DOCUMENT_ID + "/contract-signed")
                .then()
                .statusCode(200);
    }

    @Test
    void getContractSigned_shouldReturnInternalServerError_whenServiceFails() {
        Mockito.when(documentContentService.retrieveSignedFile(DOCUMENT_ID))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

        given()
                .when()
                .get("/v1/document-content/" + DOCUMENT_ID + "/contract-signed")
                .then()
                .statusCode(500);
    }

    @Test
    void getTemplateAttachment_shouldReturnFile_whenTemplateExists() throws Exception {
        File tempFile = Files.createTempFile("template", ".pdf").toFile();
        tempFile.deleteOnExit();

        Mockito.when(
                documentContentService.retrieveTemplateAttachment(
                    ONBOARDING_ID, TEMPLATE_PATH, ATTACHMENT_NAME, INSTITUTION_DESCRIPTION, PRODUCT_ID))
            .thenReturn(Uni.createFrom().item(RestResponse.ok(tempFile)));

        given()
                .when()
                .queryParam("templatePath", TEMPLATE_PATH)
                .queryParam("name", ATTACHMENT_NAME)
                .queryParam("institutionDescription", INSTITUTION_DESCRIPTION)
                .queryParam("productId", PRODUCT_ID)
                .get("/v1/document-content/" + ONBOARDING_ID + "/template-attachment")
                .then()
                .statusCode(200);
    }

    @Test
    void getTemplateAttachment_shouldReturnInternalServerError_whenServiceFails() {
        Mockito.when(
                documentContentService.retrieveTemplateAttachment(
                    ONBOARDING_ID, TEMPLATE_PATH, ATTACHMENT_NAME, INSTITUTION_DESCRIPTION, PRODUCT_ID))
            .thenReturn(Uni.createFrom().failure(new RuntimeException("Template processing error")));

        given()
                .when()
                .queryParam("templatePath", TEMPLATE_PATH)
                .queryParam("name", ATTACHMENT_NAME)
                .queryParam("institutionDescription", INSTITUTION_DESCRIPTION)
                .queryParam("productId", PRODUCT_ID)
                .get("/v1/document-content/" + ONBOARDING_ID + "/template-attachment")
                .then()
                .statusCode(500);
    }

    @Test
    void getAttachment_shouldReturnFile_whenAttachmentExists() throws Exception {
        File tempFile = Files.createTempFile("attachment", ".pdf").toFile();
        tempFile.deleteOnExit();

        Mockito.when(documentContentService.retrieveAttachment(ONBOARDING_ID, ATTACHMENT_NAME))
                .thenReturn(Uni.createFrom().item(RestResponse.ok(tempFile)));

        given()
                .when()
                .queryParam("name", ATTACHMENT_NAME)
                .get("/v1/document-content/" + ONBOARDING_ID + "/attachment")
                .then()
                .statusCode(200);
    }

    @Test
    void getAttachment_shouldReturnInternalServerError_whenServiceFails() {
        Mockito.when(documentContentService.retrieveAttachment(ONBOARDING_ID, ATTACHMENT_NAME))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

        given()
                .when()
                .queryParam("name", ATTACHMENT_NAME)
                .get("/v1/document-content/" + ONBOARDING_ID + "/attachment")
                .then()
                .statusCode(500);
    }

    @Test
    void getAttachment_shouldReturnBadRequest_whenAttachmentNameMissing() {
        given()
                .when()
                .get("/v1/document-content/" + ONBOARDING_ID + "/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void uploadAttachment_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        File tempFile = Files.createTempFile("upload", ".pdf").toFile();
        tempFile.deleteOnExit();

        DocumentBuilderRequest invalidRequest = DocumentBuilderRequest.builder()
                .productId("prod-123")
                .documentType(it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT)
                .build();

        given()
                .multiPart("file", tempFile, "application/pdf")
                .multiPart("request", invalidRequest, "application/json")
                .when()
                .post("/v1/document-content/upload-attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void getTemplateAttachment_shouldReturnBadRequest_whenMissingTemplatePath() {
        given()
                .when()
                .queryParam("name", ATTACHMENT_NAME)
                .get("/v1/document-content/" + ONBOARDING_ID + "/template-attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void getTemplateAttachment_shouldReturnBadRequest_whenMissingName() {
        given()
                .when()
                .queryParam("templatePath", TEMPLATE_PATH)
                .get("/v1/document-content/" + ONBOARDING_ID + "/template-attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void uploadAttachment_shouldReturnNoContent_whenUploadSuccessful() throws Exception {
        File tempFile = Files.createTempFile("upload", ".pdf").toFile();
        tempFile.deleteOnExit();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-123")
                .documentType(it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT)
                .documentName(ATTACHMENT_NAME)
                .build();

        Mockito.when(documentContentService.uploadAttachment(any(DocumentBuilderRequest.class), any(FormItem.class)))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .multiPart("file", tempFile, "application/pdf")
                .multiPart("request", request, "application/json")
                .when()
                .post("/v1/document-content/upload-attachment")
                .then()
                .statusCode(204);
    }

    @Test
    void uploadAttachment_shouldReturnConflict_whenAttachmentAlreadyExists() throws Exception {
        File tempFile = Files.createTempFile("upload", ".pdf").toFile();
        tempFile.deleteOnExit();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-123")
                .documentType(it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT)
                .documentName(ATTACHMENT_NAME)
                .build();

        Mockito.when(documentContentService.uploadAttachment(any(DocumentBuilderRequest.class), any(FormItem.class)))
                .thenReturn(Uni.createFrom().failure(new UpdateNotAllowedException("Attachment already exists")));

        given()
                .multiPart("file", tempFile, "application/pdf")
                .multiPart("request", request, "application/json")
                .when()
                .post("/v1/document-content/upload-attachment")
                .then()
                .statusCode(409);
    }

    @Test
    void uploadAttachment_shouldReturnInternalServerError_whenServiceFails() throws Exception {
        File tempFile = Files.createTempFile("upload", ".pdf").toFile();
        tempFile.deleteOnExit();

        DocumentBuilderRequest request = DocumentBuilderRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .productId("prod-123")
                .documentType(it.pagopa.selfcare.onboarding.common.TokenType.ATTACHMENT)
                .documentName(ATTACHMENT_NAME)
                .build();

        Mockito.when(documentContentService.uploadAttachment(any(DocumentBuilderRequest.class), any(FormItem.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

        given()
                .multiPart("file", tempFile, "application/pdf")
                .multiPart("request", request, "application/json")
                .when()
                .post("/v1/document-content/upload-attachment")
                .then()
                .statusCode(500);
    }

    @Test
    void saveVisuraForMerchant_shouldReturnNoContent_whenUploadSuccessful() {
        UploadVisuraRequest request = UploadVisuraRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .filename("VISURA_test.xml")
                .fileContent(new byte[]{1, 2, 3})
                .build();

        Mockito.when(documentContentService.saveVisuraForMerchant(any(UploadVisuraRequest.class)))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/visura")
                .then()
                .statusCode(204);
    }

    @Test
    void saveVisuraForMerchant_shouldReturnInternalServerError_whenServiceFails() {
        UploadVisuraRequest request = UploadVisuraRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .filename("VISURA_test.xml")
                .fileContent(new byte[]{1, 2, 3})
                .build();

        Mockito.when(documentContentService.saveVisuraForMerchant(any(UploadVisuraRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Generic error")));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/visura")
                .then()
                .statusCode(500);
    }

    @Test
    void getContract_shouldReturnFile_whenContractExists() throws Exception {
        File tempFile = Files.createTempFile("contract", ".pdf").toFile();
        tempFile.deleteOnExit();

        Mockito.when(documentContentService.retrieveContract(ONBOARDING_ID, Boolean.FALSE))
                .thenReturn(Uni.createFrom().item(RestResponse.ok(tempFile)));

        given()
                .when()
                .get("/v1/document-content/" + ONBOARDING_ID + "/contract")
                .then()
                .statusCode(200);
    }

    @Test
    void getContract_shouldReturnInternalServerError_whenServiceFails() {
        Mockito.when(documentContentService.retrieveContract(ONBOARDING_ID, Boolean.FALSE))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

        given()
                .when()
                .get("/v1/document-content/" + ONBOARDING_ID + "/contract")
                .then()
                .statusCode(500);
    }

    // ============================================
    // createContractPdf - Success scenarios
    // ============================================

    @Test
    void createContractPdf_shouldReturnSuccess_whenValidRequestProvided() {
        CreateContractPdfRequest request = buildValidContractRequest();
        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename(FILENAME)
                .build();

        when(documentContentService.createContractPdf(any(CreateContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(200)
                .body("storagePath", equalTo(STORAGE_PATH))
                .body("filename", equalTo(FILENAME));

        verify(documentContentService).createContractPdf(any(CreateContractPdfRequest.class));
    }

    @Test
    void createContractPdf_shouldReturnSuccess_whenRequestHasDelegates() {
        CreateContractPdfRequest request = buildValidContractRequest();
        UserPdfData delegate1 = buildValidUserPdfData("delegate-1", "DLGTAX001");
        UserPdfData delegate2 = buildValidUserPdfData("delegate-2", "DLGTAX002");
        request.setDelegates(List.of(delegate1, delegate2));

        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename(FILENAME)
                .build();

        when(documentContentService.createContractPdf(any(CreateContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(200)
                .body("storagePath", notNullValue());
    }

    @Test
    void createContractPdf_shouldReturnSuccess_whenRequestHasOptionalFields() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setPricingPlan("PREMIUM");
        request.setIsAggregator(true);
        request.setAggregatesCsvBaseUrl("https://example.com/aggregates");

        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename(FILENAME)
                .build();

        when(documentContentService.createContractPdf(any(CreateContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(200);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_whenRequestHasEmptyDelegatesList() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setDelegates(Collections.emptyList());

        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename(FILENAME)
                .build();

        when(documentContentService.createContractPdf(any(CreateContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(200);
    }

    // ============================================
    // createContractPdf - Validation error scenarios
    // ============================================

    @Test
    void createContractPdf_shouldReturn400_whenOnboardingIdIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setOnboardingId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);

        verify(documentContentService, never()).createContractPdf(any());
    }

    @Test
    void createContractPdf_shouldReturn400_whenOnboardingIdIsBlank() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setOnboardingId("   ");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);

        verify(documentContentService, never()).createContractPdf(any());
    }

    @Test
    void createContractPdf_shouldReturn400_whenContractTemplatePathIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setContractTemplatePath(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenProductIdIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setProductId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenProductNameIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setProductName(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenPdfFormatFilenameIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setPdfFormatFilename(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenInstitutionIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setInstitution(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenManagerIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.setManager(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenManagerTaxCodeIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.getManager().setTaxCode(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenManagerIdIsNull() {
        CreateContractPdfRequest request = buildValidContractRequest();
        request.getManager().setId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(400);
    }

    // ============================================
    // createContractPdf - Service error scenarios
    // ============================================

    @Test
    void createContractPdf_shouldReturn500_whenServiceThrowsException() {
        CreateContractPdfRequest request = buildValidContractRequest();

        when(documentContentService.createContractPdf(any(CreateContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("PDF generation failed")));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/contract")
                .then()
                .statusCode(500);
    }

    // ============================================
    // createAttachmentPdf - Success scenarios
    // ============================================

    @Test
    void createAttachmentPdf_shouldReturnSuccess_whenValidRequestProvided() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename("attachment-1.pdf")
                .build();

        when(documentContentService.createAttachmentPdf(any(CreateAttachmentPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(200)
                .body("storagePath", equalTo(STORAGE_PATH))
                .body("filename", equalTo("attachment-1.pdf"));

        verify(documentContentService).createAttachmentPdf(any(CreateAttachmentPdfRequest.class));
    }

    @Test
    void createAttachmentPdf_shouldReturnSuccess_whenInstitutionHasOptionalFields() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.getInstitution().setCity("Rome");
        request.getInstitution().setCountry("Italy");
        request.getInstitution().setCounty("RM");

        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename("attachment-1.pdf")
                .build();

        when(documentContentService.createAttachmentPdf(any(CreateAttachmentPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(200);
    }

    // ============================================
    // createAttachmentPdf - Validation error scenarios
    // ============================================

    @Test
    void createAttachmentPdf_shouldReturn400_whenOnboardingIdIsNull() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setOnboardingId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);

        verify(documentContentService, never()).createAttachmentPdf(any());
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenOnboardingIdIsBlank() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setOnboardingId("");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenAttachmentTemplatePathIsNull() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setAttachmentTemplatePath(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenProductIdIsNull() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setProductId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenProductNameIsNull() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setProductName(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenAttachmentNameIsNull() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setAttachmentName(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenAttachmentNameIsBlank() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setAttachmentName("   ");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenInstitutionIsNull() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setInstitution(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenManagerIsNull() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setManager(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenManagerTaxCodeIsBlank() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.getManager().setTaxCode("");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenManagerIdIsBlank() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();
        request.getManager().setId("");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(400);
    }

    // ============================================
    // createAttachmentPdf - Service error scenarios
    // ============================================

    @Test
    void createAttachmentPdf_shouldReturn500_whenServiceThrowsException() {
        CreateAttachmentPdfRequest request = buildValidAttachmentRequest();

        when(documentContentService.createAttachmentPdf(any(CreateAttachmentPdfRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Attachment generation failed")));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/v1/document-content/attachment")
                .then()
                .statusCode(500);
    }

    // ============================================
    // Helper methods
    // ============================================

    private CreateContractPdfRequest buildValidContractRequest() {
        return CreateContractPdfRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .contractTemplatePath(CONTRACT_TEMPLATE_PATH)
                .productId(PRODUCT_ID)
                .productName(PRODUCT_NAME)
                .pdfFormatFilename(PDF_FORMAT_FILENAME)
                .institution(buildValidInstitutionPdfData())
                .manager(buildValidUserPdfData("manager-1", "MNGTAX001"))
                .build();
    }

    private CreateAttachmentPdfRequest buildValidAttachmentRequest() {
        return CreateAttachmentPdfRequest.builder()
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
                .description("Test Institution S.p.A.")
                .digitalAddress("pec@test.it")
                .address("Via Test 123")
                .zipCode("00100")
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

}
