package it.pagopa.selfcare.logavailability.client;

import com.azure.core.credential.TokenCredential;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableEntityUpdateMode;
import it.pagopa.selfcare.logavailability.model.DailyAvailabilityRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.ZoneOffset;
import java.time.Duration;
import java.util.Date;

@ApplicationScoped
public class AzureTableAvailabilityWriter {

    // Provisioned by Terraform; the job identity is authorized on this table only.
    private static final String TABLE_NAME = "SelcAvailability";

    private final TableClient tableClient;

    @Inject
    public AzureTableAvailabilityWriter(
            TokenCredential credential,
            @ConfigProperty(name = "availability.storage-account-name") String storageAccountName) {
        if (!storageAccountName.matches("[a-z0-9]{3,24}")) {
            throw new IllegalArgumentException("Storage account name is invalid");
        }
        tableClient = new TableClientBuilder()
                .endpoint("https://" + storageAccountName + ".table.core.windows.net")
                .tableName(TABLE_NAME)
                .credential(credential)
                .buildClient();
    }

    public void upsert(DailyAvailabilityRecord record) {
        TableEntity entity = new TableEntity(record.partitionKey(), record.rowKey())
                .addProperty("ReferenceDate", record.referenceDate().toString())
                .addProperty("Environment", record.environment())
                .addProperty("CountLt500", record.countLt500())
                .addProperty("CountGte500", record.countGte500())
                .addProperty("Total", record.total())
                .addProperty("Availability", record.availability().doubleValue())
                .addProperty("GenerationTimestamp",
                        Date.from(record.generationTimestamp().atOffset(ZoneOffset.UTC).toInstant()));
        tableClient.upsertEntityWithResponse(
                entity, TableEntityUpdateMode.REPLACE, Duration.ofSeconds(60), com.azure.core.util.Context.NONE);
    }
}
