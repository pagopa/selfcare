package it.pagopa.selfcare.product.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.exception.ConflictException;
import it.pagopa.selfcare.product.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.model.ContractTemplateFile;
import it.pagopa.selfcare.product.model.dto.request.ContractTemplateUploadRequest;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponse;
import it.pagopa.selfcare.product.model.dto.response.ContractTemplateResponseList;
import it.pagopa.selfcare.product.model.enums.ContractTemplateFileType;
import it.pagopa.selfcare.product.service.ContractTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;

@Slf4j
@QuarkusTest
@TestHTTPEndpoint(ContractTemplateController.class)
public class ContractTemplateControllerTest {

    @InjectMock
    private ContractTemplateService contractTemplateService;

    @Test
    @TestSecurity(user = "userJwt")
    void upload_shouldReturnOk() throws URISyntaxException {
        final ContractTemplateResponse response = ContractTemplateResponse.builder()
                .contractTemplateId("123")
                .contractTemplatePath("contract-templates/prod-test/123.html")
                .contractTemplateVersion("1.0.0")
                .productId("prod-test")
                .name("Test name")
                .description("Test description")
                .createdAt(java.time.Instant.now())
                .createdBy("testuser")
                .build();

        Mockito.when(contractTemplateService.upload(Mockito.any(ContractTemplateUploadRequest.class)))
                .thenReturn(Uni.createFrom().item(response));

        given()
            .queryParam("productId", "prod-test")
            .queryParam("createdBy", "testuser")
            .queryParam("name", "Test template")
            .queryParam("version", "1.0.0")
            .queryParam("description", "Test description")
            .multiPart("file", new File(getClass().getResource("/request/contract-template-fragment.html").toURI()), "text/html")
        .when()
            .post()
        .then()
            .statusCode(201);

        Mockito.verify(contractTemplateService, Mockito.times(1))
                .upload(any(ContractTemplateUploadRequest.class));
    }

    @Test
    @TestSecurity(user = "userJwt")
    void upload_shouldReturnConflict() throws URISyntaxException {
        Mockito.when(contractTemplateService.upload(Mockito.any(ContractTemplateUploadRequest.class)))
                .thenReturn(Uni.createFrom().failure(new ConflictException("Conflict", "409")));

        given()
            .queryParam("productId", "prod-test")
            .queryParam("createdBy", "testuser")
            .queryParam("name", "Test template")
            .queryParam("version", "1.0.0")
            .queryParam("description", "Test description")
            .multiPart("file", new File(getClass().getResource("/request/contract-template-fragment.html").toURI()), "text/html")
        .when()
            .post()
        .then()
            .statusCode(409);

        Mockito.verify(contractTemplateService, Mockito.times(1))
                .upload(any(ContractTemplateUploadRequest.class));
    }

    @Test
    @TestSecurity(user = "userJwt")
    void upload_shouldReturnBadRequest() throws URISyntaxException {
        given()
            .contentType("multipart/form-data")
        .when()
            .post()
        .then()
            .statusCode(400);

        given()
            .contentType("multipart/form-data")
            .queryParam("productId", "prod-test")
        .when()
            .post()
        .then()
            .statusCode(400);

        given()
            .contentType("multipart/form-data")
            .queryParam("productId", "prod-test")
            .queryParam("name", "Test template")
        .when()
            .post()
        .then()
            .statusCode(400);

        given()
            .contentType("multipart/form-data")
            .queryParam("productId", "prod-test")
            .queryParam("name", "Test template")
            .queryParam("version", "1.0.0")
        .when()
            .post()
        .then()
            .statusCode(400);

        given()
            .contentType("multipart/form-data")
            .queryParam("productId", "prod-test")
            .queryParam("name", "Test template")
            .queryParam("version", "1.0.0")
            .multiPart("file", new File(getClass().getResource("/request/contract-template-invalid.html").toURI()), "text/html")
        .when()
            .post()
        .then()
            .statusCode(400);

        given()
            .contentType("multipart/form-data")
            .queryParam("productId", "prod-test")
            .queryParam("name", "Test template")
            .queryParam("version", "1.0.0")
            .multiPart("file", new File(getClass().getResource("/request/contract-template.pdf").toURI()), "application/pdf")
        .when()
            .post()
        .then()
            .statusCode(400);

        Mockito.verify(contractTemplateService, Mockito.times(0))
                .upload(any(ContractTemplateUploadRequest.class));
    }

