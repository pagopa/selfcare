package it.pagopa.selfcare.registry.proxy.runner.service;

import it.pagopa.selfcare.registry.proxy.runner.client.AzureSearchRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IvassInsuranceCompany;
import it.pagopa.selfcare.registry.proxy.runner.model.IvassInsuranceCompanyIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Function;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class InsuranceCompanyIndexWriterService extends AbstractIndexWriterService<IvassInsuranceCompany, IvassInsuranceCompanyIndex> {

    @ConfigProperty(name = "azure-ai-search.insurance-company.index-name")
    String indexName;

    @ConfigProperty(name = "azure-ai-search.api-version")
    String apiVersion;

    InsuranceCompanyIndexWriterService() {
        super(null);
    }

    @Inject
    public InsuranceCompanyIndexWriterService(@RestClient AzureSearchRestClient azureSearchRestClient) {
        super(azureSearchRestClient);
    }

    @Override
    protected String getId(IvassInsuranceCompany item) {
        return item.getId();
    }

    /**
     * Insurance companies do not have an updateDate in IVASS data,
     * so they are always re-indexed.
     */
    @Override
    protected String getUpdateDate(IvassInsuranceCompany item) {
        return null;
    }

    @Override
    protected Function<IvassInsuranceCompany, IvassInsuranceCompanyIndex> toDocument() {
        return IvassInsuranceCompanyIndex::fromInsuranceCompany;
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
        return "InsuranceCompany";
    }
}
