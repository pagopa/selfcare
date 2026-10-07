package it.pagopa.selfcare.document.filter;

import static io.restassured.RestAssured.given;
import static it.pagopa.selfcare.document.filter.TenantJwtTestSupport.AR_KEYS;
import static it.pagopa.selfcare.document.filter.TenantJwtTestSupport.PNPG_KEYS;
import static it.pagopa.selfcare.document.filter.TenantJwtTestSupport.token;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.document.model.entity.Document;
import it.pagopa.selfcare.document.service.DocumentService;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * End-to-end tenant resolution with enforcement enabled on a deployment serving AR and PNPG: the
 * tenant reaching the service layer must always be the one validated for that request.
 */
@QuarkusTest
@TestProfile(TenantResolutionIntegrationTest.MultiTenantProfile.class)
class TenantResolutionIntegrationTest {

  private static final String DOCUMENT_PATH = "/v1/documents/doc-1";
  private static final String HEADER = TenantResolutionFilter.TENANT_HEADER;

  @InjectMock DocumentService documentService;

  @Inject TenantContext tenantContext;

  @BeforeEach
  void setUp() {
    // Echo the tenant seen by the service layer as the document id, read after a worker-pool hop
    // like the real reactive chains do.
    Mockito.when(documentService.getDocumentById(anyString()))
        .thenAnswer(
            invocation ->
                Uni.createFrom().voidItem()
                    .emitOn(Infrastructure.getDefaultWorkerPool())
                    .map(
                        ignored -> {
                          Document document = new Document();
                          document.setId(tenantContext.requiredTenantId());
                          return document;
                        }));
  }

  @Test
  void shouldRejectMissingTenantHeader() {
    given()
        .auth().oauth2(token("SPID", "AR", AR_KEYS))
        .when().get(DOCUMENT_PATH)
        .then()
        .statusCode(400)
        .contentType("application/problem+json")
        .body("title", equalTo(TenantResolutionFilter.INVALID_TENANT_CONTEXT));
  }

  @Test
  void shouldRejectUnknownTenantHeader() {
    given()
        .auth().oauth2(token("SPID", "AR", AR_KEYS))
        .header(HEADER, "UNKNOWN")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(400);
  }

  @Test
  void shouldRejectDuplicatedTenantHeaders() {
    given()
        .auth().oauth2(token("SPID", "AR", AR_KEYS))
        .header(HEADER, "AR")
        .header(HEADER, "PNPG")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(400);
  }

  @Test
  void shouldRejectHeaderConflictingWithJwtTenantClaim() {
    given()
        .auth().oauth2(token("SPID", "AR", AR_KEYS))
        .header(HEADER, "PNPG")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(401);
  }

  @Test
  void shouldRejectSpidTokenWithoutClaimWhenHeaderIsNotSdkDefault() {
    // selfcare-sdk-security assumes DEFAULT_TENANT (PNPG unless overridden) for claim-less tokens.
    given()
        .auth().oauth2(token("SPID", null, AR_KEYS))
        .header(HEADER, "AR")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(401);
  }

  @Test
  void shouldResolveEachTenantForItsOwnKeyAndHeader() {
    given()
        .auth().oauth2(token("SPID", "AR", AR_KEYS))
        .header(HEADER, "AR")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(200).body("id", equalTo("AR"));

    given()
        .auth().oauth2(token("SPID", "PNPG", PNPG_KEYS))
        .header(HEADER, "PNPG")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(200).body("id", equalTo("PNPG"));
  }

  @Test
  void shouldRejectSpidTokenWhenHeaderCaseDiffersFromClaim() {
    // The SDK reconciles SPID claim and header with an exact, case-sensitive comparison.
    given()
        .auth().oauth2(token("SPID", "AR", AR_KEYS))
        .header(HEADER, "ar")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(401);
  }

  @Test
  void shouldNormalizeHeaderForPagopaTokens() {
    given()
        .auth().oauth2(token("PAGOPA", null, PNPG_KEYS))
        .header(HEADER, " ar ")
        .when().get(DOCUMENT_PATH)
        .then().statusCode(200).body("id", equalTo("AR"));
  }

  @Test
  void shouldIsolateTenantContextAcrossInterleavedRequests() throws Exception {
    String arToken = token("SPID", "AR", AR_KEYS);
    String pnpgToken = token("SPID", "PNPG", PNPG_KEYS);
    ExecutorService executor = Executors.newFixedThreadPool(8);
    try {
      List<Callable<String[]>> calls = new ArrayList<>();
      for (int i = 0; i < 40; i++) {
        String tenant = i % 2 == 0 ? "AR" : "PNPG";
        String jwt = i % 2 == 0 ? arToken : pnpgToken;
        calls.add(
            () -> {
              String seen =
                  given()
                      .auth().oauth2(jwt)
                      .header(HEADER, tenant)
                      .when().get(DOCUMENT_PATH)
                      .then().statusCode(200)
                      .extract().path("id");
              return new String[] {tenant, seen};
            });
      }
      for (Future<String[]> result : executor.invokeAll(calls)) {
        String[] pair = result.get();
        assertEquals(pair[0], pair[1]);
      }
    } finally {
      executor.shutdownNow();
    }
  }

  public static class MultiTenantProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "tenant.registry.json",
          "{\"AR\":{\"mongo\":{\"account\":\"cosmos-ar\",\"database\":\"selcDocument\","
              + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_AR\"},"
              + "\"jwt\":{\"publicKeyEnvVar\":\"JWT_PUBLIC_KEY_AR\"}},"
              + "\"PNPG\":{\"mongo\":{\"account\":\"cosmos-pnpg\",\"database\":\"selcDocument\","
              + "\"connectionStringEnvVar\":\"MONGODB_CONNECTION_STRING_PNPG\"},"
              + "\"jwt\":{\"publicKeyEnvVar\":\"JWT_PUBLIC_KEY_PNPG\"}}}",
          "tenant.supported-tenants", "AR,PNPG",
          "tenant.storage.mandatory-keys", "",
          "tenant.enforcement.enabled", "true",
          "MONGODB_CONNECTION_STRING_PNPG", "mongodb://localhost:27017",
          "JWT_PUBLIC_KEY_AR", TenantJwtTestSupport.pem(AR_KEYS.getPublic()),
          "JWT_PUBLIC_KEY_PNPG", TenantJwtTestSupport.pem(PNPG_KEYS.getPublic()),
          "mp.jwt.verify.publickey", "NONE");
    }
  }
}
