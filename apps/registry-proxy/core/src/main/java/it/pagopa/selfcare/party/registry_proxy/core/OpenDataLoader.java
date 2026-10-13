package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.OpenDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.*;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
  static final String FORCE_REINDEX_ARG = "--force-reindex";

  private final boolean ipaStartupIndexingEnabled;

  @Autowired
  public OpenDataLoader(
      List<OpenDataConnector> openDataConnectors,
      IndexWriterService<Institution> institutionIndexWriterService,
      IndexWriterService<Category> categoryIndexWriterService,
      IndexWriterService<AOO> aooIndexWriterService,
      IndexWriterService<UO> uoIndexWriterService,
      IndexWriterService<Station> stationIndexWriterService,
      IndexWriterService<InsuranceCompany> ivassIndexWriterService,
      ANACService anacService,
      IvassDataConnector ivassDataConnector,
      @Value("${app.startup.ipa-institution-indexing.enabled:false}") boolean ipaStartupIndexingEnabled) {
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
    this.ipaStartupIndexingEnabled = ipaStartupIndexingEnabled;
  }

  @Override
  public void run(String... args) {
    log.trace("run start");
    final boolean forceReindex = Arrays.asList(args).contains(FORCE_REINDEX_ARG);
    openDataConnectors.forEach(
        openDataConnector -> {
          loadIpaInstitutions(openDataConnector, forceReindex);
          categoryIndexWriterService.adds(openDataConnector.getCategories());
          aooIndexWriterService.adds(openDataConnector.getAOOs());
          uoIndexWriterService.adds(openDataConnector.getUOs());
          stationIndexWriterService.adds(anacService.loadStations());
          ivassIndexWriterService.adds(ivassDataConnector.getInsurances());
        });
    log.trace("run end");
  }

  /** Indexes the IPA institutions unless inhibited by config; --force-reindex overrides both. */
  private void loadIpaInstitutions(OpenDataConnector openDataConnector, boolean forceReindex) {
    if (!ipaStartupIndexingEnabled && !forceReindex) {
      log.info(
          "IPA institutions startup indexing is disabled (app.startup.ipa-institution-indexing.enabled=false)");
      return;
    }
    List<? extends Institution> institutions = openDataConnector.getInstitutions();
    if (forceReindex) {
      log.info("Forced IPA institutions reindex: skipping up-to-date checks");
      institutionIndexWriterService.addsForced(institutions);
    } else {
      institutionIndexWriterService.adds(institutions);
    }
  }
}
