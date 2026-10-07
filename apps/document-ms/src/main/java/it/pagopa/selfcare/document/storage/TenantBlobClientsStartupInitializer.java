package it.pagopa.selfcare.document.storage;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Builds the tenant blob clients at startup when {@code tenant.storage.eager-init} is enabled (fail-closed):
 * TenantBlobClientProvider is a lazy bean, so without this a misconfigured binding would surface only on the first
 * request, and the Azure client bootstrap would run on the request thread. The observer is not declared on the provider
 * itself: a StartupEvent observer there made the test subclasses of the provider ambiguous beans.
 */
@ApplicationScoped
public class TenantBlobClientsStartupInitializer {

    void onStart(@Observes StartupEvent event, TenantBlobClientProvider provider) {
        provider.initialize();
    }
}
