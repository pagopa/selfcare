package it.pagopa.selfcare.onboarding.entity.registry;

import static it.pagopa.selfcare.onboarding.constants.CustomError.AOO_NOT_FOUND;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.IPAEntity;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;

public class RegistryManagerIPAAoo extends RegistryManagerIPAUo {

    public RegistryManagerIPAAoo(Onboarding onboarding, UoApi uoApi, AooApi aooApi) {
        super(onboarding, uoApi, aooApi);
    }

    @Override
    public IPAEntity retrieveInstitution() {
        AOOResource aooResource = super.aooClient.findByUnicodeUsingGET(onboarding.getInstitution().getSubunitCode(), null)
                .onFailure().retry().atMost(MAX_NUMBER_ATTEMPTS)
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(String.format(AOO_NOT_FOUND.getMessage(), onboarding.getInstitution().getSubunitCode())))
                        : Uni.createFrom().failure(ex))
                .onItem().invoke(this::enrichOnboardingData)
                .await().atMost(Duration.of(BaseRegistryManager.DURATION_TIMEOUT, ChronoUnit.SECONDS));
        super.originIdEC = aooResource.getCodiceIpa();
        super.resourceTaxCode = aooResource.getCodiceFiscaleEnte();
        return IPAEntity.builder().aooResource(aooResource).build();
    }

    private void enrichOnboardingData(AOOResource aooResource) {
        onboarding.getInstitution().setParentDescription(aooResource.getDenominazioneEnte());
        onboarding.getInstitution().setIstatCode(aooResource.getCodiceComuneISTAT());
    }

    @Override
    public Uni<Boolean> isValid() {
        if (!originIPA(onboarding, registryResource.getAooResource())) {
            return Uni.createFrom().failure(new InvalidRequestException(
                    String.format("Field digitalAddress or description are not valid for institution with taxCode=%s and subunitCode=%s",
                            onboarding.getInstitution().getTaxCode(),
                            onboarding.getInstitution().getSubunitCode())
            ));
        }
        return Uni.createFrom().item(true);
    }


    private boolean originIPA(Onboarding onboarding, AOOResource aooResource) {
        return onboarding.getInstitution().getDigitalAddress().equalsIgnoreCase(aooResource.getMail1()) &&
                onboarding.getInstitution().getDescription().equalsIgnoreCase(aooResource.getDenominazioneAoo());
    }

}