package it.pagopa.selfcare.mscore.connector.dao;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionAggregation;
import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class MongoCustomConnectorImplTest {
    @InjectMocks
    private MongoCustomConnectorImpl mongoCustomConnector;

    @Mock
    private MongoOperations mongoOperations;

    @Test
    void testExist() {
        when(mongoOperations.exists(any(), (Class<?>) any())).thenReturn(true);
        Query query = new Query();
        assertTrue(mongoCustomConnector.exists(query, Object.class));
    }

    @Test
    void testCount() {
        when(mongoOperations.count(any(), (Class<?>) any())).thenReturn(10L);
        Query query = new Query();
        assertEquals(10L, mongoCustomConnector.count(query, Object.class));
    }


    @Test
    void testFind() {
        List<Object> list = new ArrayList<>();
        when(mongoOperations.find(any(), any())).thenReturn(list);
        Query query = new Query();
        assertNotNull(mongoCustomConnector.find(query, Object.class));
    }

    @Test
    void testFind2() {
        List<Object> list = new ArrayList<>();
        when(mongoOperations.find(any(), any())).thenReturn(list);
        Query query = new Query();
        Pageable page = Pageable.ofSize(3);
        assertNotNull(mongoCustomConnector.find(query, page, Object.class));
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
    }

    @Test
    void findUserAndInstitution() {
        AggregationResults<Object> results = mock(AggregationResults.class);
        when(results.getUniqueMappedResult()).thenReturn(new Object());
        when(mongoOperations.aggregate((Aggregation) any(), anyString(), any())).thenReturn(results);
        UserInstitutionFilter filter = new UserInstitutionFilter();
        filter.setUserId("userId");
        Assertions.assertDoesNotThrow(() -> mongoCustomConnector.findUserInstitutionAggregation(filter, UserInstitutionAggregation.class));

    }

    @Test
    void findUserAndInstitutionWithInstitutionId() {
        AggregationResults<Object> results = mock(AggregationResults.class);
        when(results.getUniqueMappedResult()).thenReturn(new Object());
        when(mongoOperations.aggregate((Aggregation) any(), anyString(), any())).thenReturn(results);
        UserInstitutionFilter filter = new UserInstitutionFilter();
        filter.setUserId("userId");
        filter.setInstitutionId("institutionId");
        Assertions.assertDoesNotThrow(() -> mongoCustomConnector.findUserInstitutionAggregation(filter, UserInstitutionAggregation.class));

    }

    @Test
    void findUserAndInstitutionWithExternalId() {
        AggregationResults<Object> results = mock(AggregationResults.class);
        when(results.getUniqueMappedResult()).thenReturn(new Object());
        when(mongoOperations.aggregate((Aggregation) any(), anyString(), any())).thenReturn(results);
        UserInstitutionFilter filter = new UserInstitutionFilter();
        filter.setUserId("userId");
        filter.setExternalId("externalId");
        Assertions.assertDoesNotThrow(() -> mongoCustomConnector.findUserInstitutionAggregation(filter, UserInstitutionAggregation.class));

    }

    @Test
    void updateMulti() {
        UpdateResult updateResult = mock(UpdateResult.class);
        when(mongoOperations.updateMulti(any(), any(), (Class<Object>) any())).thenReturn(updateResult);
        Query query = new Query();
        UpdateDefinition updateDefinition = new Update();
        assertNotNull(mongoCustomConnector.updateMulti(query, updateDefinition, Object.class));
    }

    @Test
    void findAndRemove() {
        Object a = new Object();
        when(mongoOperations.findAndRemove(any(), any())).thenReturn(a);
        Query query = new Query();
        assertNotNull(mongoCustomConnector.findAndRemove(query, Object.class));
    }

    @Test
    void upsert() {
        UpdateResult updateResult = mock(UpdateResult.class);
        when(mongoOperations.upsert(any(), any(), (Class<Object>) any())).thenReturn(updateResult);
        Query query = new Query();
        UpdateDefinition updateDefinition = new Update();
        assertNotNull(mongoCustomConnector.upsert(query, updateDefinition, Object.class));
    }

}

