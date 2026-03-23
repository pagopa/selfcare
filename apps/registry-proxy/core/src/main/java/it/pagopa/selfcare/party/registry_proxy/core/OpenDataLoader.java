package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.OpenDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OpenDataLoader implements CommandLineRunner {

    private final List<OpenDataConnector> openDataConnectors;
    private final IndexWriterService<Institution> institutionIndexWriterService;
    private final IndexWriterService<Category> categoryIndexWriterService;
    private final IndexWriterService<AOO> aooIndexWriterService;
    private final IndexWriterService<UO> uoIndexWriterService;
    private final IndexWriterService<Station> stationIndexWriterService;
    private final IndexWriterService<InsuranceCompany> ivassIndexWriterService;
    private final ANACService anacService;
    private final IvassDataConnector ivassDataConnector;

    @Autowired
    public OpenDataLoader(List<OpenDataConnector> openDataConnectors,
                          IndexWriterService<Institution> institutionIndexWriterService,
                          IndexWriterService<Category> categoryIndexWriterService,
                          IndexWriterService<AOO> aooIndexWriterService,
                          IndexWriterService<UO> uoIndexWriterService,
                          IndexWriterService<Station> stationIndexWriterService,
                          IndexWriterService<InsuranceCompany> ivassIndexWriterService,
                          ANACService anacService,
                          IvassDataConnector ivassDataConnector) {
        log.trace("Initializing {}", OpenDataLoader.class.getSimpleName());
        this.openDataConnectors = openDataConnectors;
        this.institutionIndexWriterService = institutionIndexWriterService;
        this.categoryIndexWriterService = categoryIndexWriterService;
        this.aooIndexWriterService = aooIndexWriterService;
        this.uoIndexWriterService = uoIndexWriterService;
        this.stationIndexWriterService = stationIndexWriterService;
        this.ivassIndexWriterService = ivassIndexWriterService;
        this.anacService = anacService;
        this.ivassDataConnector = ivassDataConnector;
    }

    @Override
    public void run(String... args) {
        log.trace("run start");
        openDataConnectors.forEach(openDataConnector -> {
            institutionIndexWriterService.adds(openDataConnector.getInstitutions());
            categoryIndexWriterService.adds(openDataConnector.getCategories());
            aooIndexWriterService.adds(openDataConnector.getAOOs());
            uoIndexWriterService.adds(openDataConnector.getUOs());
            stationIndexWriterService.adds(anacService.loadStations());
            ivassIndexWriterService.adds(ivassDataConnector.getInsurances());
        });
        log.trace("run end");
    }

}
