package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.InsuranceCompanyTokenAnalyzer;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.analysis.StationTokenAnalyzer;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.DocumentToInsuranceCompanyConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.DocumentToStationConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.InsuranceCompanyQueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.StationQueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
class IvassIndexSearchService extends IndexSearchServiceTemplate<InsuranceCompany> {

    @Autowired
    public IvassIndexSearchService(Directory ivassDirectory) {
        super(ivassDirectory, new InsuranceCompanyTokenAnalyzer(), new DocumentToInsuranceCompanyConverter());
    }

    @Override
    protected QueryResult<InsuranceCompany> getQueryResult(List<InsuranceCompany> items, long totalHits) {
        final InsuranceCompanyQueryResult queryResult = new InsuranceCompanyQueryResult();
        queryResult.setItems(items);
        queryResult.setTotalHits(totalHits);
        return queryResult;
    }

}
