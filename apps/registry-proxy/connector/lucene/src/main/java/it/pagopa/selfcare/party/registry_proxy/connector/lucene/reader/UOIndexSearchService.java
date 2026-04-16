package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.UOTokenAnalyzer;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.DocumentToUOConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.UOQueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
class UOIndexSearchService extends IndexSearchServiceTemplate<UO> {


    @Autowired
    public UOIndexSearchService(Directory uosDirectory) {
        super(uosDirectory, new UOTokenAnalyzer(), new DocumentToUOConverter());
    }

    @Override
    protected QueryResult<UO> getQueryResult(List<UO> items, long totalHits) {
        final UOQueryResult queryResult = new UOQueryResult();
        queryResult.setItems(items);
        queryResult.setTotalHits(totalHits);
        return queryResult;
    }

}
