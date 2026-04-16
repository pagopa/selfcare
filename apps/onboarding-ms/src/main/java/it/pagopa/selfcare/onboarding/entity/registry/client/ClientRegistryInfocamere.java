package it.pagopa.selfcare.onboarding.entity.registry.client;


import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManager;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.model.BusinessesResource;
import org.openapi.quarkus.party_registry_proxy_json.model.GetInstitutionsByLegalDto;
import org.openapi.quarkus.party_registry_proxy_json.model.GetInstitutionsByLegalFilterDto;

public abstract class ClientRegistryInfocamere extends BaseRegistryManager<BusinessesResource> {

  private final InfocamereApi client;
  private final String managerTaxCode;

  protected ClientRegistryInfocamere(Onboarding onboarding, InfocamereApi client, String managerTaxCode) {
    super(onboarding);
    this.client = client;
    this.managerTaxCode = managerTaxCode;
  }

  public BusinessesResource retrieveInstitution() {
    return client
        .institutionsByLegalTaxIdUsingPOST(
            GetInstitutionsByLegalDto.builder()
                .filter(
                    GetInstitutionsByLegalFilterDto.builder()
                        .legalTaxId(managerTaxCode)
                        .build())
                .build())
        .onFailure()
        .retry()
        .atMost(MAX_NUMBER_ATTEMPTS)
        .onFailure(WebApplicationException.class)
        .recoverWithUni(
            ex ->
                ((WebApplicationException) ex).getResponse().getStatus() == 404
                    ? Uni.createFrom()
                        .failure(
                            new ResourceNotFoundException(
                                String.format(
                                    "Institutions' list for user with taxCode %s not found",
                                        managerTaxCode)))
                    : Uni.createFrom().failure(ex))
        .await()
        .atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));
  }
}
