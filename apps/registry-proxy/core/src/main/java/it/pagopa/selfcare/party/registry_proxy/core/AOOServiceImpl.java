package it.pagopa.selfcare.party.registry_proxy.core;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.OpenDataRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.IPAOpenDataAOO;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
class AOOServiceImpl implements AOOService {

    private final IndexSearchService<AOO> indexSearchService;
    private final InstitutionService institutionService;
    private final IndexWriterService<AOO> aooIndexWriterService;
    private final OpenDataRestClient openDataRestClient;

    AOOServiceImpl(IndexSearchService<AOO> indexSearchService,
                   InstitutionService institutionService, IndexWriterService<AOO> aooIndexWriterService, OpenDataRestClient openDataRestClient) {
        this.aooIndexWriterService = aooIndexWriterService;
        this.openDataRestClient = openDataRestClient;
        log.trace("Initializing {}", AOOServiceImpl.class.getSimpleName());
        this.indexSearchService = indexSearchService;
        this.institutionService = institutionService;
    }

    @Override
    public QueryResult<AOO> findAll(int page, int limit) {
        log.trace("find all AOO start");
        log.debug("find all AOO all with page = {}, limit = {}", page, limit);
        final QueryResult<AOO> queryResult;
        queryResult = indexSearchService.findAll(page, limit, Entity.AOO.toString());
        log.debug("find all AOO result = {}", queryResult);
        log.trace("find all AOO end");
        return queryResult;
    }


    @Override
    public AOO findByUnicode(String codiceUniAOO, List<String> categoriesList) {
        log.trace("find AOO by CodiceUniAOO start");
        log.debug("find AOO by CodiceUniAOO = {} - categoriesList = {}", codiceUniAOO.toUpperCase(), categoriesList);
        final List<AOO> aooList = indexSearchService.findById(AOO.Field.ID, codiceUniAOO.toUpperCase());
        if (aooList.isEmpty()) {
            throw new ResourceNotFoundException();
        } else if (aooList.size() > 1) {
            throw new TooManyResourceFoundException();
        }
        final AOO aoo = aooList.get(0);
        if(categoriesList != null && !categoriesList.isEmpty()){
           institutionService.findById(aoo.getCodiceFiscaleEnte(), Optional.of(Origin.IPA), categoriesList);
        }
        log.debug("find AOO by CodiceUniAOO result = {}", aoo);
        log.trace("find AOO by CodiceUniAOO end");
        return aoo;
    }

    @Scheduled(cron = "0 0 2 * * *")
    void updateAOOsIndex() {
        log.trace("start update AOO IPA index");
        List<AOO> aoo = getAOOs();
        if (!aoo.isEmpty()) {
            aooIndexWriterService.cleanIndex(Entity.AOO.toString());
            aooIndexWriterService.adds(aoo);
        }
        log.trace("updated AOO IPA index end");
    }

    private List<AOO> getAOOs() {
        log.trace("getAOOs start");
        List<AOO> aoo;
        final String csv = openDataRestClient.retrieveAOOs();

        try (Reader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csv.getBytes())))) {
            CsvToBean<AOO> csvToBean = new CsvToBeanBuilder<AOO>(reader)
                    .withType(IPAOpenDataAOO.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            aoo = csvToBean.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.trace("getAOOs end");
        return aoo;
    }
}
