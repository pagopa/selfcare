package it.pagopa.selfcare.logavailability.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.logavailability.service.AvailabilityKqlQuery;
import it.pagopa.selfcare.logavailability.model.RequestCounts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AzureMonitorLogsClient {

    private static final String LOGS_QUERY_SCOPE = "https://api.loganalytics.io/.default";

    private final AzureMonitorHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String workspaceId;
    private final String applicationGatewayResourceId;

    @Inject
    public AzureMonitorLogsClient(
            AzureMonitorHttpClient httpClient,
            ObjectMapper objectMapper,
            @ConfigProperty(name = "availability.log-analytics-workspace-id") String workspaceId,
            @ConfigProperty(name = "availability.application-gateway-resource-id") String applicationGatewayResourceId) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.workspaceId = UUID.fromString(workspaceId).toString();
        this.applicationGatewayResourceId = applicationGatewayResourceId;
    }

    public RequestCounts countRequests(LocalDate referenceDate) {
        String start = referenceDate.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String end = referenceDate.plusDays(1).atStartOfDay(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", AvailabilityKqlQuery.forReferenceDate(referenceDate, applicationGatewayResourceId));
        payload.put("timespan", start + "/" + end);

        String response = httpClient.post(
                URI.create("https://api.loganalytics.azure.com/v1/workspaces/" + workspaceId + "/query"),
                LOGS_QUERY_SCOPE,
                payload);
        try {
            JsonNode primaryResult = objectMapper.readTree(response)
                    .path("tables").path(0);
            JsonNode columns = primaryResult.path("columns");
            JsonNode rows = primaryResult.path("rows");
            if (!columns.isArray() || !rows.isArray() || rows.size() != 1) {
                throw new IllegalStateException("Azure Monitor query returned an unexpected result shape");
            }

            Map<String, Integer> columnIndexes = new HashMap<>();
            for (int index = 0; index < columns.size(); index++) {
                columnIndexes.put(columns.get(index).path("name").asText(), index);
            }
            long countLt500 = readNonNegativeLong(rows.get(0), columnIndexes, "CountLt500");
            long countGte500 = readNonNegativeLong(rows.get(0), columnIndexes, "CountGte500");
            return new RequestCounts(countLt500, countGte500);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse Azure Monitor query response", exception);
        }
    }

    private static long readNonNegativeLong(JsonNode row, Map<String, Integer> columns, String columnName) {
        Integer index = columns.get(columnName);
        if (index == null || index >= row.size() || !row.get(index).isIntegralNumber()) {
            throw new IllegalStateException("Azure Monitor query response is missing " + columnName);
        }
        long value = row.get(index).longValue();
        if (value < 0) {
            throw new IllegalStateException("Azure Monitor returned a negative request count");
        }
        return value;
    }
}
