package it.pagopa.selfcare.user.controller;

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
import it.pagopa.selfcare.security.tenant.TenantConstants;
import it.pagopa.selfcare.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@code TenantValidationFilter} (from selfcare-sdk-security, Step_0 sub-task 5) is
 * correctly discovered and enforced for a real, {@code @Authenticated} user-ms endpoint: it
 * requires the {@code X-Tenant-Id} header, reconciles it against the JWT {@code tenant_id} claim,
 * and applies the hub-spid-login default-to-{@code PNPG} exception (Step_0 SELC-1..3).
 */
@QuarkusTest
@TestHTTPEndpoint(UserController.class)
class TenantValidationFilterTest {

  @InjectMock UserService userService;

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(
      claims = {
        @Claim(key = "iss", value = "PAGOPA"),
        @Claim(key = TenantConstants.TENANT_CLAIM, value = "AR")
      })
  void getUsersEmails_withMatchingHeaderAndClaim_shouldReturnOk() {
    when(userService.getUsersEmails("institutionId", "productId"))
        .thenReturn(Uni.createFrom().item(List.of()));

    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .queryParam("institutionId", "institutionId")
        .queryParam("productId", "productId")
        .when()
        .get("/emails")
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
  void getUsersEmails_withMissingHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .queryParam("institutionId", "institutionId")
        .queryParam("productId", "productId")
        .when()
        .get("/emails")
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
  void getUsersEmails_withHeaderMismatchingClaim_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .queryParam("institutionId", "institutionId")
        .queryParam("productId", "productId")
        .when()
        .get("/emails")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void getUsersEmails_hubSpidLoginTokenMissingClaim_defaultsToPnpg() {
    when(userService.getUsersEmails("institutionId", "productId"))
        .thenReturn(Uni.createFrom().item(List.of()));

    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .queryParam("institutionId", "institutionId")
        .queryParam("productId", "productId")
        .when()
        .get("/emails")
        .then()
        .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void getUsersEmails_hubSpidLoginTokenMissingClaim_headerNotPnpg_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .queryParam("institutionId", "institutionId")
        .queryParam("productId", "productId")
        .when()
        .get("/emails")
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
  void getUsersEmails_withUnknownTenantHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "UNKNOWN")
        .queryParam("institutionId", "institutionId")
        .queryParam("productId", "productId")
        .when()
        .get("/emails")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }
}
