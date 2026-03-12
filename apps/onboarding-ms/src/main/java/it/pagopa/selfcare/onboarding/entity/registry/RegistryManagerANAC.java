package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryANAC;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.openapi.quarkus.party_registry_proxy_json.api.StationsApi;
import org.openapi.quarkus.party_registry_proxy_json.model.StationResource;

public class RegistryManagerANAC extends ClientRegistryANAC {

    public RegistryManagerANAC(Onboarding onboarding, StationsApi stationsApi) {
        super(onboarding, stationsApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return Uni.createFrom().item(onboarding);
    }

    @Override
    public Uni<Boolean> isValid() {
        if (!originANAC(onboarding, registryResource)) {
            return Uni.createFrom().failure(new InvalidRequestException("Field digitalAddress or description are not valid"));
        }
        return Uni.createFrom().item(true);
    }

    private boolean originANAC(Onboarding onboarding, StationResource stationResource) {
        return onboarding.getInstitution().getDigitalAddress().equals(stationResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equals(stationResource.getDescription());
    }

}