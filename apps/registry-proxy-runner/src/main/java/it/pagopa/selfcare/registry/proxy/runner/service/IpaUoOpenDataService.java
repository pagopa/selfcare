package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.IpaUOOpenDataRestClient;
import it.pagopa.selfcare.registry.proxy.runner.client.IpaUOSfeOpenDataRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaUo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
@ApplicationScoped
public class IpaUoOpenDataService extends AbstractIpaOpenDataService<IpaUo> {

  @Inject @RestClient IpaUOOpenDataRestClient restClient;

  @Inject @RestClient IpaUOSfeOpenDataRestClient sfeRestClient;

  @Override
  public List<IpaUo> fetch() {
    log.info("Fetching IPA UOs from open data...");
    List<IpaUo> uos = parseCsv(restClient.retrieveDataSource(), IpaUo.class, "UOs", "ipa-uo");

    log.info("Fetching IPA UOs with SFE from open data...");
    List<IpaUo> uosWithSfe =
        parseCsv(sfeRestClient.retrieveDataSource(), IpaUo.class, "UOs with SFE", "ipa-uo-sfe");

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
}
