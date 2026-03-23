package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.AOOTokenAnalyzer;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.DocumentToAOOConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.AOOQueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
class AOOIndexSearchService extends IndexSearchServiceTemplate<AOO> {


    @Autowired
    public AOOIndexSearchService(Directory aoosDirectory) {
        super(aoosDirectory, new AOOTokenAnalyzer(), new DocumentToAOOConverter());
    }

    @Override
    protected QueryResult<AOO> getQueryResult(List<AOO> items, long totalHits) {
        final AOOQueryResult queryResult = new AOOQueryResult();
        queryResult.setItems(items);
        queryResult.setTotalHits(totalHits);
        return queryResult;
    }

}
