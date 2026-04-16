package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StationServiceImpl implements StationService {
    private final IndexSearchService<Station> indexSearchService;

    @Autowired
    StationServiceImpl(IndexSearchService<Station> indexSearchService) {
        log.trace("Initializing {}", StationServiceImpl.class.getSimpleName());
        this.indexSearchService = indexSearchService;
    }

    @Override
    public QueryResult<Station> search(Optional<String> searchText, int page, int limit) {
        log.trace("search start");
        log.debug("search searchText = {}, page = {}, limit = {}", searchText, page, limit);
        final QueryResult<Station> queryResult = searchText.map(text -> indexSearchService.fullTextSearch(Station.Field.DESCRIPTION, text, page, limit))
                .orElseGet(() -> indexSearchService.findAll(page, limit, Entity.STATION.toString()));
        log.debug("search result = {}", queryResult);
        log.trace("search end");
        return queryResult;
    }

    @Override
    public Station findByTaxId(String taxId) {
        log.trace("find SA By taxId start");
        log.debug("find SA By taxId = {}", taxId.toUpperCase());
        final List<Station> pdndList = indexSearchService.findById(Station.Field.ID, taxId.toUpperCase());
        if (pdndList.isEmpty()) {
            throw new ResourceNotFoundException();
        } else if (pdndList.size() > 1) {
            throw new TooManyResourceFoundException();
        }
        final Station pdnd = pdndList.get(0);
        log.debug("find SA By taxId result = {}", pdnd);
        log.trace("find SA By taxId end");
        return pdnd;
    }

}
