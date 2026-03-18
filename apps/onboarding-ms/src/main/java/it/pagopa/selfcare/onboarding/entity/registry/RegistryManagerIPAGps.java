package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.IPAEntity;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class RegistryManagerIPAGps extends RegistryManagerIPAUo {

    public RegistryManagerIPAGps(Onboarding onboarding, UoApi uoApi, InstitutionApi institutionApi) {
        super(onboarding, uoApi, institutionApi, null);
    }

    @Override
    public IPAEntity retrieveInstitution() {
        super.originIdEC = onboarding.getInstitution().getOriginId();
        InstitutionResource institutionResource = super.institutionApi.findInstitutionUsingGET(onboarding.getInstitution().getTaxCode(), null, null)
                .onFailure().retry().atMost(MAX_NUMBER_ATTEMPTS)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format("Institution with taxCode %s not found", onboarding.getInstitution().getTaxCode())))
                        : Uni.createFrom().failure(ex))
                .await().atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));
        return IPAEntity.builder().institutionResource(institutionResource).build();
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }
}