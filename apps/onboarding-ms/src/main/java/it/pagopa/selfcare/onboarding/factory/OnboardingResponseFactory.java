package it.pagopa.selfcare.onboarding.factory;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.inject.Inject;
import org.openapi.quarkus.user_registry_json.api.UserApi;

import java.util.Objects;


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
                && InstitutionType.PRV_PF.equals(model.getInstitution().getInstitutionType())) {

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
}
