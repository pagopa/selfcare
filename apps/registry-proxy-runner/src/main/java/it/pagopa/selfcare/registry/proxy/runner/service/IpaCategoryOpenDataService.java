package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.IpaCategoriesOpenDataRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
@ApplicationScoped
public class IpaCategoryOpenDataService extends AbstractIpaOpenDataService<IpaCategory> {

  @Inject @RestClient IpaCategoriesOpenDataRestClient restClient;

  @Override
  public List<IpaCategory> fetch() {
    log.info("Fetching IPA categories from open data...");
    return parseCsv(restClient.retrieveDataSource(), IpaCategory.class, "categories");
  }
}
