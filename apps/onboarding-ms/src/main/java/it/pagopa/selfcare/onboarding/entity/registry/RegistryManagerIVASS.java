package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryIVASS;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.InsuranceCompaniesApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InsuranceCompanyResource;

public class RegistryManagerIVASS extends ClientRegistryIVASS {

    public RegistryManagerIVASS(Onboarding onboarding, InsuranceCompaniesApi insuranceApi) {
        super(onboarding, insuranceApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return Uni.createFrom().item(onboarding);
    }

    @Override
    public Uni<Boolean> isValid() {
        if (!originIVASS(onboarding, registryResource)) {
            return Uni.createFrom().failure(new InvalidRequestException("Field digitalAddress or description are not valid"));
        }
        return Uni.createFrom().item(true);
    }

    private boolean originIVASS(Onboarding onboarding, InsuranceCompanyResource insuranceCompanyResource) {
        return onboarding.getInstitution().getDigitalAddress().equals(insuranceCompanyResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equals(insuranceCompanyResource.getDescription());
    }

}