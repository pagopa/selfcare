package it.pagopa.selfcare.logavailability.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class AzureIdentityProducer {

    @Produces
    @ApplicationScoped
    TokenCredential managedIdentityCredential() {
        return new ManagedIdentityCredentialBuilder().build();
    }
}
