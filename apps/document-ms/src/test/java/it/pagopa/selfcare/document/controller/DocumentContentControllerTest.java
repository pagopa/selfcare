package it.pagopa.selfcare.document.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.exception.InvalidRequestException;
import it.pagopa.selfcare.document.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.*;
import it.pagopa.selfcare.document.model.dto.response.CreatePdfResponse;
import it.pagopa.selfcare.document.service.DocumentContentService;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
class DocumentContentControllerTest {

    private static final String DOCUMENT_ID = "doc-456";
    private static final String ONBOARDING_ID = "onboarding-123";
    private static final String ATTACHMENT_NAME = "attachment.pdf";
    private static final String TEMPLATE_PATH = "templates/contract.ftl";
    private static final String INSTITUTION_DESCRIPTION = "Test Institution";
    private static final String PRODUCT_ID = "Product-123";
    private static final String PRODUCT_NAME = "PagoPA";
    private static final String CONTRACT_TEMPLATE_PATH = "templates/contract.ftl";
    private static final String ATTACHMENT_TEMPLATE_PATH = "templates/attachment.ftl";
    private static final String STORAGE_PATH = "contracts/signed/contract-123.pdf";
    private static final String FILENAME = "contract-123.pdf";
    private static final String BASE_PATH = "/v1/document-content/";

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
                .get(BASE_PATH + DOCUMENT_ID + "/contract-signed")
                .then()
                .statusCode(200);
    }

    @Test
    void getContractSigned_shouldReturnInternalServerError_whenServiceFails() {
        Mockito.when(documentContentService.retrieveSignedFile(DOCUMENT_ID))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

        given()
                .when()
                .get(BASE_PATH + DOCUMENT_ID + "/contract-signed")
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
                .get(BASE_PATH + ONBOARDING_ID + "/template-attachment")
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
                .get(BASE_PATH + ONBOARDING_ID + "/template-attachment")
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
                .get(BASE_PATH + ONBOARDING_ID + "/attachment")
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
                .get(BASE_PATH + ONBOARDING_ID + "/attachment")
                .then()
                .statusCode(500);
    }

    @Test
    void getAttachment_shouldReturnBadRequest_whenAttachmentNameMissing() {
        given()
                .when()
                .get(BASE_PATH + ONBOARDING_ID + "/attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void uploadAttachment_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        File tempFile = Files.createTempFile("upload", ".pdf").toFile();
        tempFile.deleteOnExit();

        DocumentBuilderRequest invalidRequest = DocumentBuilderRequest.builder()
                .productId("prod-123")
                .documentType(DocumentType.ATTACHMENT)
                .build();

        given()
                .multiPart("file", tempFile, "application/pdf")
                .multiPart("request", invalidRequest, "application/json")
                .when()
                .post(BASE_PATH + "upload-attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void getTemplateAttachment_shouldReturnBadRequest_whenMissingTemplatePath() {
        given()
                .when()
                .queryParam("name", ATTACHMENT_NAME)
                .get(BASE_PATH + ONBOARDING_ID + "/template-attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void getTemplateAttachment_shouldReturnBadRequest_whenMissingName() {
        given()
                .when()
                .queryParam("templatePath", TEMPLATE_PATH)
                .get(BASE_PATH + ONBOARDING_ID + "/template-attachment")
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
                .documentType(DocumentType.ATTACHMENT)
                .attachmentName(ATTACHMENT_NAME)
                .build();

        Mockito.when(documentContentService.uploadAttachment(any(DocumentBuilderRequest.class), any(FormItem.class)))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .multiPart("file", tempFile, "application/pdf")
                .multiPart("request", request, "application/json")
                .when()
                .post(BASE_PATH + "upload-attachment")
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
                .documentType(DocumentType.ATTACHMENT)
                .attachmentName(ATTACHMENT_NAME)
                .build();

        Mockito.when(documentContentService.uploadAttachment(any(DocumentBuilderRequest.class), any(FormItem.class)))
                .thenReturn(Uni.createFrom().failure(new UpdateNotAllowedException("Attachment already exists")));

        given()
                .multiPart("file", tempFile, "application/pdf")
                .multiPart("request", request, "application/json")
                .when()
                .post(BASE_PATH + "upload-attachment")
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
                .documentType(DocumentType.ATTACHMENT)
                .attachmentName(ATTACHMENT_NAME)
                .build();

        Mockito.when(documentContentService.uploadAttachment(any(DocumentBuilderRequest.class), any(FormItem.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

        given()
                .multiPart("file", tempFile, "application/pdf")
                .multiPart("request", request, "application/json")
                .when()
                .post(BASE_PATH + "upload-attachment")
                .then()
                .statusCode(500);
    }

    @Test
    void uploadAggregatesCsv_shouldReturnNoContent_whenUploadSuccessful() throws IOException {
        File csvFile = Files.createTempFile("aggregates", ".csv").toFile();
        csvFile.deleteOnExit();

        Mockito.when(documentContentService.uploadAggregatesCsv(any(UploadAggregateCsvRequest.class)))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .multiPart("onboardingId", ONBOARDING_ID)
                .multiPart("productId", PRODUCT_ID)
                .multiPart("file", csvFile, "text/csv")
                .when()
                .post(BASE_PATH + "aggregates-csv")
                .then()
                .statusCode(204);

        Mockito.verify(documentContentService).uploadAggregatesCsv(any(UploadAggregateCsvRequest.class));
    }

    @Test
    void uploadAggregatesCsv_shouldReturnInternalServerError_whenServiceFails() throws IOException {
        File csvFile = Files.createTempFile("aggregates", ".csv").toFile();
        csvFile.deleteOnExit();

        Mockito.when(documentContentService.uploadAggregatesCsv(any(UploadAggregateCsvRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

        given()
                .multiPart("onboardingId", ONBOARDING_ID)
                .multiPart("productId", PRODUCT_ID)
                .multiPart("file", csvFile, "text/csv")
                .when()
                .post(BASE_PATH + "aggregates-csv")
                .then()
                .statusCode(500);
    }

    @Test
    void getAggregatesCsv() {
        final String onboardingId = "onboardingId";
        final String productId = "productId";
        RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok();
        when(documentContentService.retrieveAggregatesCsv(onboardingId,productId))
                .thenReturn(Uni.createFrom().item(response.build()));

        given()
                .when()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .get(BASE_PATH + "aggregates-csv/{onboardingId}/products/{productId}", onboardingId, productId)
                .then()
                .statusCode(200);
    }

    @Test
    void saveVisuraForMerchant_shouldReturnNoContent_whenUploadSuccessful() throws IOException {
        File tempFile = Files.createTempFile("visura", ".pdf").toFile();
        tempFile.deleteOnExit();

        UploadVisuraRequest request = UploadVisuraRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .filename("VISURA_test.xml")
                .build();

        Mockito.when(documentContentService.saveVisuraForMerchant(any(UploadVisuraRequest.class)))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .multiPart("onboardingId", request.getOnboardingId())
                .multiPart("filename", request.getFilename())
                .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
                .when()
                .post(BASE_PATH + "visura")
                .then()
                .statusCode(204);
    }

    @Test
    void saveVisuraForMerchant_shouldReturnInternalServerError_whenServiceFails() throws IOException {
        File tempFile = Files.createTempFile("visura", ".pdf").toFile();
        tempFile.deleteOnExit();

        UploadVisuraRequest request = UploadVisuraRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .filename("VISURA_test.xml")
                .build();

        Mockito.when(documentContentService.saveVisuraForMerchant(any(UploadVisuraRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Generic error")));

        given()
                .multiPart("onboardingId", request.getOnboardingId())
                .multiPart("filename", request.getFilename())
                .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
                .when()
                .post(BASE_PATH + "visura")
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
                .get(BASE_PATH + ONBOARDING_ID + "/contract")
                .then()
                .statusCode(200);
    }

    @Test
    void getContract_shouldReturnInternalServerError_whenServiceFails() {
        Mockito.when(documentContentService.retrieveContract(ONBOARDING_ID, Boolean.FALSE))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Storage error")));

        given()
                .when()
                .get(BASE_PATH + ONBOARDING_ID + "/contract")
                .then()
                .statusCode(500);
    }

    // ============================================
    // createContractPdf - Success scenarios
    // ============================================

    @Test
    void createContractPdf_shouldReturnSuccess_whenValidRequestProvided() {
        ContractPdfRequest request = buildValidContractRequest();
        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename(FILENAME)
                .build();

        when(documentContentService.createContractPdf(any(ContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(200)
                .body("storagePath", equalTo(STORAGE_PATH))
                .body("filename", equalTo(FILENAME));

        verify(documentContentService).createContractPdf(any(ContractPdfRequest.class));
    }

    @Test
    void createContractPdf_shouldReturnSuccess_whenRequestHasDelegates() {
        ContractPdfRequest request = buildValidContractRequest();
        UserPdfData delegate1 = buildValidUserPdfData("delegate-1", "DLGTAX001");
        UserPdfData delegate2 = buildValidUserPdfData("delegate-2", "DLGTAX002");
        request.setDelegates(List.of(delegate1, delegate2));

        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename(FILENAME)
                .build();

        when(documentContentService.createContractPdf(any(ContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(200)
                .body("storagePath", notNullValue());
    }

    @Test
    void createContractPdf_shouldReturnSuccess_whenRequestHasOptionalFields() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setPricingPlan("PREMIUM");
        request.setIsAggregator(true);
        request.setAggregatesCsvBaseUrl("https://example.com/aggregates");

        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename(FILENAME)
                .build();

        when(documentContentService.createContractPdf(any(ContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(200);
    }

    @Test
    void createContractPdf_shouldReturnSuccess_whenRequestHasEmptyDelegatesList() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setDelegates(Collections.emptyList());

        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename(FILENAME)
                .build();

        when(documentContentService.createContractPdf(any(ContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(200);
    }

    // ============================================
    // createContractPdf - Validation error scenarios
    // ============================================

    @Test
    void createContractPdf_shouldReturn400_whenOnboardingIdIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setOnboardingId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);

        verify(documentContentService, never()).createContractPdf(any());
    }

    @Test
    void createContractPdf_shouldReturn400_whenOnboardingIdIsBlank() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setOnboardingId("   ");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);

        verify(documentContentService, never()).createContractPdf(any());
    }

    @Test
    void createContractPdf_shouldReturn400_whenContractTemplatePathIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setContractTemplatePath(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenProductIdIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenProductNameIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setProductName(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenPdfFormatFilenameIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setOnboardingId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenInstitutionIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setInstitution(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenManagerIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.setManager(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenManagerTaxCodeIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.getManager().setTaxCode(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    @Test
    void createContractPdf_shouldReturn400_whenManagerIdIsNull() {
        ContractPdfRequest request = buildValidContractRequest();
        request.getManager().setId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    // ============================================
    // createContractPdf - Service error scenarios
    // ============================================

    @Test
    void createContractPdf_shouldReturn500_whenServiceThrowsException() {
        ContractPdfRequest request = buildValidContractRequest();

        when(documentContentService.createContractPdf(any(ContractPdfRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("PDF generation failed")));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "contract")
                .then()
                .statusCode(500);
    }

    // ============================================
    // createAttachmentPdf - Success scenarios
    // ============================================

    @Test
    void createAttachmentPdf_shouldReturnSuccess_whenValidRequestProvided() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename("attachment-1.pdf")
                .build();

        when(documentContentService.createAttachmentPdf(any(AttachmentPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(200)
                .body("storagePath", equalTo(STORAGE_PATH))
                .body("filename", equalTo("attachment-1.pdf"));

        verify(documentContentService).createAttachmentPdf(any(AttachmentPdfRequest.class));
    }

    @Test
    void createAttachmentPdf_shouldReturnSuccess_whenInstitutionHasOptionalFields() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.getInstitution().setCity("Rome");
        request.getInstitution().setCountry("Italy");
        request.getInstitution().setCounty("RM");

        CreatePdfResponse response = CreatePdfResponse.builder()
                .storagePath(STORAGE_PATH)
                .filename("attachment-1.pdf")
                .build();

        when(documentContentService.createAttachmentPdf(any(AttachmentPdfRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(200);
    }

    // ============================================
    // createAttachmentPdf - Validation error scenarios
    // ============================================

    @Test
    void createAttachmentPdf_shouldReturn400_whenOnboardingIdIsNull() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setOnboardingId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);

        verify(documentContentService, never()).createAttachmentPdf(any());
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenOnboardingIdIsBlank() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setOnboardingId("");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenAttachmentTemplatePathIsNull() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setAttachmentTemplatePath(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenProductIdIsNull() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setProductId(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenProductNameIsNull() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setProductName(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenAttachmentNameIsNull() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setAttachmentName(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenAttachmentNameIsBlank() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setAttachmentName("   ");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenInstitutionIsNull() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setInstitution(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenManagerIsNull() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.setManager(null);

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenManagerTaxCodeIsBlank() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.getManager().setTaxCode("");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    @Test
    void createAttachmentPdf_shouldReturn400_whenManagerIdIsBlank() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();
        request.getManager().setId("");

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(400);
    }

    // ============================================
    // createAttachmentPdf - Service error scenarios
    // ============================================

    @Test
    void createAttachmentPdf_shouldReturn500_whenServiceThrowsException() {
        AttachmentPdfRequest request = buildValidAttachmentRequest();

        when(documentContentService.createAttachmentPdf(any(AttachmentPdfRequest.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Attachment generation failed")));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post(BASE_PATH + "attachment")
                .then()
                .statusCode(500);
    }

    @Test
    void deleteContract_shouldReturnOk_whenDeletionSuccessful() {
        String onboardingId = "test-onboarding-123";
        String expectedMessage = "Contract deleted successfully";

        Mockito.when(documentContentService.deleteContract(onboardingId))
                .thenReturn(Uni.createFrom().item(expectedMessage));

        given()
                .queryParam("document", onboardingId) // <-- CAMBIATO DA "onboardingId" A "document"
                .when()
                .delete(BASE_PATH + "contract")
                .then()
                .statusCode(200)
                .body(equalTo(expectedMessage));
    }

    @Test
    void deleteContract_shouldReturnBadRequest_whenOnboardingIdMissing() {
        // Act & Assert
        // Non passiamo il parametro "document" per far scattare il @NotBlank e ottenere un 400
        given()
                .when()
                .delete(BASE_PATH + "contract")
                .then()
                .statusCode(400);
    }

    @Test
    void deleteContract_shouldReturnInternalServerError_whenServiceFails() {
        // Arrange
        String onboardingId = "test-onboarding-123";

        // Simuliamo il fallimento del service (es. eccezione di I/O o DB)
        Mockito.when(documentContentService.deleteContract(onboardingId))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Error deleting contract files from Azure")));

        // Act & Assert
        given()
                .queryParam("document", onboardingId) // <-- Usiamo "document" per allinearci al controller!
                .when()
                .delete(BASE_PATH + "contract")
                .then()
                .statusCode(500);
    }

    @Test
    void uploadSignedContract_Success() {
        // GIVEN: Il service risponde con successo
        String onboardingId = "onb-123";

        Mockito.when(documentContentService.uploadSignedContract(
                eq(onboardingId),
                eq(new DocumentBuilderRequest()),
                eq(false),
                any(InputStream.class),
                anyString()
        )).thenReturn(Uni.createFrom().item("success-path")); // O .voidItem() se il service restituisce Uni<Void>

        File dummyFile = new File("src/test/resources/pdf/dummy.pdf");

        // WHEN & THEN: Facciamo la chiamata multipart e ci aspettiamo 204 No Content
        given()
                .pathParam("onboardingId", onboardingId)
                .multiPart("productId", "prod-1")
                .multiPart("productTitle", "Product Title")
                .multiPart("institutionType", "INSTITUTION")
                .multiPart("contractPath", "/path/to/template")
                .multiPart("fiscalCodes", "FC1") // Puoi aggiungere più multiPart con lo stesso nome per le liste
                .multiPart("fiscalCodes", "FC2")
                .multiPart("skipSignatureVerification", "false")
                .multiPart("file", dummyFile)
                .multiPart("fileName", "filename")
                .when()
                .post(BASE_PATH + "{onboardingId}/upload-signed-contract")
                .then()
                .statusCode(204); // Assert: Il .replaceWith(() -> Response.noContent().build()) ha funzionato
    }

    @Test
    void uploadSignedContract_SignatureVerificationFails() {
        // GIVEN: Il service lancia l'eccezione corretta di dominio
        String onboardingId = "onb-123";

        // SOSTITUISCI QUI: Usa InvalidRequestException invece di IllegalArgumentException
        Mockito.when(documentContentService.uploadSignedContract(
                any(), any(), anyBoolean(), any(), anyString()
        )).thenReturn(Uni.createFrom().failure(new InvalidRequestException("Invalid signature", "CODE-400")));

        File dummyFile = new File("src/test/resources/pdf/dummy.pdf");

        // WHEN & THEN: Ora ci aspettiamo un 400 Bad Request
        given()
                .pathParam("onboardingId", onboardingId)
                .multiPart("productId", "prod-1")
                .multiPart("productTitle", "Product Title")
                .multiPart("institutionType", "INSTITUTION") // Modificato nome param per allinearsi al controller
                .multiPart("contractPath", "/path/to/template")
                .multiPart("file", dummyFile)
                .multiPart("fileName", "filename")
                .when()
                .post(BASE_PATH + "{onboardingId}/upload-signed-contract")
                .then()
                .statusCode(400);
    }

    @Test
    void uploadSignedContract_InternalServerError_AzureDown() {
        // GIVEN: Il service lancia un'eccezione di sistema (es. RuntimeException per Azure)
        String onboardingId = "onb-123";

        Mockito.when(documentContentService.uploadSignedContract(
                eq(onboardingId), any(), anyBoolean(), any(), anyString()
        )).thenReturn(Uni.createFrom().failure(new RuntimeException("Azure is down")));

        File dummyFile = new File("src/test/resources/pdf/dummy.pdf");

        // WHEN & THEN: Ci aspettiamo un 500 Internal Server Error
        given()
                .pathParam("onboardingId", onboardingId)
                .multiPart("productId", "prod-1")
                .multiPart("institutionType", "INSTITUTION")
                .multiPart("file", dummyFile)
                .multiPart("fileName",  "filename")
                .when()
                .post(BASE_PATH + "{onboardingId}/upload-signed-contract")
                .then()
                .statusCode(500);
    }

    // ============================================
    // Helper methods
    // ============================================

    private ContractPdfRequest buildValidContractRequest() {
        return ContractPdfRequest.builder()
                .onboardingId(ONBOARDING_ID)
                .contractTemplatePath(CONTRACT_TEMPLATE_PATH)
                .productId(PRODUCT_ID)
                .productName(PRODUCT_NAME)
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
