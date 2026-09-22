package it.pagopa.selfcare.commons.health.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractBlobStorageReadinessIndicatorTest {

    @Test
    void exposesBlobStorageMetadata() {
        AbstractBlobStorageReadinessIndicator indicator =
                new AbstractBlobStorageReadinessIndicator() {
                    @Override
                    protected String checkName() {
                        return "blob-storage-product";
                    }

                    @Override
                    protected String account() {
                        return "storage-account";
                    }

                    @Override
                    protected String container() {
                        return "products";
                    }

                    @Override
                    protected String probeTarget() {
                        return "products.json";
                    }

                    @Override
                    protected void probe() {
                    }
                };

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("component", "blob-storage")
                .containsEntry("account", "storage-account")
                .containsEntry("container", "products")
                .containsEntry("probeTarget", "products.json");
    }

    @Test
    void omitsBlankProbeTarget() {
        AbstractBlobStorageReadinessIndicator indicator =
                new AbstractBlobStorageReadinessIndicator() {
                    @Override
                    protected String checkName() {
                        return "blob-storage-container";
                    }

                    @Override
                    protected String account() {
                        return "storage-account";
                    }

                    @Override
                    protected String container() {
                        return "documents";
                    }

                    @Override
                    protected void probe() {
                    }
                };

        assertThat(indicator.health().getDetails()).doesNotContainKey("probeTarget");
    }
}
