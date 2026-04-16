package it.pagopa.selfcare.party.registry_proxy.core;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexSearchService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.OpenDataRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.IPAOpenDataUO;
import it.pagopa.selfcare.party.registry_proxy.core.exception.TooManyResourceFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
class UOServiceImpl implements UOService {

    private final IndexSearchService<UO> indexSearchService;
    private final InstitutionService institutionService;
    private final IndexWriterService<UO> uoIndexWriterService;
    private final OpenDataRestClient openDataRestClient;
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    UOServiceImpl(IndexSearchService<UO> indexSearchService,
                  InstitutionService institutionService,
                  IndexWriterService<UO> uoIndexWriterService,
                  OpenDataRestClient openDataRestClient) {
        log.trace("Initializing {}", UOServiceImpl.class.getSimpleName());
        this.indexSearchService = indexSearchService;
        this.institutionService = institutionService;
        this.uoIndexWriterService = uoIndexWriterService;
        this.openDataRestClient = openDataRestClient;
    }

    @Override
    public QueryResult<UO> findAll(Optional<String> taxCodeInvoicing, int page, int limit) {
        log.trace("find all UO start");
        log.debug("find all UO, page = {}, limit = {}", page, limit);
        final QueryResult<UO> queryResult = taxCodeInvoicing.map(text -> indexSearchService.fullTextSearch(UO.Field.CODICE_FISCALE_SFE, text, page, limit))
                .orElseGet(() -> indexSearchService.findAll(page, limit, Entity.UO.toString()));
        log.debug("find all UO result = {}", queryResult);
        log.trace("find all UO end");
        return queryResult;
    }

    @Override
    public UO findByUnicode(String codiceUniUO, List<String> categoriesList) {
        log.trace("find UO By CodiceUniUO start");
        log.debug("find UO By CodiceUniUO = {} - categoriesList = {}", codiceUniUO.toUpperCase(), categoriesList);
        final List<UO> uoList = indexSearchService.findById(UO.Field.ID, codiceUniUO.toUpperCase());
        if (uoList.isEmpty()) {
            throw new ResourceNotFoundException();
        } else if (uoList.size() > 1) {
            throw new TooManyResourceFoundException();
        }
        final UO uo = uoList.get(0);
        if(categoriesList != null && !categoriesList.isEmpty()){
            institutionService.findById(uo.getCodiceFiscaleEnte(), Optional.of(Origin.IPA), categoriesList);
        }
        log.debug("find UO By CodiceUniUO result = {}", uo);
        log.trace("find UO By CodiceUniUO end");
        return uo;
    }

    @Scheduled(cron = "0 0 2 * * *")
    void updateUOsIndex() {
        log.trace("start update UOs index");
        final List<UO> uos = filterForLastUpdate(getUOs());
        if (!uos.isEmpty()) {
            uoIndexWriterService.adds(uos);
        }
        final List<UO> uosWithSfe = filterForLastUpdate(getUOsWithSfe());

        uosWithSfe.forEach(uo -> {
            List<UO> result = indexSearchService.findById(UO.Field.ID, uo.getId().toUpperCase());
            if (result.isEmpty()) {
                log.error("Document with ID: {} not found", uo.getId());
            } else if (result.size() > 1) {
                log.error("Too many documents with ID: {}", uo.getId());
            } else {
                uoIndexWriterService.updateDocumentValues(result.get(0), Map.of(UO.Field.CODICE_FISCALE_SFE.toString(), uo.getCodiceFiscaleSfe()));
            }
        });
        log.trace("updated UOs index end");
    }

    private List<UO> getUOs() {
        log.trace("getUOs start");
        List<UO> uos;
        final String csv = openDataRestClient.retrieveUOs();

        try (Reader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csv.getBytes())))) {
            CsvToBean<UO> csvToBean = new CsvToBeanBuilder<UO>(reader)
                    .withType(IPAOpenDataUO.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            uos = csvToBean.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.trace("getUOs end");
        return uos;
    }

    private List<UO> getUOsWithSfe() {
        log.trace("getUOsWithSfe start");
        List<UO> uos;
        final String csv = openDataRestClient.retrieveUOsWithSfe();

        try (Reader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csv.getBytes())))) {
            CsvToBean<UO> csvToBean = new CsvToBeanBuilder<UO>(reader)
                    .withType(IPAOpenDataUO.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            uos = csvToBean.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.trace("getUOsWithSfe end");
        return uos;
    }

    private List<UO> filterForLastUpdate(List<UO> uos) {
        return uos.stream()
                .filter(uo -> !uo.getDataAggiornamento().isEmpty())
                .filter(uo -> {
                    try {
                        final var pattern = DateTimeFormatter.ofPattern(DATE_FORMAT);
                        final LocalDate dt = LocalDate.from(LocalDate.parse(uo.getDataAggiornamento(), pattern));
                        return dt.isEqual(LocalDate.now().minusDays(1));
                    } catch (Exception e) {
                        log.error("Impossible to parse date {}", uo.getDataAggiornamento());
                        return false;
                    }
                }).toList();
    }

}
