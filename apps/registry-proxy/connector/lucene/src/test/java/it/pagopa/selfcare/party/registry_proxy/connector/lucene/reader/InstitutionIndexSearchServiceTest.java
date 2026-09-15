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
    void fullTextSearch_withUnbalancedDoubleQuote_doesNotThrow() {
        // given: value with an unclosed double quote that makes the Lucene classic
        final SearchField field = Field.DESCRIPTION;
        final String value = "Istituto istruzione superiore \"sol";
        final int page = 1;
        final int limit = 10;
        // when
        final QueryResult<Institution> queryResult =
                assertDoesNotThrow(() -> indexSearchService.fullTextSearch(field, value, page, limit));
        // then
        assertNotNull(queryResult);
        assertNotNull(queryResult.getItems());
    }


    @Test
    void fullTextSearch_withOtherUnbalancedSpecialChars_doesNotThrow() {
        // given: other unbalanced Lucene special characters
        final SearchField field = Field.DESCRIPTION;
        final int page = 1;
        final int limit = 10;
        // when / then
        assertDoesNotThrow(() -> indexSearchService.fullTextSearch(field, "istituto (sol", page, limit));
        assertDoesNotThrow(() -> indexSearchService.fullTextSearch(field, "istituto [sol", page, limit));
        assertDoesNotThrow(() -> indexSearchService.fullTextSearch(field, "istituto sol\"", page, limit));
    }


    @Test
    void fullTextSearchWithCategories_withUnbalancedDoubleQuote_doesNotThrow() {
        // given
        final SearchField descriptionField = Field.DESCRIPTION;
        final SearchField categoryField = Field.CATEGORY;
        final String description = "Istituto istruzione superiore \"sol";
        final String categories = "L6,L7";
        final int page = 1;
        final int limit = 10;
        // when
        final QueryResult<Institution> queryResult =
                assertDoesNotThrow(() -> indexSearchService.fullTextSearch(
                        descriptionField, description, categoryField, categories, page, limit));
        // then
        assertNotNull(queryResult);
        assertNotNull(queryResult.getItems());
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
