package it.pagopa.selfcare.document.controller;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.security.tenant.TenantConstants;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@code TenantValidationFilter} (from selfcare-sdk-security, Step_0 sub-task 5) is
 * correctly discovered and enforced for a real, {@code @Authenticated} document-ms endpoint: it
 * requires the {@code X-Tenant-Id} header, reconciles it against the JWT {@code tenant_id} claim,
 * and applies the hub-spid-login default-to-{@code PNPG} exception (Step_0 SELC-1..3).
 */
@QuarkusTest
@TestHTTPEndpoint(DocumentController.class)
class TenantValidationFilterTest {

  @InjectMock DocumentService documentService;

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(
      claims = {
        @Claim(key = "iss", value = "PAGOPA"),
        @Claim(key = TenantConstants.TENANT_CLAIM, value = "AR")
      })
  void getDocumentByOnboardingId_withMatchingHeaderAndClaim_shouldReturnOk() {
    when(documentService.getDocumentByOnboardingId("onboardingId", "AR"))
        .thenReturn(Uni.createFrom().item(new Document()));

    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .when()
        .get("/onboarding/onboardingId")
        .then()
        .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(
      claims = {
        @Claim(key = "iss", value = "PAGOPA"),
        @Claim(key = TenantConstants.TENANT_CLAIM, value = "AR")
      })
  void getDocumentByOnboardingId_withMissingHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .when()
        .get("/onboarding/onboardingId")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(
      claims = {
        @Claim(key = "iss", value = "PAGOPA"),
        @Claim(key = TenantConstants.TENANT_CLAIM, value = "AR")
      })
  void getDocumentByOnboardingId_withHeaderMismatchingClaim_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .when()
        .get("/onboarding/onboardingId")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void getDocumentByOnboardingId_hubSpidLoginTokenMissingClaim_defaultsToPnpg() {
    when(documentService.getDocumentByOnboardingId("onboardingId", "PNPG"))
        .thenReturn(Uni.createFrom().item(new Document()));

    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .when()
        .get("/onboarding/onboardingId")
        .then()
        .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void getDocumentByOnboardingId_hubSpidLoginTokenMissingClaim_headerNotPnpg_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .when()
        .get("/onboarding/onboardingId")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(
      claims = {
        @Claim(key = "iss", value = "PAGOPA"),
        @Claim(key = TenantConstants.TENANT_CLAIM, value = "AR")
      })
  void getDocumentByOnboardingId_withUnknownTenantHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "UNKNOWN")
        .when()
        .get("/onboarding/onboardingId")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }
}
