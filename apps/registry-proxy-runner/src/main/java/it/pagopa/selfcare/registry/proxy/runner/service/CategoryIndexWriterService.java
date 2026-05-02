package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategoryIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CategoryIndexWriterService
    extends AbstractIndexWriterService<IpaCategory, IpaCategoryIndex> {

  @ConfigProperty(name = "azure-ai-search.category.index-name")
  String indexName;

  @ConfigProperty(name = "azure-ai-search.api-version")
  String apiVersion;

  @Inject
  public CategoryIndexWriterService(@RestClient AzureSearchRestClient azureSearchRestClient) {
    super(azureSearchRestClient);
  }

  @Override
  protected String getId(IpaCategory item) {
    return item.getId();
  }

  /** Categories do not have an updateDate in IPA open data, so they are always re-indexed. */
  @Override
  protected String getUpdateDate(IpaCategory item) {
    return null;
  }

  @Override
  protected Function<IpaCategory, IpaCategoryIndex> toDocument() {
    return IpaCategoryIndex::fromCategory;
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
    return "Category";
  }
}
