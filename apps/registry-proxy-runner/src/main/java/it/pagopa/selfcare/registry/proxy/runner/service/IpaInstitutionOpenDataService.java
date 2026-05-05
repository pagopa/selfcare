package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.IpaInstitutionOpenDataRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
@ApplicationScoped
public class IpaInstitutionOpenDataService extends AbstractIpaOpenDataService<IpaInstitution> {

  @Inject @RestClient IpaInstitutionOpenDataRestClient restClient;

  @Override
  public List<IpaInstitution> fetch() {
    log.info("Fetching IPA institutions from open data...");
    return parseCsv(restClient.retrieveDataSource(), IpaInstitution.class, "institutions");
  }
}
