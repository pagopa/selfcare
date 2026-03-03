package it.pagopa.selfcare.onboarding.entity.registry.client;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManager;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.StationsApi;
import org.openapi.quarkus.party_registry_proxy_json.model.StationResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static it.pagopa.selfcare.onboarding.constants.CustomError.STATION_NOT_FOUND;

public abstract class ClientRegistryANAC extends BaseRegistryManager<StationResource> {

    private final StationsApi client;

    protected ClientRegistryANAC(Onboarding onboarding, StationsApi stationsApi) {
        super(onboarding);
        client = stationsApi;
    }

    public StationResource retrieveInstitution() {
        return client.searchByTaxCodeUsingGET1(onboarding.getInstitution().getTaxCode())
                .onFailure().retry().atMost(MAX_NUMBER_ATTEMPTS)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(STATION_NOT_FOUND.getMessage(), onboarding.getInstitution().getTaxCode())))
                        : Uni.createFrom().failure(ex))
                .await().atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));
    }
}
