package it.pagopa.selfcare.document.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.exception.ConflictException;
import it.pagopa.selfcare.document.model.FormItem;
import it.pagopa.selfcare.document.model.dto.request.DocumentBuilderRequest;
import it.pagopa.selfcare.document.service.DocumentContentService;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class DocumentContentControllerTest {

    private static final String DOCUMENT_ID = "doc-456";
    private static final String ONBOARDING_ID = "onboarding-123";
    private static final String ATTACHMENT_NAME = "attachment.pdf";
    private static final String TEMPLATE_PATH = "templates/contract.ftl";
    private static final String INSTITUTION_DESCRIPTION = "Test Institution";
    private static final String PRODUCT_ID = "Product-123";

    @InjectMock DocumentContentService documentContentService;

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
                .thenReturn(Uni.createFrom().failure(new ConflictException("Attachment already exists")));

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


}
