package it.pagopa.selfcare.document.storage;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With {@code tenant.storage.eager-init=true} the clients of every supported tenant and mandatory key must exist as
 * soon as the application is started, before any request is served. Building a client never contacts Azure.
 */
@QuarkusTest
@TestProfile(TenantBlobClientProviderStartupTest.EagerInitProfile.class)
class TenantBlobClientProviderStartupTest {

    @Inject
    TenantBlobClientProvider provider;

    @Test
    void startup_shouldBuildTheClientsOfEveryMandatoryBindingWithoutAnyRequestAndReuseThem() throws Exception {
        assertThat(cachedClients(provider)).hasSize(2);

        provider.clientFor("AR", StorageKeys.CONTRACTS);
        provider.clientFor("AR", StorageKeys.USER_ATTACHMENTS);

        assertThat(cachedClients(provider)).hasSize(2);
    }

    static Map<?, ?> cachedClients(TenantBlobClientProvider provider) throws Exception {
        Field clients = TenantBlobClientProvider.class.getDeclaredField("clients");
        clients.setAccessible(true);
        return (Map<?, ?>) clients.get(ClientProxy.unwrap(provider));
    }

    public static class EagerInitProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("tenant.storage.eager-init", "true");
        }
    }
}
