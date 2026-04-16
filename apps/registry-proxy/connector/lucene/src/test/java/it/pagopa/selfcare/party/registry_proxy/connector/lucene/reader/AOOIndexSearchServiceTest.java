package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.config.InMemoryIndexConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.AOOEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        InMemoryIndexConfig.class,
        AOOIndexSearchService.class
})
class AOOIndexSearchServiceTest {

    @Autowired
    private AOOIndexSearchService indexSearchService;

    @Autowired
    private Directory categoriesDirectory;

    private List<AOOEntity> categories;

    @Test
    void getQueryResult() {
        final QueryResult<AOO> results = indexSearchService.getQueryResult(new ArrayList<>(), 1);
        assertNotNull(results);
    }


}