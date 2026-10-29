package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndProfile;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.JwtConfig;
import org.springframework.stereotype.Component;

/**
 * Selects a tenant's PDND subscription, preserving the product-specific Invitalia route.
 */
@Component
public class PdndSecretValueResolver {

    private final RegistryProxyTenantResources tenantResources;

    public PdndSecretValueResolver(RegistryProxyTenantResources tenantResources) {
        this.tenantResources = tenantResources;
    }

    public PdndSecretValue resolve(String productId) {
        var resources = tenantResources.current();
        boolean invitalia = productId != null && resources.invitaliaProducts().contains(productId);
        var definition = invitalia ? resources.pdndInvitalia() : resources.pdnd();
        if (definition == null) {
            throw new IllegalStateException("PDND profile is not configured for the current tenant");
        }
        String clientId = tenantResources.secret(definition.clientIdEnvVar());
        return PdndSecretValue.builder()
                .clientId(clientId)
                .secretKey(tenantResources.secret(definition.privateKeyEnvVar()))
                .profile(invitalia ? PdndProfile.INVITALIA : PdndProfile.SELFCARE)
                .jwtConfig(JwtConfig.builder()
                        .issuer(clientId).subject(clientId).audience(definition.audience())
                        .kid(tenantResources.secret(definition.kidEnvVar()))
                        .purposeId(tenantResources.secret(definition.purposeIdEnvVar()))
                        .build())
                .build();
    }
}
