package it.pagopa.selfcare.logavailability.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class AvailabilityKqlQuery {

    private static final Pattern APPLICATION_GATEWAY_RESOURCE_ID = Pattern.compile(
            "^/subscriptions/[0-9a-fA-F-]{36}/resourceGroups/[A-Za-z0-9._()-]+/providers/Microsoft\\.Network/applicationGateways/[A-Za-z0-9._()-]+$");

    private AvailabilityKqlQuery() {
    }

    public static String forReferenceDate(LocalDate referenceDate, String applicationGatewayResourceId) {
        if (referenceDate == null || applicationGatewayResourceId == null
                || !APPLICATION_GATEWAY_RESOURCE_ID.matcher(applicationGatewayResourceId).matches()) {
            throw new IllegalArgumentException("A valid reference date and Application Gateway resource ID are required");
        }
        String start = referenceDate.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String end = referenceDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        return """
                let startTime = datetime(%s);
                let endTime = datetime(%s);
                AzureDiagnostics
                | where TimeGenerated >= startTime and TimeGenerated < endTime
                | where ResourceProvider == "MICROSOFT.NETWORK"
                    and Category == "ApplicationGatewayAccessLog"
                    and _ResourceId =~ "%s"
                | where listenerName_s in ("api", "api-pnpg")
                | where isnull(requestUri_s) or requestUri_s !in ("/spid/v1/metadata", "dummy")
                | extend statusCode = toint(httpStatus_d)
                | extend validStatus = isnotnull(httpStatus_d)
                    and isnotnull(statusCode)
                    and todouble(statusCode) == httpStatus_d
                    and statusCode between (100 .. 599)
                | summarize
                    CountLt500 = countif(validStatus and statusCode < 500),
                    CountGte500 = countif(not(validStatus) or statusCode >= 500)
                """.formatted(start, end, applicationGatewayResourceId);
    }
}
