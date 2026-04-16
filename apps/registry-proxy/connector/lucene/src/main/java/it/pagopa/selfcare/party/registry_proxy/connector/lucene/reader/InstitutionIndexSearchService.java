package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.InstitutionTokenAnalyzer;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.DocumentToInstitutionConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.InstitutionQueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
class InstitutionIndexSearchService extends IndexSearchServiceTemplate<Institution> {

    @Autowired
    public InstitutionIndexSearchService(Directory institutionsDirectory) {
        super(institutionsDirectory, new InstitutionTokenAnalyzer(), new DocumentToInstitutionConverter());
    }


    @Override
    protected QueryResult<Institution> getQueryResult(List<Institution> items, long totalHits) {
        final InstitutionQueryResult queryResult = new InstitutionQueryResult();
        queryResult.setItems(items);
        queryResult.setTotalHits(totalHits);
        return queryResult;
    }

}
