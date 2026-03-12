package it.pagopa.selfcare.onboarding.entity.registry.client;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManager;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InsuranceCompaniesApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InsuranceCompanyResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static it.pagopa.selfcare.onboarding.constants.CustomError.INSURANCE_NOT_FOUND;

public abstract class ClientRegistryIVASS extends BaseRegistryManager<InsuranceCompanyResource> {

    private final InsuranceCompaniesApi client;

    protected ClientRegistryIVASS(Onboarding onboarding, InsuranceCompaniesApi client) {
        super(onboarding);
        this.client = client;
    }

    public InsuranceCompanyResource retrieveInstitution() {
        return client.searchByTaxCodeUsingGET(onboarding.getInstitution().getOriginId())
                .onFailure().retry().atMost(MAX_NUMBER_ATTEMPTS)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(INSURANCE_NOT_FOUND.getMessage(), onboarding.getInstitution().getOriginId())))
                        : Uni.createFrom().failure(ex))
                .await().atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));
    }
}