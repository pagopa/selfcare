package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaAoo;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaAooIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class AooIndexWriterService extends AbstractIndexWriterService<IpaAoo, IpaAooIndex> {

  @ConfigProperty(name = "azure-ai-search.aoo.index-name")
  String indexName;

  @ConfigProperty(name = "azure-ai-search.api-version")
  String apiVersion;

  @Inject
  public AooIndexWriterService(@RestClient AzureSearchRestClient azureSearchRestClient) {
    super(azureSearchRestClient);
  }

  @Override
  protected String getId(IpaAoo item) {
    return item.getId();
  }

  @Override
  protected String getUpdateDate(IpaAoo item) {
    return item.getDataAggiornamento();
  }

  @Override
  protected Function<IpaAoo, IpaAooIndex> toDocument() {
    return IpaAooIndex::fromAoo;
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
    return "AOO";
  }
}
