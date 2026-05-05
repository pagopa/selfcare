package it.pagopa.selfcare.onboarding.service.integration;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

public interface ProxyRegistryService {
    Uni<UOResource> findUoByRecipientCode(String recipientCode);
}
