package it.pagopa.selfcare.logavailability.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityKqlQueryTest {

    private static final String APP_GATEWAY_ID =
            "/subscriptions/01234567-89ab-cdef-0123-456789abcdef/resourceGroups/selc-d-vnet-rg/providers/Microsoft.Network/applicationGateways/selc-d-app-gw";

    @Test
    void queriesOneFullUtcDayAndOnlyRequiredListeners() {
        String query = AvailabilityKqlQuery.forReferenceDate(
                LocalDate.parse("2026-10-07"), APP_GATEWAY_ID);

        assertTrue(query.contains("datetime(2026-10-07T00:00:00Z)"));
        assertTrue(query.contains("datetime(2026-10-08T00:00:00Z)"));
        assertTrue(query.contains("listenerName_s in (\"api\", \"api-pnpg\")"));
        assertTrue(query.contains("requestUri_s !in (\"/spid/v1/metadata\", \"dummy\")"));
    }

    @Test
    void countsOnlyValidStatusesBelow500AsAvailable() {
        String query = AvailabilityKqlQuery.forReferenceDate(
                LocalDate.parse("2026-10-07"), APP_GATEWAY_ID);

        assertTrue(query.contains("statusCode between (100 .. 599)"));
        assertTrue(query.contains("countif(validStatus and statusCode < 500)"));
        assertTrue(query.contains("countif(not(validStatus) or statusCode >= 500)"));
    }

    @Test
    void rejectsResourceIdsThatCouldInjectKql() {
        assertThrows(IllegalArgumentException.class, () ->
                AvailabilityKqlQuery.forReferenceDate(
                        LocalDate.parse("2026-10-07"), APP_GATEWAY_ID + "\" | union *"));
    }
}