    @Test
    @TestSecurity(user = "userJwt")
    void download_shouldReturnHTML() throws URISyntaxException, IOException {
        final ContractTemplateFile contractTemplateFile = ContractTemplateFile.builder()
                .data(Files.readAllBytes(Paths.get(getClass().getResource("/request/contract-template-fragment.html").toURI())))
                .type(ContractTemplateFileType.HTML)
                .build();

        Mockito.when(contractTemplateService.download("prod-test", "123", ContractTemplateFileType.HTML))
                .thenReturn(Uni.createFrom().item(contractTemplateFile));

        given()
            .queryParam("productId", "prod-test")
            .pathParam("contractTemplateId", "123")
        .when()
            .get("/{contractTemplateId}")
        .then()
            .statusCode(200)
            .contentType("text/html");

        Mockito.verify(contractTemplateService, Mockito.times(1))
                .download("prod-test", "123", ContractTemplateFileType.HTML);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void download_shouldReturnPDF() throws URISyntaxException, IOException {
        final ContractTemplateFile contractTemplateFile = ContractTemplateFile.builder()
                .data(Files.readAllBytes(Paths.get(getClass().getResource("/request/contract-template.pdf").toURI())))
                .type(ContractTemplateFileType.PDF)
                .build();

        Mockito.when(contractTemplateService.download("prod-test", "123", ContractTemplateFileType.PDF))
                .thenReturn(Uni.createFrom().item(contractTemplateFile));

        given()
            .queryParam("productId", "prod-test")
            .queryParam("fileType", "pdf")
            .pathParam("contractTemplateId", "123")
        .when()
            .get("/{contractTemplateId}")
        .then()
            .statusCode(200)
            .contentType("application/pdf");

        Mockito.verify(contractTemplateService, Mockito.times(1))
                .download("prod-test", "123", ContractTemplateFileType.PDF);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void download_shouldReturnNotFound() {
        Mockito.when(contractTemplateService.download("prod-test", "123", ContractTemplateFileType.HTML))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Not found", "404")));

        given()
            .queryParam("productId", "prod-test")
            .pathParam("contractTemplateId", "123")
        .when()
            .get("/{contractTemplateId}")
        .then()
            .statusCode(404);

        Mockito.verify(contractTemplateService, Mockito.times(1))
                .download("prod-test", "123", ContractTemplateFileType.HTML);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void download_shouldReturnBadRequest() {
        given()
            .queryParam("productId", "prod-test")
            .queryParam("fileType", "json")
            .pathParam("contractTemplateId", "123")
        .when()
            .get("/{contractTemplateId}")
        .then()
            .statusCode(400);

        Mockito.verify(contractTemplateService, Mockito.times(0))
                .download(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @TestSecurity(user = "userJwt")
    void list_shouldReturnOk() {
        Mockito.when(contractTemplateService.list("prod-test", "test", "1.0.0"))
                .thenReturn(Uni.createFrom().item(new ContractTemplateResponseList(List.of(
                        ContractTemplateResponse.builder()
                                .contractTemplateId("123")
                                .contractTemplatePath("contract-templates/prod-test/123.html")
                                .contractTemplateVersion("1.0.0")
                                .productId("prod-test")
                                .name("Test name")
                                .description("Test description")
                                .createdAt(java.time.Instant.now())
                                .createdBy("testuser")
                                .build(),
                        ContractTemplateResponse.builder()
                                .contractTemplateId("123")
                                .contractTemplatePath("contract-templates/prod-test/123.html")
                                .contractTemplateVersion("1.0.0")
                                .productId("prod-test")
                                .name("Test name")
                                .description("Test description")
                                .createdAt(java.time.Instant.now())
                                .createdBy("testuser")
                                .build()
                ))));

        given()
            .queryParam("productId", "prod-test")
            .queryParam("version", "1.0.0")
            .queryParam("name", "test")
        .when()
            .get()
        .then()
            .statusCode(200);

        Mockito.verify(contractTemplateService, Mockito.times(1))
                .list("prod-test", "test", "1.0.0");
    }

}
