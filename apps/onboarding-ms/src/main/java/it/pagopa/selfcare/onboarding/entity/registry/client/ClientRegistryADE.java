package it.pagopa.selfcare.onboarding.entity.registry.client;


import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.BaseRegistryManager;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;

@Slf4j
public abstract class ClientRegistryADE extends BaseRegistryManager<Boolean> {

  private final NationalRegistriesApi client;
  private final String managerTaxCode;

  protected ClientRegistryADE(Onboarding onboarding, NationalRegistriesApi client, String managerTaxCode) {
    super(onboarding);
    this.client = client;
    this.managerTaxCode = managerTaxCode;
  }

  public Boolean retrieveInstitution() {
    if (Boolean.TRUE.equals(onboarding.getSkipVerifyLegal())) {
      log.info("ClientRegistryADE :: verifyLegal is skipped");
      return true;
    }
    return client
            .verifyLegalUsingGET(managerTaxCode, onboarding.getInstitution().getTaxCode())
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
                                                            "Institution with taxCode %s not found",
                                                            onboarding.getInstitution().getTaxCode())))
                                    : Uni.createFrom().failure(ex))
            .await()
            .atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS))
            .getVerificationResult();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!super.equals(o)) return false;
    if (getClass() != o.getClass()) return false;
    ClientRegistryADE that = (ClientRegistryADE) o;
    return Objects.equals(client, that.client)
            && Objects.equals(managerTaxCode, that.managerTaxCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), client, managerTaxCode);
  }
}
