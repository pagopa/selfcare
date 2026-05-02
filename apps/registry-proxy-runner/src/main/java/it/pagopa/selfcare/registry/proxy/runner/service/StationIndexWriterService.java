package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.AnacStation;
import it.pagopa.selfcare.registry.proxy.runner.model.AnacStationIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class StationIndexWriterService
    extends AbstractIndexWriterService<AnacStation, AnacStationIndex> {

  @ConfigProperty(name = "azure-ai-search.station.index-name")
  String indexName;

  @ConfigProperty(name = "azure-ai-search.api-version")
  String apiVersion;

  @Inject
  public StationIndexWriterService(@RestClient AzureSearchRestClient azureSearchRestClient) {
    super(azureSearchRestClient);
  }

  @Override
  protected String getId(AnacStation item) {
    return item.getId();
  }

  /** Stations do not have an updateDate in ANAC data, so they are always re-indexed. */
  @Override
  protected String getUpdateDate(AnacStation item) {
    return null;
  }

  @Override
  protected Function<AnacStation, AnacStationIndex> toDocument() {
    return AnacStationIndex::fromStation;
  }

  @Override
  protected String getIndexName() {
    return indexName;
  }

  @Override
  protected String getApiVersion() {
    return apiVersion;
  }

  @Override
  protected String getEntityName() {
    return "Station";
  }
}
