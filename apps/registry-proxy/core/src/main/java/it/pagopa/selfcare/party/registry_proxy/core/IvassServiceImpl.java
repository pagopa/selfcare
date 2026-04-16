package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IvassServiceImpl implements IvassService {

    private final IndexSearchService<InsuranceCompany> indexSearchService;
    private final IvassDataConnector ivassDataConnector;
    private final IndexWriterService<InsuranceCompany> indexWriterService;

    @Autowired
    IvassServiceImpl(
            IndexSearchService<InsuranceCompany> indexSearchService,
            IndexWriterService<InsuranceCompany> indexWriterService,
            IvassDataConnector ivassDataConnector
    ) {
        log.trace("Initializing {}", IvassServiceImpl.class.getSimpleName());
        this.indexSearchService = indexSearchService;
        this.indexWriterService = indexWriterService;
        this.ivassDataConnector = ivassDataConnector;
    }

    @Override
    public InsuranceCompany findByOriginId(String originId) {
        log.trace("findByIvassCode start");
        final List<InsuranceCompany> companies = indexSearchService.findById(InsuranceCompany.Field.ORIGIN_ID, originId.toUpperCase());
        if (companies.isEmpty()) {
            throw new ResourceNotFoundException();
        } else if (companies.size() > 1) {
            throw new TooManyResourceFoundException();
        }
        final InsuranceCompany company = companies.get(0);
        log.debug("findByIvassCode result = {}", company);
        log.trace("findByIvassCode end");
        return company;
    }

    @Override
    public QueryResult<InsuranceCompany> search(Optional<String> searchText, int page, int limit) {
        log.trace("search start");
        log.debug("search searchText = {}, page = {}, limit = {}", searchText, page, limit);
        final QueryResult<InsuranceCompany> queryResult = searchText.map(text -> indexSearchService.fullTextSearch(InsuranceCompany.Field.DESCRIPTION, text, page, limit))
                .orElseGet(() -> indexSearchService.findAll(page, limit, Entity.INSURANCE_COMPANY.toString()));
        log.debug("search result = {}", queryResult);
        log.trace("search end");
        return queryResult;
    }

    @Scheduled(cron = "0 0 0/6 * * *")
    void updateIvassIndex() {
        log.trace("start update IVASS Stations index");
        List<InsuranceCompany> companies = ivassDataConnector.getInsurances();
        if (!companies.isEmpty()) {
            indexWriterService.cleanIndex(Entity.INSURANCE_COMPANY.toString());
            indexWriterService.adds(companies);
        }
        log.trace("updated IVASS Stations index end");
    }
}
