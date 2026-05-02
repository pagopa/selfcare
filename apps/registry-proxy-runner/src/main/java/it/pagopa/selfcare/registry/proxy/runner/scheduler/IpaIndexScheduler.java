package it.pagopa.selfcare.registry.proxy.runner.scheduler;

import io.quarkus.scheduler.Scheduled;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaAoo;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaUo;
import it.pagopa.selfcare.registry.proxy.runner.service.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class IpaIndexScheduler {

    @Inject
    IpaOpenDataService ipaOpenDataService;

    @Inject
    InstitutionIndexWriterService institutionIndexWriterService;

    @Inject
    CategoryIndexWriterService categoryIndexWriterService;

    @Inject
    AooIndexWriterService aooIndexWriterService;

    @Inject
    UoIndexWriterService uoIndexWriterService;

    /**
     * Runs 4 times a day (every 6 hours) to feed AI Search indexes
     * with IPA data (institutions, categories, AOOs, UOs) from open data.
     */
    @Scheduled(cron = "{scheduler.ipa-index.cron}")
    void feedAiSearchIndex() {
        log.info("Starting scheduled IPA AI Search index update");
        try {
            List<IpaInstitution> institutions = ipaOpenDataService.fetchInstitutions();
            institutionIndexWriterService.index(institutions);

            List<IpaCategory> categories = ipaOpenDataService.fetchCategories();
            categoryIndexWriterService.index(categories);

            List<IpaAoo> aoos = ipaOpenDataService.fetchAOOs();
            aooIndexWriterService.index(aoos);

            // UOs are enriched with codiceFiscaleSfe from the SFE endpoint
            List<IpaUo> uos = ipaOpenDataService.fetchUOs();
            uoIndexWriterService.index(uos);

            log.info("Completed scheduled IPA AI Search index update");
        } catch (Exception e) {
            log.error("Error during scheduled IPA AI Search index update", e);
        }
    }
}
