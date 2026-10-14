package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitutionIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class InstitutionIndexWriterService
    extends AbstractIndexWriterService<IpaInstitution, IpaInstitutionIndex> {

  static final String COMPARED_FIELDS =
      "id,updateDate,originId,taxCode,description,category,digitalAddress,address,zipCode,istatCode,origin";

  @ConfigProperty(name = "azure-ai-search.institution.index-name")
  String indexName;

  @ConfigProperty(name = "azure-ai-search.api-version")
  String apiVersion;

  InstitutionIndexWriterService() {
    super(null);
  }

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

  /**
   * IPA can change institution data (e.g. Codice_comune_ISTAT after the 2025 Sardinian provinces
   * reorganization) without updating Data_aggiornamento: compare the indexed content too.
   */
  @Override
  protected String getSelectFields() {
    return COMPARED_FIELDS;
  }

  @Override
  protected boolean hasContentChanged(IpaInstitutionIndex indexed, IpaInstitution item) {
    IpaInstitutionIndex expected = IpaInstitutionIndex.fromInstitution(item);
    return differs(indexed.getOriginId(), expected.getOriginId())
        || differs(indexed.getTaxCode(), expected.getTaxCode())
        || differs(indexed.getDescription(), expected.getDescription())
        || differs(indexed.getCategory(), expected.getCategory())
        || differs(indexed.getDigitalAddress(), expected.getDigitalAddress())
        || differs(indexed.getAddress(), expected.getAddress())
        || differs(indexed.getZipCode(), expected.getZipCode())
        || differs(indexed.getIstatCode(), expected.getIstatCode())
        || differs(indexed.getOrigin(), expected.getOrigin());
  }

  /** null and empty string are considered equal, to avoid useless re-indexing. */
  private static boolean differs(String indexed, String source) {
    return !Objects.equals(normalize(indexed), normalize(source));
  }

  private static String normalize(String value) {
    return value == null || value.isEmpty() ? null : value;
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
