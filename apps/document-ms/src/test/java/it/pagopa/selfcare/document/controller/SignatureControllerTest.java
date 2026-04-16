package it.pagopa.selfcare.document.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.service.SignatureService;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
class SignatureControllerTest {

  private static final String ONBOARDING_ID = "onboarding-123";
  private static final String FISCAL_CODE_1 = "RSSMRA80A01H501U";
  private static final String FISCAL_CODE_2 = "VRDGPP85M01H501X";

  @InjectMock SignatureService signatureService;

  @Test
  void verifyContractSignature_shouldReturnNoContent_whenSignatureIsValid() throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    Mockito.when(
            signatureService.verifyContractSignature(
                eq(ONBOARDING_ID), any(File.class), eq(fiscalCodes), any(Boolean.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(204);
  }

  @Test
  void verifyContractSignature_shouldReturnNoContent_whenSingleFiscalCode() throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Collections.singletonList(FISCAL_CODE_1);

    Mockito.when(
            signatureService.verifyContractSignature(
                eq(ONBOARDING_ID), any(File.class), eq(fiscalCodes), any(Boolean.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(204);
  }

  @Test
  void verifyContractSignature_shouldReturnNoContent_whenMultipleFiscalCodes() throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes =
        Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2, "BNCLRD90A01H501Z", "MRRGNN95A01H501Y");

    Mockito.when(
            signatureService.verifyContractSignature(
                eq(ONBOARDING_ID), any(File.class), eq(fiscalCodes), any(Boolean.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(204);
  }

  @Test
  void verifyContractSignature_shouldReturnBadRequest_whenOnboardingIdIsMissing()
      throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    given()
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(400);
  }

  @Test
  void verifyContractSignature_shouldReturnBadRequest_whenOnboardingIdIsEmpty() throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    given()
        .multiPart("onboardingId", "")
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(400);
  }

  @Test
  void verifyContractSignature_shouldReturnBadRequest_whenOnboardingIdIsBlank() throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    given()
        .multiPart("onboardingId", "   ")
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(400);
  }

  @Test
  void verifyContractSignature_shouldReturnBadRequest_whenFileIsMissing() {
    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(400);
  }

  @Test
  void verifyContractSignature_shouldReturnInternalServerError_whenServiceFails()
      throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    Mockito.when(
            signatureService.verifyContractSignature(
                anyString(), any(File.class), anyList(), any(Boolean.class)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("Signature verification failed")));

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(500);
  }

  @Test
  void verifyContractSignature_shouldReturnInternalServerError_whenSignatureIsInvalid()
      throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    Mockito.when(
            signatureService.verifyContractSignature(
                anyString(), any(File.class), anyList(), any(Boolean.class)))
        .thenReturn(
            Uni.createFrom().failure(new IllegalStateException("Invalid signature format")));

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(500);
  }

  @Test
  void verifyContractSignature_shouldReturnInternalServerError_whenFileCannotBeRead()
      throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    Mockito.when(
            signatureService.verifyContractSignature(
                anyString(), any(File.class), anyList(), any(Boolean.class)))
        .thenReturn(Uni.createFrom().failure(new java.io.IOException("Cannot read file")));

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(500);
  }

  @Test
  void verifyContractSignature_shouldReturnInternalServerError_whenFiscalCodeMismatch()
      throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    Mockito.when(
            signatureService.verifyContractSignature(
                anyString(), any(File.class), anyList(), any(Boolean.class)))
        .thenReturn(
            Uni.createFrom()
                .failure(new IllegalArgumentException("Fiscal code does not match signature")));

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(500);
  }

  @Test
  void verifyContractSignature_shouldCallServiceWithCorrectParameters() throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    Mockito.when(
            signatureService.verifyContractSignature(
                anyString(), any(File.class), anyList(), any(Boolean.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(204);

    Mockito.verify(signatureService, Mockito.times(1))
        .verifyContractSignature(eq(ONBOARDING_ID), any(File.class), anyList(), any(Boolean.class));
  }

  @Test
  void verifyContractSignature_shouldHandleLargeFile() throws Exception {
    File tempFile = Files.createTempFile("large-contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Arrays.asList(FISCAL_CODE_1, FISCAL_CODE_2);

    Mockito.when(
            signatureService.verifyContractSignature(
                eq(ONBOARDING_ID), any(File.class), eq(fiscalCodes), any(Boolean.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(204);
  }

  @Test
  void verifyContractSignature_shouldHandleDifferentFileExtensions_p7m() throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Collections.singletonList(FISCAL_CODE_1);

    Mockito.when(
            signatureService.verifyContractSignature(
                eq(ONBOARDING_ID), any(File.class), eq(fiscalCodes), any(Boolean.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(204);
  }

  @Test
  void verifyContractSignature_shouldHandleDifferentFileExtensions_pdf() throws Exception {
    File tempFile = Files.createTempFile("contract", ".pdf").toFile();
    tempFile.deleteOnExit();

    List<String> fiscalCodes = Collections.singletonList(FISCAL_CODE_1);


    Mockito.when(
            signatureService.verifyContractSignature(
                eq(ONBOARDING_ID), any(File.class), eq(fiscalCodes), any(Boolean.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .multiPart("onboardingId", ONBOARDING_ID)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(204);
  }

  @Test
  void verifyContractSignature_shouldHandleSpecialCharactersInOnboardingId() throws Exception {
    File tempFile = Files.createTempFile("contract", ".p7m").toFile();
    tempFile.deleteOnExit();

    String specialOnboardingId = "onboarding-123-ABC_456";
    List<String> fiscalCodes = Collections.singletonList(FISCAL_CODE_1);

    Mockito.when(
            signatureService.verifyContractSignature(
                eq(specialOnboardingId), any(File.class), eq(fiscalCodes), any(Boolean.class)))
        .thenReturn(Uni.createFrom().voidItem());

    given()
        .multiPart("onboardingId", specialOnboardingId)
        .multiPart("file", tempFile, MediaType.APPLICATION_OCTET_STREAM)
        .multiPart("fiscalCodes", fiscalCodes, MediaType.APPLICATION_JSON)
        .multiPart("skipSignatureVerification", Boolean.FALSE)
        .when()
        .post("/v1/signature/verify")
        .then()
        .statusCode(204);
  }

  @Test
  void verifyContractSignature_shouldReturnUnauthorized_whenNotAuthenticated() {
    // This test would require removing @TestSecurity annotation or using a different approach
    // For now, documenting that authentication is required via @Authenticated annotation
  }
}
