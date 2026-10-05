package it.pagopa.selfcare.document.filter;

import static io.restassured.RestAssured.given;
import static it.pagopa.selfcare.document.filter.TenantJwtTestSupport.AR_KEYS;
import static it.pagopa.selfcare.document.filter.TenantJwtTestSupport.FOREIGN_KEYS;
import static it.pagopa.selfcare.document.filter.TenantJwtTestSupport.token;
import static org.mockito.ArgumentMatchers.anyString;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.DocumentService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Verifies that JWTs are checked with the per-tenant key referenced by the tenant registry
 * ({@code JWT_PUBLIC_KEY_AR}) instead of the legacy {@code mp.jwt.verify.publickey}.
 */
@QuarkusTest
@TestProfile(TenantJwtAuthenticationTest.ArJwtProfile.class)
class TenantJwtAuthenticationTest {

  private static final String DOCUMENT_PATH = "/v1/documents/doc-1";

  @InjectMock DocumentService documentService;

  @BeforeEach
  void setUp() {
    Mockito.when(documentService.getDocumentById(anyString()))
        .thenReturn(Uni.createFrom().item(new Document()));
  }

  @Test
  void shouldAcceptSpidTokenSignedWithTenantKey() {
    given()
        .auth().oauth2(token("SPID", "AR", AR_KEYS))
        .header(TenantResolutionFilter.TENANT_HEADER, "AR")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(200);
  }

  @Test
  void shouldAcceptPagopaTokenWithoutTenantClaim() {
    given()
        .auth().oauth2(token("PAGOPA", null, AR_KEYS))
        .header(TenantResolutionFilter.TENANT_HEADER, "AR")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(200);
  }

  @Test
  void shouldRejectTokenSignedWithUnknownKey() {
    given()
        .auth().oauth2(token("SPID", "AR", FOREIGN_KEYS))
        .header(TenantResolutionFilter.TENANT_HEADER, "AR")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(401);
  }

  @Test
  void shouldRejectTokenWithUnsupportedIssuer() {
    given()
        .auth().oauth2(token("UNKNOWN", "AR", AR_KEYS))
        .header(TenantResolutionFilter.TENANT_HEADER, "AR")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(401);
  }

  @Test
  void shouldRejectRequestWithoutToken() {
    given()
        .header(TenantResolutionFilter.TENANT_HEADER, "AR")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(401);
  }

  public static class ArJwtProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "JWT_PUBLIC_KEY_AR", TenantJwtTestSupport.pem(AR_KEYS.getPublic()),
          "mp.jwt.verify.publickey", "NONE",
          "mp.jwt.verify.issuer", "SPID",
          "tenant.enforcement.enabled", "true");
    }
  }
}
