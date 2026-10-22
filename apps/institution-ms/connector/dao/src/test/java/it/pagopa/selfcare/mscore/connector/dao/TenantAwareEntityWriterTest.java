package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.mscore.connector.dao.model.InstitutionEntity;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAwareEntityWriterTest {

    @Mock
    private MongoOperations mongoOperations;

    private TenantContext tenantContext;
    private TenantAwareEntityWriter writer;

    @BeforeEach
    void setUp() {
        tenantContext = new TenantContext();
        tenantContext.setTenantId("AR");
        writer = new TenantAwareEntityWriter(
                mongoOperations,
                new TenantOwnershipPolicy(tenantContext, false),
                tenantContext);
    }

    @Test
    void atomicallyReplacesOwnedDocumentWithCurrentTenantMarker() {
        InstitutionEntity replacement = new InstitutionEntity();
        replacement.setId("institution-1");
        when(mongoOperations.findAndReplace(
                any(Query.class),
                eq(replacement),
                any(FindAndReplaceOptions.class))).thenReturn(replacement);

        InstitutionEntity saved = writer.replace(
                "institution-1",
                replacement,
                InstitutionEntity.class,
                InstitutionEntity::setTenantId);

        assertEquals(replacement, saved);
        assertEquals("AR", replacement.getTenantId());
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<FindAndReplaceOptions> optionsCaptor =
                ArgumentCaptor.forClass(FindAndReplaceOptions.class);
        verify(mongoOperations).findAndReplace(
                queryCaptor.capture(),
                eq(replacement),
                optionsCaptor.capture());
        Document query = queryCaptor.getValue().getQueryObject();
        assertEquals("institution-1", query.getString("_id"));
        assertEquals(java.util.Arrays.asList("AR", null), query.get("tenantId", Document.class)
                .get("$in", java.util.List.class));
        assertEquals(true, optionsCaptor.getValue().isUpsert());
        assertEquals(true, optionsCaptor.getValue().isReturnNew());
    }

    @Test
    void upsertsNewDocumentWithCurrentTenantMarker() {
        InstitutionEntity replacement = new InstitutionEntity();
        replacement.setId("institution-new");
        when(mongoOperations.findAndReplace(
                any(Query.class),
                eq(replacement),
                any(FindAndReplaceOptions.class))).thenReturn(replacement);

        writer.replace(
                "institution-new",
                replacement,
                InstitutionEntity.class,
                InstitutionEntity::setTenantId);

        assertEquals("AR", replacement.getTenantId());
        verify(mongoOperations, never()).save(any(InstitutionEntity.class));
    }

    @Test
    void conflictingOwnershipFailsAfterGuardedAtomicUpsert() {
        InstitutionEntity replacement = new InstitutionEntity();
        replacement.setId("foreign-id");
        when(mongoOperations.findAndReplace(
                any(Query.class),
                eq(replacement),
                any(FindAndReplaceOptions.class))).thenThrow(new DuplicateKeyException("duplicate _id"));
        when(mongoOperations.exists(any(Query.class), eq(InstitutionEntity.class))).thenReturn(true);

        assertThrows(TenantOwnershipConflictException.class, () -> writer.replace(
                "foreign-id",
                replacement,
                InstitutionEntity.class,
                InstitutionEntity::setTenantId));

        InOrder order = inOrder(mongoOperations);
        order.verify(mongoOperations).findAndReplace(
                any(Query.class),
                eq(replacement),
                any(FindAndReplaceOptions.class));
        order.verify(mongoOperations).exists(any(Query.class), eq(InstitutionEntity.class));
        verify(mongoOperations, never()).save(any(InstitutionEntity.class));
    }
}
