package it.pagopa.selfcare.webhook.controller;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.security.tenant.TenantConstants;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@code TenantValidationFilter} (from selfcare-sdk-security, Step_0 sub-task 5) is
 * correctly discovered and enforced for webhook requests carrying an authenticated JWT: the filter
 * runs at {@code Priorities.AUTHENTICATION + 100} for every request bearing a valid JWT, regardless
 * of whether the target endpoint itself requires {@code @Authenticated} (SELC-2.2 scoping only
 * exempts requests with *no* JWT principal at all). It requires the {@code X-Tenant-Id} header,
 * reconciles it against the JWT {@code tenant_id} claim, and applies the hub-spid-login
 * default-to-{@code PNPG} exception (Step_0 SELC-1..3).
 */
@QuarkusTest
@TestHTTPEndpoint(InfoController.class)
class TenantValidationFilterTest {

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(
      claims = {
        @Claim(key = "iss", value = "PAGOPA"),
        @Claim(key = TenantConstants.TENANT_CLAIM, value = "AR")
      })
  void getVersion_withMatchingHeaderAndClaim_shouldReturnOk() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .when()
        .get("/version")
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
  void getVersion_withMissingHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .when()
        .get("/version")
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
  void getVersion_withHeaderMismatchingClaim_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .when()
        .get("/version")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void getVersion_hubSpidLoginTokenMissingClaim_defaultsToPnpg() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .when()
        .get("/version")
        .then()
        .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void getVersion_hubSpidLoginTokenMissingClaim_headerNotPnpg_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .when()
        .get("/version")
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
  void getVersion_withUnknownTenantHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "UNKNOWN")
        .when()
        .get("/version")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  void getVersion_withoutAnyAuthentication_shouldBypassFilterAndReturnOk() {
    given()
        .accept(ContentType.JSON)
        .when()
        .get("/version")
        .then()
        .statusCode(200);
  }
}
