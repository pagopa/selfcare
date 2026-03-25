package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.Category.createId;

@Slf4j
@Service
class CategoryServiceImpl implements CategoryService {

    private final IndexSearchService<Category> indexSearchService;

    @Autowired
    CategoryServiceImpl(IndexSearchService<Category> indexSearchService) {
        log.trace("Initializing {}", CategoryServiceImpl.class.getSimpleName());
        this.indexSearchService = indexSearchService;
    }


    @Override
    public QueryResult<Category> search(Optional<Origin> origin, int page, int limit) {
        log.trace("search start");
        log.debug("search origin = {}, page = {}, limit = {}", origin, page, limit);
        final QueryResult<Category> queryResult;
        if (origin.isPresent()) {
            final QueryFilter queryFilter = new QueryFilter();
            queryFilter.setField(Field.ORIGIN);
            queryFilter.setValue(origin.get().toString());
            queryResult = indexSearchService.findAll(page, limit, Entity.CATEGORY.toString(), queryFilter);
        } else {
            queryResult = indexSearchService.findAll(page, limit, Entity.CATEGORY.toString());
        }
        log.debug("search result = {}", queryResult);
        log.trace("search end");
        return queryResult;
    }


    @Override
    public Category findById(String id, Origin origin) {
        log.trace("findById start");
        log.debug("findById id = {}, origin = {}", id, origin);
        if (Origin.INFOCAMERE.equals(origin)) {
            throw new RuntimeException("Data source not found");//FIXME: there is no INFOCAMERE categories...choose the right exception to throw
        } else {
            final List<Category> categories = indexSearchService.findById(Field.ID, createId(origin, id));
            if (categories.isEmpty()) {
                throw new ResourceNotFoundException();
            } else if (categories.size() > 1) {
                throw new TooManyResourceFoundException();
            } else {
                final Category category = categories.get(0);
                log.debug("findById result = {}", category);
                log.trace("findById end");
                return category;
            }
        }
    }

}
