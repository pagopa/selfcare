package it.pagopa.selfcare.party.registry_proxy.core;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution.Field;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.OpenDataRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.IPAOpenDataInstitution;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
class InstitutionServiceImpl implements InstitutionService {

    private final IndexSearchService<Institution> indexSearchService;
    private final OpenDataRestClient openDataRestClient;
    private final IndexWriterService<Institution> institutionIndexWriterService;

    @Autowired
    InstitutionServiceImpl(IndexSearchService<Institution> indexSearchService, OpenDataRestClient openDataRestClient, IndexWriterService<Institution> institutionIndexWriterService) {
        this.openDataRestClient = openDataRestClient;
        this.institutionIndexWriterService = institutionIndexWriterService;
        log.trace("Initializing {}", InstitutionServiceImpl.class.getSimpleName());
        this.indexSearchService = indexSearchService;
    }

    @Override
    public QueryResult<Institution> search(Optional<String> searchText, int page, int limit) {
        log.trace("search start");
        log.debug("search searchText = {}, page = {}, limit = {}", searchText, page, limit);
        final QueryResult<Institution> queryResult = searchText.map(text -> indexSearchService.fullTextSearch(Field.DESCRIPTION, text, page, limit))
                .orElseGet(() -> indexSearchService.findAll(page, limit, Entity.INSTITUTION.toString()));
        log.debug("search result = {}", queryResult);
        log.trace("search end");
        return queryResult;
    }

    @Override
    public QueryResult<Institution> search(Optional<String> searchText, String categories, int page, int limit) {
        log.trace("search start");
        log.debug("search searchText = {}, categories = {}, page = {}, limit = {}", searchText, categories, page, limit);
        final QueryResult<Institution> queryResult = searchText.map(text -> indexSearchService.fullTextSearch(Field.DESCRIPTION, searchText.orElseThrow(), Field.CATEGORY, categories, page, limit))
                .orElseGet(() -> indexSearchService.findAll(page, limit, Entity.INSTITUTION.toString()));

        log.debug("search result = {}", queryResult);
        log.trace("search end");
        return queryResult;
    }

    @Override
    public Institution findById(String id, Optional<Origin> origin, List<String> categories) {
        log.trace("findById start");
        log.debug("findById id = {}, origin = {}", id, origin);
        if (origin.map(Origin.INFOCAMERE::equals).orElse(false)) {
            throw new RuntimeException("Not implemented yet");//TODO: onboarding privati
        } else {
            final Supplier<List<Institution>> institutionsSupplier = () -> indexSearchService.findById(Field.ID, id);
            List<Institution> institutions;

            Origin orig = origin.get();
            institutions = institutionsSupplier.get().stream()
                    .filter(institution -> institution.getOrigin().equals(orig) &&
                            (categories.isEmpty() || categories.contains(institution.getCategory())))
                    .toList();

            if (institutions.isEmpty()) {
                throw new ResourceNotFoundException();
            } else if (institutions.size() > 1) {
                throw new TooManyResourceFoundException();
            }
            final Institution institution = institutions.get(0);
            log.debug("findById result = {}", institution);
            log.trace("findById end");
            return institution;
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    void updateInstitutionsIndex() {
        log.trace("start update Institutions IPA index");
        List<Institution> institutions = getInstitutions();
        if (!institutions.isEmpty()) {
            institutionIndexWriterService.cleanIndex(Entity.INSTITUTION.toString());
            institutionIndexWriterService.adds(institutions);
        }
        log.trace("updated Institutions IPA index end");
    }

    private List<Institution> getInstitutions() {
        log.trace("getInstitutions start");
        List<Institution> institutions;
        final String csv = openDataRestClient.retrieveInstitutions();

        try (Reader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csv.getBytes())))) {
            CsvToBean<Institution> csvToBean = new CsvToBeanBuilder<Institution>(reader)
                    .withType(IPAOpenDataInstitution.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            institutions = csvToBean.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.trace("getInstitutions end");
        return institutions;
    }
}
