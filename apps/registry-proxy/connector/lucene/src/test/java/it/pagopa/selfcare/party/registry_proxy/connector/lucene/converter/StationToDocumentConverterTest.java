package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyStation;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.StationEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

class StationToDocumentConverterTest {

    private final Function<Station, Document> converter;

    StationToDocumentConverterTest() {
        this.converter = new StationToDocumentConverter();
    }

    @Test
    void apply_nullInput() {
        // given
        final Station input = null;
        // when
        final Document output = converter.apply(input);
        // then
        assertNull(output);
    }

    @Test
    void apply() {
        // given
        final Station input = new DummyStation();
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getId(), output.get(Field.ID.toString()));
        assertEquals(input.getTaxCode(), output.get(Field.TAX_CODE.toString()));
        assertEquals(input.getDescription(), output.get(Field.DESCRIPTION.toString()));
        assertEquals(input.getDigitalAddress(), output.get(Field.DIGITAL_ADDRESS.toString()));
        assertEquals(input.getAnacEnabled(), output.get(Field.ANAC_ENABLED.toString()));
        assertEquals(input.getAnacEngaged(), output.get(Field.ANAC_ENGAGED.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(1, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }

    @Test
    void apply_NullOptionalFields() {
        // given
        final Station input = mockInstance(new StationEntity());
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getId(), output.get(Field.ID.toString()));
        assertEquals(input.getTaxCode(), output.get(Field.TAX_CODE.toString()));
        assertEquals(input.getDescription(), output.get(Field.DESCRIPTION.toString()));
        assertEquals(input.getDigitalAddress(), output.get(Field.DIGITAL_ADDRESS.toString()));
        assertEquals(input.getAnacEnabled(), output.get(Field.ANAC_ENABLED.toString()));
        assertEquals(input.getAnacEngaged(), output.get(Field.ANAC_ENGAGED.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(1, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }

}