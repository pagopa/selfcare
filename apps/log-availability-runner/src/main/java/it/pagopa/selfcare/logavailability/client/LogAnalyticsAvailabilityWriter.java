package it.pagopa.selfcare.logavailability.client;

import it.pagopa.selfcare.logavailability.model.DailyAvailabilityRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class LogAnalyticsAvailabilityWriter {

    private static final String INGESTION_SCOPE = "https://monitor.azure.com/.default";
    private static final String STREAM_NAME = "Custom-SelcAvailability_CL";

    private final AzureMonitorHttpClient httpClient;
    private final URI ingestionEndpoint;
    private final String immutableRuleId;

    @Inject
    public LogAnalyticsAvailabilityWriter(
            AzureMonitorHttpClient httpClient,
            @ConfigProperty(name = "availability.logs-ingestion-endpoint") String ingestionEndpoint,
            @ConfigProperty(name = "availability.dcr-immutable-id") String immutableRuleId) {
        this.httpClient = httpClient;
        String normalizedEndpoint = ingestionEndpoint.endsWith("/")
                ? ingestionEndpoint.substring(0, ingestionEndpoint.length() - 1)
                : ingestionEndpoint;
        this.ingestionEndpoint = URI.create(normalizedEndpoint);
        this.immutableRuleId = immutableRuleId;
    }

    public void ingest(DailyAvailabilityRecord record) {
        String endpoint = ingestionEndpoint
                + "/dataCollectionRules/" + immutableRuleId
                + "/streams/" + STREAM_NAME
                + "?api-version=2023-01-01";
        Map<String, Object> row = Map.of(
                "TimeGenerated", record.generationTimestamp().toString(),
                "ReferenceDate", record.referenceDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toString(),
                "Environment", record.environment(),
                "CountLt500", record.countLt500(),
                "CountGte500", record.countGte500(),
                "Total", record.total(),
                "Availability", record.availability(),
                "GenerationTimestamp", record.generationTimestamp().toString());
        httpClient.post(URI.create(endpoint), INGESTION_SCOPE, List.of(row));
    }
}
