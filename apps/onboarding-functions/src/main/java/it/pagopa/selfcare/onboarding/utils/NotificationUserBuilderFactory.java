package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

import static it.pagopa.selfcare.onboarding.entity.Topic.*;

@ApplicationScoped
public class NotificationUserBuilderFactory {
    @Inject
    @RestClient
    InstitutionApi proxyRegistryInstitutionApi;
    @Inject
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;
    @Inject
    @RestClient
    org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;
    private final String alternativeEmail;
    @Inject
    @RestClient
    UoApi proxyRegistryUoApi;
    @Inject
    @RestClient
    AooApi proxyRegistryAooApi;

    public NotificationUserBuilderFactory(@ConfigProperty(name = "onboarding-functions.sender-mail") String alternativeEmail) {
        this.alternativeEmail = alternativeEmail;
    }

    public NotificationUserBuilder create(NotificationConfig.Consumer consumer) {

        NotificationUserBuilder notificationUserBuilder;
        if (SC_CONTRACTS_FD.getValue().equalsIgnoreCase(consumer.topic())) {
            notificationUserBuilder = new FdNotificationBuilder(this.alternativeEmail, consumer, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
        } else if (SC_CONTRACTS_SAP.getValue().equalsIgnoreCase(consumer.topic())) {
            notificationUserBuilder = new SapNotificationBuilder(this.alternativeEmail, consumer, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi, proxyRegistryUoApi, proxyRegistryAooApi);
        } else if (SC_CONTRACTS.getValue().equalsIgnoreCase(consumer.topic())) {
            notificationUserBuilder = new StandardNotificationBuilder(this.alternativeEmail, consumer, proxyRegistryInstitutionApi, geographicTaxonomiesApi, coreInstitutionApi);
        } else {
            throw new IllegalArgumentException("Topic not supported");
        }

        return notificationUserBuilder;
    }

}
