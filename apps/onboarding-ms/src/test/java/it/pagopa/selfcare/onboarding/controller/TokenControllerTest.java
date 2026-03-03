package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.UpdateNotAllowedException;
import it.pagopa.selfcare.onboarding.service.TokenService;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(TokenController.class)
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class TokenControllerTest {

  @InjectMock
  private TokenService tokenService;

  @Test
  @TestSecurity(user = "userJwt")
  void getToken() {

    final String onboardingId = "onboardingId";
    Token token = new Token();
    token.setId(UUID.randomUUID().toString());
    when(tokenService.getToken(onboardingId))
      .thenReturn(Uni.createFrom().item(List.of(token)));

    given()
      .when()
      .contentType(ContentType.JSON)
      .queryParams("onboardingId", onboardingId)
      .get()
      .then()
      .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getContract() {
    final String onboardingId = "onboardingId";
    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok();
    when(tokenService.retrieveContract(onboardingId, false))
      .thenReturn(Uni.createFrom().item(response.build()));

    given()
      .when()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .get("/{onboardingId}/contract", onboardingId)
      .then()
      .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getContractSignedTest() {
    // given
    final String onboardingId = "onboardingId";
    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok();

    when(tokenService.retrieveSignedFile(onboardingId))
      .thenReturn(Uni.createFrom().item(response.build()));

    // when
    given()
      .when()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .get("/{onboardingId}/contract-signed", onboardingId)
      .then()
      .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getTemplateAttachment() {
    final String onboardingId = "onboardingId";
    final String filename = "filename";
    RestResponse.ResponseBuilder<File> response = RestResponse.ResponseBuilder.ok();
    when(tokenService.retrieveTemplateAttachment(onboardingId, filename))
      .thenReturn(Uni.createFrom().item(response.build()));

    given()
      .when()
      .queryParam("name", filename)
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .get("/{onboardingId}/template-attachment", onboardingId)
      .then()
      .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void getTemplateAttachmentBadRequest() {
    final String onboardingId = "onboardingId";
    given()
      .when()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .get("/{onboardingId}/attachment", onboardingId)
      .then()
      .statusCode(400);
  }

  @Test
  @TestSecurity(user = "userJwt")
  void updateContractSignedTest_OK() {
    // given
    final String onboardingId = "onboardingId";
    final String contractSigned = "contractSigned";

    Uni<Long> response = Uni.createFrom().item(1L);
    when(tokenService.updateContractSigned(onboardingId, contractSigned)).thenReturn(response);

    // when
    given()
      .when()
      .queryParam("onboardingId", onboardingId)
      .queryParam("contractSigned", contractSigned)
      .contentType(MediaType.APPLICATION_JSON)
      .put("/contract-signed")
      .then()
      .statusCode(200);

    // then
    Mockito.verify(tokenService, times(1)).updateContractSigned(anyString(), anyString());
  }

  @Test
  @TestSecurity(user = "userJwt")
  void updateContractSignedTest_KO() {
    // given
    final String onboardingId = "onboardingId";
    final String contractSigned = "contractSigned";

    Uni<Long> response = Uni.createFrom()
      .failure(
        new InvalidRequestException(
          String.format("Error %S", onboardingId)));

    when(tokenService.updateContractSigned(onboardingId, contractSigned)).thenReturn(response);

    // when
    given()
      .when()
      .queryParam("onboardingId", onboardingId)
      .queryParam("contractSigned", contractSigned)
      .contentType(MediaType.APPLICATION_JSON)
      .put("/contract-signed")
      .then()
      .statusCode(400);

    // then
    Mockito.verify(tokenService, times(1)).updateContractSigned(anyString(), anyString());
  }

  @Test
  @TestSecurity(user = "userJwt")
  void reportContractSignedTest_OK() {
    // given
    final String onboardingId = "onboardingId";

    Uni<ContractSignedReport> response = Uni.createFrom().item(ContractSignedReport.cades(true));
    when(tokenService.reportContractSigned(onboardingId)).thenReturn(response);

    // when
    given()
      .when()
      .queryParam("onboardingId", onboardingId)
      .contentType(MediaType.APPLICATION_JSON)
      .get("/contract-report")
      .then()
      .statusCode(200);

    // then
    Mockito.verify(tokenService, times(1)).reportContractSigned(anyString());
  }

  @Test
  @TestSecurity(user = "userJwt")
  void uploadAttachment() {
    File testFile = new File("src/test/resources/application.properties");
    String onboardingId = "actual-onboarding-id";

    when(tokenService.uploadAttachment(any(), any(), anyString()))
            .thenReturn(Uni.createFrom().nullItem());

    given()
            .when()
            .pathParam("onboardingId", onboardingId)
            .queryParam("name", "name")
            .contentType(ContentType.MULTIPART)
            .multiPart("file", testFile)
            .post("/{onboardingId}/attachment")
            .then()
            .statusCode(204);

    ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
    verify(tokenService, times(1))
            .uploadAttachment(expectedId.capture(), any(), anyString());
    assertEquals(expectedId.getValue(), onboardingId);
  }

    @Test
    @TestSecurity(user = "userJwt")
    void uploadAttachmentTest_shouldFailed_whenThrowsException() {
        File testFile = new File("src/test/resources/application.properties");
        String onboardingId = "actual-onboarding-id";

        when(tokenService.uploadAttachment(any(), any(), anyString()))
                .thenThrow(new UpdateNotAllowedException("Attachment already uploaded"));

        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .queryParam("name", "name")
                .contentType(ContentType.MULTIPART)
                .multiPart("file", testFile)
                .post("/{onboardingId}/attachment")
                .then()
                .statusCode(409);

        ArgumentCaptor<String> expectedId = ArgumentCaptor.forClass(String.class);
        verify(tokenService, times(1))
                .uploadAttachment(expectedId.capture(), any(), anyString());
    }

  @Test
  @TestSecurity(user = "userJwt")
  void uploadAttachmentError() {
    File testFile1 = new File("src/test/resources/application.properties");
    File testFile2 = new File("src/test/resources/application.properties");
    String onboardingId = "actual-onboarding-id";

    given()
            .when()
            .pathParam("onboardingId", onboardingId)
            .queryParam("name", "name")
            .contentType(ContentType.MULTIPART)
            .multiPart("file", testFile1)
            .multiPart("file", testFile2)
            .post("/{onboardingId}/attachment")
            .then()
            .statusCode(400);

    verify(tokenService, never())
            .uploadAttachment(any(), any(), any());
  }

    @Test
    @TestSecurity(user = "userJwt")
    void headAttachmentTest_OK() {
        // given
        final String onboardingId = "onboardingId";
        final String name = "name";

        when(tokenService.existsAttachment(onboardingId, name)).thenReturn(Uni.createFrom().item(Boolean.TRUE));

        // when
        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .queryParam("name", name)
                .contentType(MediaType.APPLICATION_JSON)
                .head("/{onboardingId}/attachment/status")
                .then()
                .statusCode(204);

        // then
        Mockito.verify(tokenService, times(1)).existsAttachment(anyString(), anyString());
    }


    @Test
    @TestSecurity(user = "userJwt")
    void headAttachmentTest_KO() {
        // given
        final String onboardingId = "onboardingId";
        final String name = "name";

        when(tokenService.existsAttachment(onboardingId, name)).thenReturn(Uni.createFrom().item(Boolean.FALSE));

        // when
        given()
                .when()
                .pathParam("onboardingId", onboardingId)
                .queryParam("name", name)
                .contentType(MediaType.APPLICATION_JSON)
                .head("/{onboardingId}/attachment/status")
                .then()
                .statusCode(404);

        // then
        Mockito.verify(tokenService, times(1)).existsAttachment(anyString(), anyString());
    }
}
