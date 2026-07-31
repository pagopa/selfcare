package it.pagopa.selfcare.product.controller;

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
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.security.tenant.TenantConstants;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@code TenantValidationFilter} (from selfcare-sdk-security, Step_0 sub-task 5) is
 * correctly discovered and enforced for a real, {@code @Authenticated} product-ms endpoint: it
 * requires the {@code X-Tenant-Id} header, reconciles it against the JWT {@code tenant_id} claim,
 * and applies the hub-spid-login default-to-{@code PNPG} exception (Step_0 SELC-1..3).
 */
@QuarkusTest
@TestHTTPEndpoint(ProductController.class)
class TenantValidationFilterTest {

  @InjectMock ProductService productService;

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(
      claims = {
        @Claim(key = "iss", value = "PAGOPA"),
        @Claim(key = TenantConstants.TENANT_CLAIM, value = "AR")
      })
  void ping_withMatchingHeaderAndClaim_shouldReturnOk() {
    when(productService.ping()).thenReturn(Uni.createFrom().item("pong"));

    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .when()
        .get("/ping")
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
  void ping_withMissingHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .when()
        .get("/ping")
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
  void ping_withHeaderMismatchingClaim_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .when()
        .get("/ping")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void ping_hubSpidLoginTokenMissingClaim_defaultsToPnpg() {
    when(productService.ping()).thenReturn(Uni.createFrom().item("pong"));

    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .when()
        .get("/ping")
        .then()
        .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void ping_hubSpidLoginTokenMissingClaim_headerNotPnpg_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .when()
        .get("/ping")
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
  void ping_withUnknownTenantHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "UNKNOWN")
        .when()
        .get("/ping")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }
}
