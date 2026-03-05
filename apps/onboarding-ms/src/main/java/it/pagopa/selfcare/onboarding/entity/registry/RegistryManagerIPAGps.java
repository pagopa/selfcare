package it.pagopa.selfcare.onboarding.entity.registry;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.IPAEntity;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;

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
    public Uni<Onboarding> customValidation(Product product) {
        return super.customValidation(product)
                .onItem()
                .transformToUni(
                        onboarding -> {
                            if (PROD_PAGOPA.getValue().equals(onboarding.getProductId())
                                    && Objects.isNull(onboarding.getAdditionalInformations())) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        BaseRegistryManager.ADDITIONAL_INFORMATION_REQUIRED));
                            } else if (PROD_PAGOPA.getValue().equals(onboarding.getProductId())
                                    && !onboarding.getAdditionalInformations().isIpa()
                                    && !onboarding.getAdditionalInformations().isBelongRegulatedMarket()
                                    && !onboarding.getAdditionalInformations().isEstablishedByRegulatoryProvision()
                                    && !onboarding.getAdditionalInformations().isAgentOfPublicService()
                                    && Objects.isNull(onboarding.getAdditionalInformations().getOtherNote())) {
                                return Uni.createFrom()
                                        .failure(new InvalidRequestException(BaseRegistryManager.OTHER_NOTE_REQUIRED));
                            }
                            return Uni.createFrom().item(onboarding);
                        });
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }
}