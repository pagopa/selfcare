package it.pagopa.selfcare.onboarding.factory;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.inject.Inject;
import org.openapi.quarkus.user_registry_json.api.UserApi;

import java.util.Objects;
import java.util.UUID;


@ApplicationScoped
public class OnboardingResponseFactory {

    private static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";

    @Inject
    OnboardingMapper mapper;

    @Inject
    @RestClient
    UserApi userRegistryApi;

    public Uni<OnboardingGet> toGetResponse(Onboarding model) {
        OnboardingGet dto = mapper.toGetResponse(model);

        if (Objects.nonNull(model) && Objects.nonNull(model.getInstitution())
                && isUUID(model.getInstitution().getTaxCode())) {

            return userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, model.getInstitution().getTaxCode())
                    .onItem().transform(user -> {
                        dto.getInstitution().setTaxCode(user.getFiscalCode());
                        dto.getInstitution().setOriginId(user.getFiscalCode());
                        return dto;
                    })
                    .onFailure().transform(t -> t);
        }
        return Uni.createFrom().item(dto);
    }

    /**
     * Verifica se la stringa è un UUID valido.
     * Sul DB il taxCode delle persone fisiche viene salvato come UUID (token opaco di PDV),
     * quindi se è un UUID significa che va de-tokenizzato per recuperare il CF in chiaro.
     */
    static boolean isUUID(String value) {
        if (value == null || value.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
