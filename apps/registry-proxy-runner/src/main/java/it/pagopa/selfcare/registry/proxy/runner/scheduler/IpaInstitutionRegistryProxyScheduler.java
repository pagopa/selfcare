package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import it.pagopa.selfcare.registry.proxy.runner.model.*;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class IpaInstitutionRegistryProxyScheduler {

  @Inject IpaInstitutionOpenDataService ipaInstitutionOpenDataService;

  @Inject InstitutionIndexWriterService institutionIndexWriterService;

  void onStart(@Observes StartupEvent ev) {
    log.info("Application started: triggering initial AI Search index update");
    feedAiSearchIndex();
  }

  /**
   * Runs 4 times a day (every 6 hours) to feed AI Search indexes with IPA, ANAC and IVASS data from
   * open data sources.
   */
  @Scheduled(cron = "{scheduler.ipa-index-institution.cron}")
  void feedAiSearchIndex() {
    log.info("Starting scheduled AI Search index update");
    try {
      // IPA indexes
      List<IpaInstitution> institutions = ipaInstitutionOpenDataService.fetch();
      institutionIndexWriterService.index(institutions);

      log.info("Completed scheduled AI Search index update");
    } catch (Exception e) {
      log.error("Error during scheduled AI Search index update", e);
    }
  }
}
