package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

public interface RegistryProxyService {
    Uni<UOResource> findUoByRecipientCode(String recipientCode, String categories);
}
