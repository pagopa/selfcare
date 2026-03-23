package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.CategoryTokenAnalyzer;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.DocumentToCategoryConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.CategoryQueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.InstitutionQueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
class CategoryIndexSearchService extends IndexSearchServiceTemplate<Category> {


    @Autowired
    public CategoryIndexSearchService(Directory categoriesDirectory) {
        super(categoriesDirectory, new CategoryTokenAnalyzer(), new DocumentToCategoryConverter());
    }


    @Override
    protected QueryResult<Category> getQueryResult(List<Category> items, long totalHits) {
        final CategoryQueryResult queryResult = new CategoryQueryResult();
        queryResult.setItems(items);
        queryResult.setTotalHits(totalHits);
        return queryResult;
    }

    protected QueryResult<Institution> getQueryResultFiltered(List<Institution> items, long totalHits) {
        final InstitutionQueryResult queryResult = new InstitutionQueryResult();
        queryResult.setItems(items);
        queryResult.setTotalHits(totalHits);
        return queryResult;
    }



}
