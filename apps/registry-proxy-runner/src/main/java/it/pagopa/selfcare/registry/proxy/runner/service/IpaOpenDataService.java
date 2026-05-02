package it.pagopa.selfcare.registry.proxy.runner.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.registry.proxy.runner.client.IpaOpenDataRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaAoo;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaUo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
@ApplicationScoped
public class IpaOpenDataService {

  @Inject @RestClient IpaOpenDataRestClient ipaOpenDataRestClient;

  public List<IpaInstitution> fetchInstitutions() {
    log.info("Fetching IPA institutions from open data...");
    return parseCsv(
        ipaOpenDataRestClient.retrieveInstitutions(), IpaInstitution.class, "institutions");
  }

  public List<IpaCategory> fetchCategories() {
    log.info("Fetching IPA categories from open data...");
    return parseCsv(ipaOpenDataRestClient.retrieveCategories(), IpaCategory.class, "categories");
  }

  public List<IpaAoo> fetchAOOs() {
    log.info("Fetching IPA AOOs from open data...");
    return parseCsv(ipaOpenDataRestClient.retrieveAOOs(), IpaAoo.class, "AOOs");
  }

  /**
   * Fetches UOs and enriches them with the codiceFiscaleSfe field from the separate SFE endpoint.
   */
  public List<IpaUo> fetchUOs() {
    log.info("Fetching IPA UOs from open data...");
    List<IpaUo> uos = parseCsv(ipaOpenDataRestClient.retrieveUOs(), IpaUo.class, "UOs");

    log.info("Fetching IPA UOs with SFE from open data...");
    List<IpaUo> uosWithSfe =
        parseCsv(ipaOpenDataRestClient.retrieveUOsWithSfe(), IpaUo.class, "UOs with SFE");

    // Enrich UOs with codiceFiscaleSfe from the SFE dataset
    Map<String, IpaUo> uoMap =
        uos.stream().collect(Collectors.toMap(IpaUo::getId, Function.identity()));

    uosWithSfe.forEach(
        sfeUo -> {
          IpaUo existingUo = uoMap.get(sfeUo.getId());
          if (existingUo != null
              && sfeUo.getCodiceFiscaleSfe() != null
              && !sfeUo.getCodiceFiscaleSfe().isBlank()) {
            existingUo.setCodiceFiscaleSfe(sfeUo.getCodiceFiscaleSfe());
          }
        });

    log.info(
        "Enriched {} UOs with SFE data",
        uos.stream()
            .filter(uo -> uo.getCodiceFiscaleSfe() != null && !uo.getCodiceFiscaleSfe().isBlank())
            .count());
    return uos;
  }

  private <T> List<T> parseCsv(String csv, Class<T> type, String entityName) {
    try (Reader reader =
        new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csv.getBytes())))) {
      CsvToBean<T> csvToBean =
          new CsvToBeanBuilder<T>(reader).withType(type).withIgnoreLeadingWhiteSpace(true).build();
      List<T> result = csvToBean.parse();
      log.info("Fetched {} {} from IPA open data", result.size(), entityName);
      return result;
    } catch (Exception e) {
      log.error("Error fetching IPA {} from open data", entityName, e);
      return Collections.emptyList();
    }
  }
}
