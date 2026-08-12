package it.pagopa.selfcare.user.util;

import it.pagopa.selfcare.user.conf.CurrentTenantProvider;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static it.pagopa.selfcare.user.constant.CollectionUtil.USER_INSTITUTION_COLLECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the tenant predicate injected into {@link QueryUtils}, which is the single point every
 * filter-map-driven query in this service passes through (Step_0 sub-task 6).
 *
 * <p>These assertions are on the shape of the query document, not on what Mongo returns: this module
 * has no embedded Mongo on its test classpath. That the predicate actually excludes another tenant's
 * documents is verified against a real database in {@code document-ms}'s
 * {@code DocumentRepositoryTenantTest}; what matters here is that the predicate is emitted at all,
 * and emitted for the right tenant.
 */
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

    private Map<String, Object> institutionFilter() {
        return UserInstitutionFilter.builder().institutionId("institutionId").build().constructMap();
    }

    private void currentTenantIs(String tenantId) {
        Mockito.when(currentTenantProvider.currentTenantId()).thenReturn(Optional.ofNullable(tenantId));
    }

    @Test
    void buildQueryDocumentRestrictsToCurrentTenantAndKeepsCallerFilters() {
        currentTenantIs(TENANT_AR);

        Document query = queryUtils.buildQueryDocument(institutionFilter(), USER_INSTITUTION_COLLECTION);

        String json = query.toJson();
        assertTrue(json.contains("tenantId"), "the query must carry a tenant predicate: " + json);
        assertTrue(json.contains(TENANT_AR), "the predicate must name the current tenant: " + json);
        assertTrue(json.contains("institutionId"), "the caller's own filter must survive: " + json);
    }

    @Test
    void buildQueryDocumentAdmitsRecordsWrittenBeforeTheDiscriminatorExisted() {
        currentTenantIs(TENANT_AR);

        Document query = queryUtils.buildQueryDocument(institutionFilter(), USER_INSTITUTION_COLLECTION);

        // Migration-phase concession: untagged documents predate the discriminator and would otherwise
        // become invisible to every tenant at once. This assertion is expected to be inverted once the
        // backfill has run and the filter becomes strictly fail-closed.
        assertTrue(query.toJson().contains("null"),
                "untagged documents must stay reachable during the migration: " + query.toJson());
    }

    @Test
    void buildQueryDocumentScopesEvenWhenTheCallerPassedNoFilters() {
        currentTenantIs(TENANT_AR);

        Document query = queryUtils.buildQueryDocument(Map.of(), USER_INSTITUTION_COLLECTION);

        // The unfiltered "list everything" path is exactly the one where a missing tenant predicate
        // would leak the most, so an empty filter map must not short-circuit the scoping.
        assertFalse(query.isEmpty(), "an unfiltered query must still be tenant-scoped");
        assertTrue(query.toJson().contains(TENANT_AR), query.toJson());
    }

    @Test
    void buildQueryDocumentUsesTheTenantOfTheCurrentRequestNotAFixedOne() {
        currentTenantIs(TENANT_PNPG);

        Document query = queryUtils.buildQueryDocument(institutionFilter(), USER_INSTITUTION_COLLECTION);

        String json = query.toJson();
        assertTrue(json.contains(TENANT_PNPG), json);
        assertFalse(json.contains(TENANT_AR), "a request must never be scoped to another tenant: " + json);
    }

    @Test
    void buildQueryDocumentIsLeftUnscopedWhenNoTenantIsResolvable() {
        currentTenantIs(null);

        Document filtered = queryUtils.buildQueryDocument(institutionFilter(), USER_INSTITUTION_COLLECTION);
        Document unfiltered = queryUtils.buildQueryDocument(Map.of(), USER_INSTITUTION_COLLECTION);

        // Event consumers and schedulers run outside a request and have no tenant to scope to. They
        // keep the pre-multitenant behaviour rather than being handed an unsatisfiable query.
        assertFalse(filtered.toJson().contains("tenantId"), filtered.toJson());
        assertEquals(new Document(), unfiltered);
    }

    @Test
    void buildQueryDocumentByDateIsScopedToo() {
        currentTenantIs(TENANT_AR);

        Document query = queryUtils.buildQueryDocumentByDate(
                institutionFilter(), USER_INSTITUTION_COLLECTION, OffsetDateTime.now());

        String json = query.toJson();
        assertTrue(json.contains("tenantId"), json);
        assertTrue(json.contains(TENANT_AR), json);
    }
}
