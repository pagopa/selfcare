package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.domain.Sort.by;

class TenantOwnershipPolicyTest {

    private final TenantContext tenantContext = new TenantContext();

    @AfterEach
    void clearTenantContext() {
        tenantContext.clear();
    }

    @Test
    void strictModeMatchesOnlyTheCurrentTenant() {
        tenantContext.setTenantId("ar");

        Document criteria = new TenantOwnershipPolicy(tenantContext, true)
                .criteria()
                .getCriteriaObject();

        assertEquals(new Document("tenantId", "AR"), criteria);
    }

    @Test
    void compatibilityModeMatchesCurrentTenantAndBothNullAndMissingMarkers() {
        tenantContext.setTenantId("AR");

        Document criteria = new TenantOwnershipPolicy(tenantContext, false)
                .criteria()
                .getCriteriaObject();

        assertEquals(java.util.Arrays.asList("AR", null),
                criteria.get("tenantId", Document.class).get("$in"));
    }

    @Test
    void ownershipCriterionIsConjoinedWithExistingOrDomainFilter() {
        tenantContext.setTenantId("AR");
        Criteria existingOr = new Criteria().orOperator(
                Criteria.where("status").is("ACTIVE"),
                Criteria.where("status").is("PENDING"));

        Document criteria = new TenantOwnershipPolicy(tenantContext, false)
                .and(existingOr)
                .getCriteriaObject();

        java.util.List<Document> and = criteria.getList("$and", Document.class);
        assertEquals(existingOr.getCriteriaObject(), and.get(0));
        assertEquals(java.util.Arrays.asList("AR", null),
                and.get(1).get("tenantId", Document.class).get("$in"));
    }

    @Test
    void queryScopePreservesProjectionSortPaginationAndCollation() {
        tenantContext.setTenantId("AR");
        org.springframework.data.mongodb.core.query.Query query =
                org.springframework.data.mongodb.core.query.Query.query(
                        Criteria.where("status").is("ACTIVE"))
                        .skip(2)
                        .limit(5)
                        .with(by(DESC, "createdAt"))
                        .collation(org.springframework.data.mongodb.core.query.Collation.of("en"));
        query.fields().include("id");

        org.springframework.data.mongodb.core.query.Query scoped =
                new TenantOwnershipPolicy(tenantContext, false).scope(query);

        assertEquals(2, scoped.getSkip());
        assertEquals(5, scoped.getLimit());
        assertEquals(query.getFieldsObject(), scoped.getFieldsObject());
        assertEquals(query.getSortObject(), scoped.getSortObject());
        assertEquals(query.getCollation(), scoped.getCollation());
        assertTrue(scoped.getQueryObject().containsKey("tenantId"));
    }

    @Test
    void ownershipCriteriaRequiresTenantContext() {
        assertThrows(
                IllegalStateException.class,
                () -> new TenantOwnershipPolicy(tenantContext, true).criteria());
    }
}
