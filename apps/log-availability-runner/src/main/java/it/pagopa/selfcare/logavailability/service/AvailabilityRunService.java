package it.pagopa.selfcare.logavailability.service;

import com.fasterxml.jackson.databind.JsonNode;
import it.pagopa.selfcare.logavailability.client.AzureMonitorLogsClient;
import it.pagopa.selfcare.logavailability.client.AzureTableAvailabilityWriter;
import it.pagopa.selfcare.logavailability.client.LogAnalyticsAvailabilityWriter;
import it.pagopa.selfcare.logavailability.model.DailyAvailabilityRecord;
import it.pagopa.selfcare.logavailability.model.RequestCounts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.time.LocalDate;

@ApplicationScoped
public class AvailabilityRunService {

    @Inject
    AzureMonitorLogsClient logsClient;

    @Inject
    AzureTableAvailabilityWriter tableWriter;

    @Inject
    LogAnalyticsAvailabilityWriter reportingWriter;

    @ConfigProperty(name = "availability.environment")
    String environment;

    public void run(String selectedEnvironment, LocalDate referenceDate) {
        if (!environment.equals(selectedEnvironment)) {
            throw new IllegalArgumentException("Run environment must match the deployed environment");
        }
        RequestCounts counts;
        try {
            counts = logsClient.countRequests(referenceDate);
        } catch (RuntimeException exception) {
            throw new AvailabilityRunException(AvailabilityRunException.Stage.SOURCE_QUERY, exception);
        }

        DailyAvailabilityRecord record = DailyAvailabilityRecord.fromCounts(
                referenceDate, environment, counts.countLt500(), counts.countGte500(), Instant.now());
        try {
            tableWriter.upsert(record);
        } catch (RuntimeException exception) {
            throw new AvailabilityRunException(AvailabilityRunException.Stage.TABLE_STORAGE_WRITE, exception);
        }
        try {
            reportingWriter.ingest(record);
        } catch (RuntimeException exception) {
            throw new AvailabilityRunException(AvailabilityRunException.Stage.LOG_ANALYTICS_WRITE, exception);
        }
    }
}
