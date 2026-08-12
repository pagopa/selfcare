package it.pagopa.selfcare.onboarding.controller;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.security.tenant.TenantConstants;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@code TenantValidationFilter} (from selfcare-sdk-security, Step_0 sub-task 5) is
 * correctly discovered and enforced for a real, {@code @Authenticated} onboarding-ms endpoint: it
 * requires the {@code X-Tenant-Id} header, reconciles it against the JWT {@code tenant_id} claim,
 * and applies the hub-spid-login default-to-{@code PNPG} exception (Step_0 SELC-1..3).
 */
@QuarkusTest
@TestHTTPEndpoint(OnboardingController.class)
class TenantValidationFilterTest {

  @InjectMock OnboardingService onboardingService;

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(
      claims = {
        @Claim(key = "iss", value = "PAGOPA"),
        @Claim(key = TenantConstants.TENANT_CLAIM, value = "AR")
      })
  void getOnboardingWithFilter_withMatchingHeaderAndClaim_shouldReturnOk() {
    when(onboardingService.onboardingGet(any(OnboardingGetFilters.class)))
        .thenReturn(Uni.createFrom().item(new OnboardingGetResponse()));

    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .when()
        .get()
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
  void getOnboardingWithFilter_withMissingHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .when()
        .get()
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
  void getOnboardingWithFilter_withHeaderMismatchingClaim_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .when()
        .get()
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void getOnboardingWithFilter_hubSpidLoginTokenMissingClaim_defaultsToPnpg() {
    when(onboardingService.onboardingGet(any(OnboardingGetFilters.class)))
        .thenReturn(Uni.createFrom().item(new OnboardingGetResponse()));

    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "PNPG")
        .when()
        .get()
        .then()
        .statusCode(200);
  }

  @Test
  @TestSecurity(user = "userJwt")
  @JwtSecurity(claims = {@Claim(key = "iss", value = "SPID")})
  void getOnboardingWithFilter_hubSpidLoginTokenMissingClaim_headerNotPnpg_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "AR")
        .when()
        .get()
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
  void getOnboardingWithFilter_withUnknownTenantHeader_shouldReturnBadRequest() {
    given()
        .accept(ContentType.JSON)
        .header(TenantConstants.TENANT_HEADER, "UNKNOWN")
        .when()
        .get()
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }
}
