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
public class IpaAOORegistryProxyScheduler {

  @Inject IpaAOOOpenDataService ipaAooOpenDataService;

  @Inject IpaAOOIndexWriterService ipaAOOIndexWriterService;

  /**
   * Runs 4 times a day (every 6 hours) to feed AI Search indexes with IPA, ANAC and IVASS data from
   * open data sources.
   */
  @Scheduled(cron = "{scheduler.ipa-index-aoo.cron}")
  void feedAiSearchIndex() {
    log.info("Starting scheduled AI Search index update");
    try {
      // IPA indexes
      List<IpaAoo> aoos = ipaAooOpenDataService.fetch();
      ipaAOOIndexWriterService.index(aoos);

      log.info("Completed scheduled AI Search index update");
    } catch (Exception e) {
      log.error("Error during scheduled AI Search index update", e);
    }
  }
}
