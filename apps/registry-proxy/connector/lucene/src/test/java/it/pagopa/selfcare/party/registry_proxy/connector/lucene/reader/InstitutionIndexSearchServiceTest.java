package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.config.InMemoryIndexConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.InstitutionEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer.DummyInstitutionIndexWriterFactory;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer.DummyInstitutionIndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer.IndexWriterFactory;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution.Field;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.annotation.PostConstruct;
import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        InMemoryIndexConfig.class,
        InstitutionIndexSearchService.class
})
class InstitutionIndexSearchServiceTest {

    @Autowired
    private IndexSearchService<Institution> indexSearchService;

    @Autowired
    private Directory institutionsDirectory;

    private List<InstitutionEntity> institutions;


    @PostConstruct
    void init() {
        institutions = List.of(mockInstance(new InstitutionEntity(), 1), mockInstance(new InstitutionEntity(), 2));
        final IndexWriterFactory indexWriterFactory = new DummyInstitutionIndexWriterFactory(institutionsDirectory);
        IndexWriterService<Institution> indexWriterService = new DummyInstitutionIndexWriterService(indexWriterFactory);
        indexWriterService.adds(institutions);
    }


    @Test
    void fullTextSearch() {
        // given
        final SearchField field = Field.DESCRIPTION;
        final String value = "description";
        final int page = 1;
        final int limit = 10;
        // when
        final QueryResult<Institution> queryResult = indexSearchService.fullTextSearch(field, value, page, limit);
        // then
        assertNotNull(queryResult);
        assertEquals(2, queryResult.getTotalHits());
        assertNotNull(queryResult.getItems());
        assertEquals(2, queryResult.getItems().size());
        assertIterableEquals(institutions, queryResult.getItems());
    }


    @Test
    void findById() {
        // given
        final SearchField field = Field.ID;
        final String value = institutions.get(0).getId();
        // when
        final List<Institution> results = indexSearchService.findById(field, value);
        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(institutions.get(0), results.get(0));
    }


    @Test
    void findAll_WithoutFilters() {
        // given
        final int page = 1;
        final int limit = 10;
        // when
        final QueryResult<Institution> queryResult = indexSearchService.findAll(page, limit, Entity.INSTITUTION.toString());
        // then
        assertNotNull(queryResult);
        assertEquals(2, queryResult.getTotalHits());
        assertNotNull(queryResult.getItems());
        assertEquals(2, queryResult.getItems().size());
        assertIterableEquals(institutions, queryResult.getItems());
    }


    @Test
    void findAll_WithFilters() {
        // given
        final int page = 1;
        final int limit = 10;
        final QueryFilter filter = new QueryFilter();
        filter.setField(Field.ID);
        filter.setValue(institutions.get(0).getId());
        // when
        final QueryResult<Institution> queryResult = indexSearchService.findAll(page, limit, Entity.INSTITUTION.toString(), filter);
        // then
        assertNotNull(queryResult);
        assertEquals(1, queryResult.getTotalHits());
        assertNotNull(queryResult.getItems());
        assertEquals(1, queryResult.getItems().size());
        assertEquals(institutions.get(0), queryResult.getItems().get(0));
    }

}