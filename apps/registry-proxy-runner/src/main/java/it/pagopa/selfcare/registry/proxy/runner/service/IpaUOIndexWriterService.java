package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaUo;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaUoIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class IpaUOIndexWriterService extends AbstractIndexWriterService<IpaUo, IpaUoIndex> {

  @ConfigProperty(name = "azure-ai-search.uo.index-name")
  String indexName;

  @ConfigProperty(name = "azure-ai-search.api-version")
  String apiVersion;

  IpaUOIndexWriterService() {
    super(null);
  }

  @Inject
  public IpaUOIndexWriterService(@RestClient AzureSearchRestClient azureSearchRestClient) {
    super(azureSearchRestClient);
  }

  @Override
  protected String getId(IpaUo item) {
    return item.getId();
  }

  @Override
  protected String getUpdateDate(IpaUo item) {
    return item.getDataAggiornamento();
  }

  @Override
  protected Function<IpaUo, IpaUoIndex> toDocument() {
    return IpaUoIndex::fromUo;
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
    return "UO";
  }
}
