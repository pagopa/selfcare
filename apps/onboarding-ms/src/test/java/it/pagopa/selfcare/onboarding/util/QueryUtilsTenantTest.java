package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.conf.CurrentTenantProvider;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryUtilsTenantTest {

  private static final String TENANT_AR = "AR";
  private static final String TENANT_PNPG = "PNPG";

  private QueryUtils queryUtils;
  private CurrentTenantProvider currentTenantProvider;

  @BeforeEach
  void setUp() {
    queryUtils = new QueryUtils();
    currentTenantProvider = Mockito.mock(CurrentTenantProvider.class);
    queryUtils.currentTenantProvider = currentTenantProvider;
  }

  private void currentTenantIs(String tenantId) {
    Mockito.when(currentTenantProvider.currentTenantId()).thenReturn(Optional.ofNullable(tenantId));
  }

  @Test
  void buildQueryRestrictsToCurrentTenantAndNotAnotherTenant() {
    currentTenantIs(TENANT_AR);

    Document query = queryUtils.buildQuery(Map.of("institution.id", "institution-id"));

    String json = query.toJson();
    assertTrue(json.contains("tenantId"), json);
    assertTrue(json.contains(TENANT_AR), json);
    assertFalse(json.contains(TENANT_PNPG), json);
    assertTrue(json.contains("institution.id"), json);
  }

  @Test
  void buildQueryScopesAnOtherwiseUnfilteredQuery() {
    currentTenantIs(TENANT_AR);

    Document query = queryUtils.buildQuery(Map.of());

    assertFalse(query.isEmpty(), "unfiltered reads must still be tenant-scoped");
    assertTrue(query.toJson().contains(TENANT_AR), query.toJson());
  }

  @Test
  void buildQueryAdmitsLegacyDocumentsWithNoTenantDuringMigration() {
    currentTenantIs(TENANT_AR);

    Document query = queryUtils.buildQuery(Map.of("productId", "prod-io"));

    assertTrue(query.toJson().contains("null"),
      "legacy untagged records must stay reachable until the tenant backfill completes: " + query.toJson());
  }

  @Test
  void tenantScopedAddsTheSameMigrationPredicateToNativeDocumentQueries() {
    currentTenantIs(TENANT_AR);

    Document query = queryUtils.tenantScoped(new Document("_id", "onboarding-id"));

    String json = query.toJson();
    assertTrue(json.contains("_id"), json);
    assertTrue(json.contains("tenantId"), json);
    assertTrue(json.contains(TENANT_AR), json);
    assertTrue(json.contains("null"), json);
  }

  @Test
  void buildQueryAndTenantScopedAreLeftUnscopedWhenNoTenantIsResolvable() {
    currentTenantIs(null);

    Document filtered = queryUtils.buildQuery(Map.of("institution.id", "institution-id"));
    Document unfiltered = queryUtils.buildQuery(Map.of());
    Document nativeQuery = queryUtils.tenantScoped(new Document("_id", "onboarding-id"));

    assertFalse(filtered.toJson().contains("tenantId"), filtered.toJson());
    assertEquals(new Document(), unfiltered);
    assertFalse(nativeQuery.toJson().contains("tenantId"), nativeQuery.toJson());
    assertTrue(nativeQuery.toJson().contains("_id"), nativeQuery.toJson());
  }
}
