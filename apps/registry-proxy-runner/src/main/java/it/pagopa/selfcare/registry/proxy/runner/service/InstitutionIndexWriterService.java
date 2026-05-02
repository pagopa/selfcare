package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitutionIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class InstitutionIndexWriterService
    extends AbstractIndexWriterService<IpaInstitution, IpaInstitutionIndex> {

  @ConfigProperty(name = "azure-ai-search.institution.index-name")
  String indexName;

  @ConfigProperty(name = "azure-ai-search.api-version")
  String apiVersion;

  @Inject
  public InstitutionIndexWriterService(@RestClient AzureSearchRestClient azureSearchRestClient) {
    super(azureSearchRestClient);
  }

  @Override
  protected String getId(IpaInstitution item) {
    return item.getId();
  }

  @Override
  protected String getUpdateDate(IpaInstitution item) {
    return item.getUpdateDate();
  }

  @Override
  protected Function<IpaInstitution, IpaInstitutionIndex> toDocument() {
    return IpaInstitutionIndex::fromInstitution;
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
    return "Institution";
  }
}
