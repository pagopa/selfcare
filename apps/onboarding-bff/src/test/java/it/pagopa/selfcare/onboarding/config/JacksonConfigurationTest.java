package it.pagopa.selfcare.onboarding.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResource;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JacksonConfigurationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        new JacksonConfiguration().customize(objectMapper);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-10-08T17:00:07+02:00, 2026-10-08T17:00:07+02:00",
            "2026-10-08T15:00:07Z, 2026-10-08T15:00:07Z",
            "2026-10-08T15:00:07, 2026-10-08T15:00:07Z",
            "2026-10-08T15:00:07.123456789, 2026-10-08T15:00:07.123456789Z"
    })
    void downstreamDatesKeepTheirOffsetOrUseUtcWhenItIsAbsent(String value, String expected) throws Exception {
        OffsetDateTime result = objectMapper.readValue(objectMapper.writeValueAsString(value), OffsetDateTime.class);

        assertEquals(OffsetDateTime.parse(expected), result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void absentOrBlankDownstreamDatesRemainNull(String value) throws Exception {
        assertNull(objectMapper.readValue(objectMapper.writeValueAsString(value), OffsetDateTime.class));
    }

    @Test
    void malformedDownstreamDatesFailInsteadOfBecomingNull() {
        InvalidFormatException failure = assertThrows(InvalidFormatException.class,
                () -> objectMapper.readValue("\"not-a-date\"", OffsetDateTime.class));

        assertTrue(failure.getMessage().contains("Unable to parse OffsetDateTime with or without timezone offset"));
    }

    @Test
    void serializationOmitsNullFieldsAndWritesIsoDatesInsteadOfTimestamps() throws Exception {
        OnboardingResource resource = new OnboardingResource();
        resource.setProductId("prod-io");
        resource.setCreatedAt(OffsetDateTime.parse("2026-10-08T17:00:07+02:00"));

        assertEquals(objectMapper.readTree("""
                {"productId":"prod-io","createdAt":"2026-10-08T17:00:07+02:00"}
                """), objectMapper.readTree(objectMapper.writeValueAsString(resource)));
    }

    @Test
    void downstreamModelsAcceptUnknownFieldsAndLocalDates() throws Exception {
        OnboardingResource resource = objectMapper.readValue("""
                {"productId":"prod-io","createdAt":"2026-10-08T15:00:07","unknownField":"ignored"}
                """, OnboardingResource.class);

        assertEquals("prod-io", resource.getProductId());
        assertEquals(OffsetDateTime.parse("2026-10-08T15:00:07Z"), resource.getCreatedAt());
        assertNull(resource.getUpdatedAt());
        assertNull(resource.getClosedAt());
    }
}
