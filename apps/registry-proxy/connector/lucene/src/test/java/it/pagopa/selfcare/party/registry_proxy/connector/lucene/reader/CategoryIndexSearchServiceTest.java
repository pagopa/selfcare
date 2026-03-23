package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.config.InMemoryIndexConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.CategoryEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer.DummyCategoryIndexWriterFactory;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer.DummyCategoryIndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer.IndexWriterFactory;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        InMemoryIndexConfig.class,
        CategoryIndexSearchService.class
})
class CategoryIndexSearchServiceTest {

    @Autowired
    private IndexSearchService<Category> indexSearchService;

    @Autowired
    private CategoryIndexSearchService categoryIndexSearchService;

    @Autowired
    private Directory categoriesDirectory;

    private List<CategoryEntity> categories;


    @PostConstruct
    void init() {
        categories = List.of(mockInstance(new CategoryEntity(), 1), mockInstance(new CategoryEntity(), 2));
        final IndexWriterFactory indexWriterFactory = new DummyCategoryIndexWriterFactory(categoriesDirectory);
        IndexWriterService<Category> indexWriterService = new DummyCategoryIndexWriterService(indexWriterFactory);
        indexWriterService.adds(categories);
    }


    @Test
    void fullTextSearch() {
        // given
        final SearchField field = Field.ORIGIN;
        final String value = categories.get(0).getOrigin().toString();
        final int page = 1;
        final int limit = 10;
        // when
        final QueryResult queryResult = indexSearchService.fullTextSearch(field, value, page, limit);
        // then
        assertNotNull(queryResult);
        assertEquals(2, queryResult.getTotalHits());
        assertNotNull(queryResult.getItems());
        assertEquals(2, queryResult.getItems().size());
        assertIterableEquals(categories, queryResult.getItems());
    }

    @Test
    void fullTextSearch1() {
        // given
        final SearchField field = Field.ORIGIN;
        final String value = categories.get(0).getOrigin().toString();
        final int page = 1;
        final int limit = 10;
        // when
        final QueryResult queryResult = indexSearchService.fullTextSearch(field, value, field, "cateogires", page, limit);
        // then
        assertNotNull(queryResult);
        assertEquals(0, queryResult.getTotalHits());
    }


    @Test
    void findById() {
        // given
        final SearchField field = Field.ID;
        final String value = categories.get(0).getId();
        // when
        final List<Category> results = indexSearchService.findById(field, value);
        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(categories.get(0), results.get(0));
    }


    @Test
    void findAll_WithoutFilters() {
        // given
        final int page = 1;
        final int limit = 10;
        // when
        final QueryResult<Category> queryResult = indexSearchService.findAll(page, limit, Entity.CATEGORY.toString());
        // then
        assertNotNull(queryResult);
        assertEquals(2, queryResult.getTotalHits());
        assertNotNull(queryResult.getItems());
        assertEquals(2, queryResult.getItems().size());
        assertIterableEquals(categories, queryResult.getItems());
    }


    @Test
    void findAll_WithFilters() {
        // given
        final int page = 1;
        final int limit = 10;
        final QueryFilter filter = new QueryFilter();
        filter.setField(Field.ID);
        filter.setValue(categories.get(0).getId());
        // when
        final QueryResult<Category> queryResult = indexSearchService.findAll(page, limit, Entity.CATEGORY.toString(), filter);
        // then
        assertNotNull(queryResult);
        assertEquals(1, queryResult.getTotalHits());
        assertNotNull(queryResult.getItems());
        assertEquals(1, queryResult.getItems().size());
        assertEquals(categories.get(0), queryResult.getItems().get(0));
    }

    @Test
    void getQueryResultFiltered(){
        assertNotNull(categoryIndexSearchService.getQueryResultFiltered(new ArrayList<>(),0));
    }
}