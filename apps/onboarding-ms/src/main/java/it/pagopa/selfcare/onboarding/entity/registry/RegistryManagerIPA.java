package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.IPAEntity;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static it.pagopa.selfcare.onboarding.service.impl.OnboardingServiceDefault.GSP_CATEGORY_INSTITUTION_TYPE;
import static it.pagopa.selfcare.onboarding.service.impl.OnboardingServiceDefault.SCEC_CATEGORY_INSTITUTION_TYPE;

public class RegistryManagerIPA extends RegistryManagerIPAUo {

    private static final String DESCRIPTION_TO_REPLACE_REGEX = " - COMUNE";

    public RegistryManagerIPA(Onboarding onboarding, UoApi uoApi, InstitutionApi institutionApi, GeographicTaxonomiesApi geographicTaxonomiesApi) {
        super(onboarding, uoApi, institutionApi, geographicTaxonomiesApi);
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
        if (!originIPA(onboarding, registryResource.getInstitutionResource())) {
            return Uni.createFrom().failure(new InvalidRequestException(
                    String.format("Field digitalAddress or description are not valid for institution with taxCode=%s",
                            onboarding.getInstitution().getTaxCode())
            ));
        }
        return Uni.createFrom().item(true);
    }


    private boolean originIPA(Onboarding onboarding, InstitutionResource institutionResource) {
        return onboarding.getInstitution().getDigitalAddress().equalsIgnoreCase(institutionResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equalsIgnoreCase(institutionResource.getDescription());
    }

    @Override
    public RegistryManager<IPAEntity> setResource(IPAEntity registryResource) {
        this.registryResource = registryResource;
        if (onboarding.getInstitution().isImported()) {
            populateInstitution();
            populateBilling();
        }
        return this;
    }

    private void populateInstitution() {
        InstitutionResource institutionResource = registryResource.getInstitutionResource();
        onboarding.getInstitution().setOriginId(institutionResource.getOriginId());
        onboarding.getInstitution().setTaxCode(institutionResource.getTaxCode());
        onboarding.getInstitution().setDigitalAddress(institutionResource.getDigitalAddress());
        onboarding.getInstitution().setAddress(institutionResource.getAddress());
        onboarding.getInstitution().setDescription(institutionResource.getDescription());

        InstitutionType institutionType = getCurrentInstitutionType(institutionResource);
        onboarding.getInstitution().setInstitutionType(institutionType);

        onboarding.getInstitution().setZipCode(institutionResource.getZipCode());
        GeographicTaxonomyResource geographicTaxonomyResource = geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(institutionResource.getIstatCode())
                .await().atMost(Duration.of(DURATION_TIMEOUT, ChronoUnit.SECONDS));

        onboarding.getInstitution().setCounty(geographicTaxonomyResource.getProvinceAbbreviation());
        onboarding.getInstitution().setCountry(geographicTaxonomyResource.getCountryAbbreviation());
        onboarding.getInstitution().setCity(geographicTaxonomyResource.getDesc().replace(DESCRIPTION_TO_REPLACE_REGEX, ""));
    }

    private void populateBilling() {
        if (Objects.isNull(onboarding.getBilling()) || Objects.isNull(onboarding.getBilling().getRecipientCode())) {
            InstitutionResource institutionResource = registryResource.getInstitutionResource();
            Billing billing = new Billing();
            billing.setVatNumber(institutionResource.getTaxCode());
            billing.setRecipientCode(institutionResource.getOriginId());
            onboarding.setBilling(billing);
        }
    }

    private InstitutionType getCurrentInstitutionType(InstitutionResource institutionResource) {
        return switch (institutionResource.getCategory()) {
            case GSP_CATEGORY_INSTITUTION_TYPE -> InstitutionType.GSP;
            case SCEC_CATEGORY_INSTITUTION_TYPE -> InstitutionType.SCEC;
            default -> InstitutionType.PA;
        };
    }

}
