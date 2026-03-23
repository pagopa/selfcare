package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.config.InMemoryIndexConfig;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        InMemoryIndexConfig.class,
        IvassIndexSearchService.class
})
class IvassIndexSearchServiceTest {

    @Autowired
    private IvassIndexSearchService indexSearchService;

    @Test
    void getQueryResult() {
        final QueryResult<InsuranceCompany> results = indexSearchService.getQueryResult(new ArrayList<>(), 1);
        assertNotNull(results);
    }

}