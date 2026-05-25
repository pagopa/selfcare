package it.pagopa.selfcare.registry.proxy.runner;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusMain
public class RegistryProxyMain implements QuarkusApplication {

  @Inject AnacDataService anacDataService;
  @Inject StationIndexWriterService stationIndexWriterService;

  @Inject IpaInstitutionOpenDataService ipaInstitutionOpenDataService;
  @Inject InstitutionIndexWriterService institutionIndexWriterService;

  @Inject IpaAOOOpenDataService ipaAooOpenDataService;
  @Inject IpaAOOIndexWriterService ipaAOOIndexWriterService;

  @Inject IpaCategoryOpenDataService ipaCategoryOpenDataService;
  @Inject CategoryIndexWriterService categoryIndexWriterService;

  @Inject IpaUoOpenDataService ipaUoOpenDataService;
  @Inject IpaUOIndexWriterService ipaUOIndexWriterService;

  @Inject IvassDataService ivassDataService;
  @Inject InsuranceCompanyIndexWriterService insuranceCompanyIndexWriterService;

  @Override
  public int run(String... args) {
    log.info("Starting registry proxy runner");
    boolean success = true;

    success &= runTask("ANAC stations", () -> stationIndexWriterService.index(anacDataService.fetch()));
    success &= runTask("IPA institutions", () -> institutionIndexWriterService.index(ipaInstitutionOpenDataService.fetch()));
    success &= runTask("IPA AOO", () -> ipaAOOIndexWriterService.index(ipaAooOpenDataService.fetch()));
    success &= runTask("IPA categories", () -> categoryIndexWriterService.index(ipaCategoryOpenDataService.fetch()));
    success &= runTask("IPA UO", () -> ipaUOIndexWriterService.index(ipaUoOpenDataService.fetch()));
    success &= runTask("IVASS insurance companies", () -> insuranceCompanyIndexWriterService.index(ivassDataService.fetch()));

    if (success) {
      log.info("All index updates completed successfully");
    } else {
      log.warn("One or more index updates failed — check logs above for details");
    }
    return 0;
  }

  private boolean runTask(String taskName, Runnable task) {
    try {
      log.info("Starting index update: {}", taskName);
      task.run();
      log.info("Completed index update: {}", taskName);
      return true;
    } catch (Exception e) {
      log.error("Error during index update: {}", taskName, e);
      return false;
    }
  }
}
