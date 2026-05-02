package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.IpaAOOOpenDataRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaAoo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
@ApplicationScoped
public class IpaAOOOpenDataService extends AbstractIpaOpenDataService<IpaAoo> {

  @Inject @RestClient IpaAOOOpenDataRestClient restClient;

  @Override
  public List<IpaAoo> fetch() {
    log.info("Fetching IPA AOOs from open data...");
    return parseCsv(restClient.retrieveDataSource(), IpaAoo.class, "AOOs");
  }
}
