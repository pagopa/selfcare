package it.pagopa.selfcare.delegation.event.health;

import com.azure.data.tables.TableClient;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.commons.health.AbstractAsyncReadinessCheck;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;

import java.util.Map;
import java.util.Optional;

@Readiness
@ApplicationScoped
public class DelegationCdcTableStorageReadinessCheck extends AbstractAsyncReadinessCheck {

    private static final String ACCOUNT_NOT_APPLICABLE = "n/a";

    private final TableClient tableClient;
    private final String tableName;
    private final String account;

    @Inject
    public DelegationCdcTableStorageReadinessCheck(
            TableClient tableClient,
            @ConfigProperty(name = "delegation-cdc.table.name") String tableName,
            @ConfigProperty(name = "delegation-cdc.storage-account-name") Optional<String> account) {
        this.tableClient = tableClient;
        this.tableName = tableName;
        this.account = account.filter(s -> !s.isBlank()).orElse(ACCOUNT_NOT_APPLICABLE);
    }

    @Override
    protected String checkName() {
        return "table-storage-delegation-cdc";
    }

    @Override
    protected Map<String, String> data() {
        return Map.of(
                "component", "table-storage",
                "account", account,
                "table", tableName);
    }

    @Override
    protected Uni<?> probe() {
        return Uni.createFrom()
                .item(() -> tableClient.listEntities().iterator().hasNext())
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
