package it.pagopa.selfcare.onboarding.entity.registry.client;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManager;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;

public abstract class ClientRegistryPDNDInfocamere extends BaseRegistryManager<PDNDBusinessResource> {

  private final InfocamerePdndApi client;

  protected ClientRegistryPDNDInfocamere(
      Onboarding onboarding, InfocamerePdndApi infocamerePdndApi) {
    super(onboarding);
    this.client = infocamerePdndApi;
  }

  public PDNDBusinessResource retrieveInstitution() {
    return client
        .institutionPdndByTaxCodeUsingGET(onboarding.getInstitution().getTaxCode())
        .onFailure()
        .retry()
        .atMost(MAX_NUMBER_ATTEMPTS)
        .onFailure(WebApplicationException.class)
        .recoverWithUni(
            Uni.createFrom()
                .failure(
                    new ResourceNotFoundException(
                        String.format(
                            "Institution %s not found in the registry",
                            onboarding.getInstitution().getTaxCode()))))
        .await()
        .atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));
  }
}
