package it.pagopa.selfcare.onboarding.service.impl;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.RegistryProxyService;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

@ApplicationScoped
public class RegistryProxyServiceImpl implements RegistryProxyService {

    private final UoApi uoApi;

    public RegistryProxyServiceImpl(@RestClient UoApi uoApi) {
        this.uoApi = uoApi;
    }

    @Override
    public Uni<UOResource> findUoByRecipientCode(String recipientCode, String categories) {
        return uoApi.findByUnicodeUsingGET1(recipientCode, categories);
    }
}
