package it.pagopa.selfcare.document.storage;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With {@code tenant.storage.eager-init=false} (the {@code %test} default) startup must not build any client: they are
 * created on first use. A dedicated profile gives a fresh application that no other test has used.
 */
@QuarkusTest
@TestProfile(TenantBlobClientProviderLazyStartupTest.LazyInitProfile.class)
class TenantBlobClientProviderLazyStartupTest {

    @Inject
    TenantBlobClientProvider provider;

    @Test
    void startup_shouldNotBuildAnyClientWhenEagerInitIsDisabledAndClientsAreBuiltOnFirstUse() throws Exception {
        assertThat(TenantBlobClientProviderStartupTest.cachedClients(provider)).isEmpty();

        provider.clientFor("AR", StorageKeys.CONTRACTS);

        assertThat(TenantBlobClientProviderStartupTest.cachedClients(provider)).hasSize(1);
    }

    public static class LazyInitProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("tenant.storage.eager-init", "false");
        }
    }
}
