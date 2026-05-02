package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import io.quarkus.scheduler.Scheduled;
import it.pagopa.selfcare.registry.proxy.runner.model.*;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class IpaIndexScheduler {

  @Inject IpaOpenDataService ipaOpenDataService;

  @Inject AnacDataService anacDataService;

  @Inject IvassDataService ivassDataService;

  @Inject InstitutionIndexWriterService institutionIndexWriterService;

  @Inject CategoryIndexWriterService categoryIndexWriterService;

  @Inject AooIndexWriterService aooIndexWriterService;

  @Inject UoIndexWriterService uoIndexWriterService;

  @Inject StationIndexWriterService stationIndexWriterService;

  @Inject InsuranceCompanyIndexWriterService insuranceCompanyIndexWriterService;

  /**
   * Runs 4 times a day (every 6 hours) to feed AI Search indexes with IPA, ANAC and IVASS data from
   * open data sources.
   */
  @Scheduled(cron = "{scheduler.ipa-index.cron}")
  void feedAiSearchIndex() {
    log.info("Starting scheduled AI Search index update");
    try {
      // IPA indexes
      List<IpaInstitution> institutions = ipaOpenDataService.fetchInstitutions();
      institutionIndexWriterService.index(institutions);

      List<IpaCategory> categories = ipaOpenDataService.fetchCategories();
      categoryIndexWriterService.index(categories);

      List<IpaAoo> aoos = ipaOpenDataService.fetchAOOs();
      aooIndexWriterService.index(aoos);

      // UOs enriched with codiceFiscaleSfe from the SFE endpoint
      List<IpaUo> uos = ipaOpenDataService.fetchUOs();
      uoIndexWriterService.index(uos);

      // ANAC stations
      List<AnacStation> stations = anacDataService.fetchStations();
      stationIndexWriterService.index(stations);

      // IVASS insurance companies
      List<IvassInsuranceCompany> insuranceCompanies = ivassDataService.fetchInsuranceCompanies();
      insuranceCompanyIndexWriterService.index(insuranceCompanies);

      log.info("Completed scheduled AI Search index update");
    } catch (Exception e) {
      log.error("Error during scheduled AI Search index update", e);
    }
  }
}
