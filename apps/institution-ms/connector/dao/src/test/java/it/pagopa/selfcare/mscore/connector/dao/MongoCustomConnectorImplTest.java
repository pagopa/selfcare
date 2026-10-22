package it.pagopa.selfcare.mscore.connector.dao;

import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionAggregation;
import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoCustomConnectorImplTest {
    private MongoCustomConnectorImpl mongoCustomConnector;

    @Mock
    private MongoOperations mongoOperations;

    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {
        tenantContext = new TenantContext();
        tenantContext.setTenantId("AR");
        mongoCustomConnector = new MongoCustomConnectorImpl(
                mongoOperations,
                new TenantOwnershipPolicy(tenantContext, true));
    }

    @Test
    void testExist() {
        when(mongoOperations.exists(any(), (Class<?>) any())).thenReturn(true);
        Query query = new Query();
        assertTrue(mongoCustomConnector.exists(query, Object.class));
        assertTenantScoped(captureQuery(captor ->
                verify(mongoOperations).exists(captor.capture(), (Class<?>) any())));
    }

    @Test
    void testCount() {
        when(mongoOperations.count(any(), (Class<?>) any())).thenReturn(10L);
        Query query = new Query();
        assertEquals(10L, mongoCustomConnector.count(query, Object.class));
        assertTenantScoped(captureQuery(captor ->
                verify(mongoOperations).count(captor.capture(), (Class<?>) any())));
    }


    @Test
    void testFind() {
        List<Object> list = new ArrayList<>();
        when(mongoOperations.find(any(), any())).thenReturn(list);
        Query query = new Query();
        assertNotNull(mongoCustomConnector.find(query, Object.class));
        assertTenantScoped(captureQuery(captor ->
                verify(mongoOperations).find(captor.capture(), (Class<?>) any())));
    }

    @Test
    void testFind2() {
        Query query = new Query();
        Pageable page = Pageable.ofSize(3);
        assertNotNull(mongoCustomConnector.find(query, page, Object.class));
        assertTenantScoped(captureQuery(captor ->
                verify(mongoOperations).count(captor.capture(), (Class<?>) any())));
    }

    @Test
    void testFind3() {
        Object a = new Object();
        List<Object> list = new ArrayList<>();
        list.add(a);
        when(mongoOperations.count(any(), (Class<?>) any())).thenReturn(1L);
        when(mongoOperations.find(any(), any())).thenReturn(list);
        Query query = new Query();
        Pageable page = Pageable.ofSize(3);
        assertNotNull(mongoCustomConnector.find(query, page, Object.class));
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoOperations).count(captor.capture(), (Class<?>) any());
        assertTenantScoped(captor.getValue());
        verify(mongoOperations).find(captor.capture(), (Class<?>) any());
        assertTenantScoped(captor.getValue());
    }

    @Test
    void findAndModify() {
        Object a = new Object();
        List<Object> list = new ArrayList<>();
        list.add(a);
        when(mongoOperations.findAndModify(any(), any(), any(), (Class<Object>) any())).thenReturn(list);
        Query query = new Query();
        UpdateDefinition updateDefinition = new Update();
        FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().upsert(false).returnNew(true);
        assertNotNull(mongoCustomConnector.findAndModify(query, updateDefinition, findAndModifyOptions, Object.class));
        assertTenantScoped(captureQuery(captor ->
                verify(mongoOperations).findAndModify(
                        captor.capture(), any(), any(), (Class<Object>) any())));
    }

    @Test
    void findUserAndInstitution() {
        when(mongoOperations.aggregate((Aggregation) any(), anyString(), any())).thenReturn(mock(AggregationResults.class));
        UserInstitutionFilter filter = new UserInstitutionFilter();
        filter.setUserId("userId");
        Assertions.assertDoesNotThrow(() -> mongoCustomConnector.findUserInstitutionAggregation(filter, UserInstitutionAggregation.class));
        assertGraphLookupTenantScope(captureAggregation(), false);

    }

    @Test
    void findUserAndInstitutionWithInstitutionId() {
        when(mongoOperations.aggregate((Aggregation) any(), anyString(), any())).thenReturn(mock(AggregationResults.class));
        UserInstitutionFilter filter = new UserInstitutionFilter();
        filter.setUserId("userId");
        filter.setInstitutionId("institutionId");
        Assertions.assertDoesNotThrow(() -> mongoCustomConnector.findUserInstitutionAggregation(filter, UserInstitutionAggregation.class));

    }

    @Test
    void findUserAndInstitutionWithExternalId() {
        when(mongoOperations.aggregate((Aggregation) any(), anyString(), any())).thenReturn(mock(AggregationResults.class));
        UserInstitutionFilter filter = new UserInstitutionFilter();
        filter.setUserId("userId");
        filter.setExternalId("externalId");
        Assertions.assertDoesNotThrow(() -> mongoCustomConnector.findUserInstitutionAggregation(filter, UserInstitutionAggregation.class));
        assertGraphLookupTenantScope(captureAggregation(), true);

    }

    @Test
    void updateMulti() {
        UpdateResult updateResult = mock(UpdateResult.class);
        when(mongoOperations.updateMulti(any(), any(), (Class<Object>) any())).thenReturn(updateResult);
        Query query = new Query();
        UpdateDefinition updateDefinition = new Update();
        assertNotNull(mongoCustomConnector.updateMulti(query, updateDefinition, Object.class));
        assertTenantScoped(captureQuery(captor ->
                verify(mongoOperations).updateMulti(
                        captor.capture(), any(), (Class<Object>) any())));
    }

    @Test
    void findAndRemove() {
        Object a = new Object();
        when(mongoOperations.findAndRemove(any(), any())).thenReturn(a);
        Query query = new Query();
        assertNotNull(mongoCustomConnector.findAndRemove(query, Object.class));
        assertTenantScoped(captureQuery(captor ->
                verify(mongoOperations).findAndRemove(captor.capture(), (Class<?>) any())));
    }

    @Test
    void upsert() {
        UpdateResult updateResult = mock(UpdateResult.class);
        when(mongoOperations.upsert(any(), any(), (Class<Object>) any())).thenReturn(updateResult);
        Query query = new Query();
        UpdateDefinition updateDefinition = new Update();
        assertNotNull(mongoCustomConnector.upsert(query, updateDefinition, Object.class));
        assertTenantScoped(captureQuery(captor ->
                verify(mongoOperations).upsert(captor.capture(), any(), (Class<Object>) any())));
    }

    private Query captureQuery(Consumer<ArgumentCaptor<Query>> verification) {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verification.accept(captor);
        return captor.getValue();
    }

    private Aggregation captureAggregation() {
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoOperations).aggregate(
                captor.capture(), anyString(), eq(UserInstitutionAggregation.class));
        return captor.getValue();
    }

    private void assertGraphLookupTenantScope(Aggregation aggregation, boolean externalIdExpected) {
        List<Document> stages = aggregation.toPipeline(Aggregation.DEFAULT_CONTEXT);
        assertTrue(stages.get(0).get("$match", Document.class).toString().contains("tenantId"));
        Document graphLookup = stages.stream()
                .filter(stage -> stage.containsKey("$graphLookup"))
                .findFirst()
                .orElseThrow()
                .get("$graphLookup", Document.class);
        String restriction = graphLookup.get("restrictSearchWithMatch", Document.class).toString();
        assertTrue(restriction.contains("tenantId"));
        assertEquals(externalIdExpected, restriction.contains("externalId"));
    }

    private void assertTenantScoped(Query query) {
        assertEquals("AR", query.getQueryObject().getString("tenantId"));
    }

}
