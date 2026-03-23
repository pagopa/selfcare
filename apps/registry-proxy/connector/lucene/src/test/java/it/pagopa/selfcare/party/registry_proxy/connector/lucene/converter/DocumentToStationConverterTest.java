package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station.Field;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static org.junit.jupiter.api.Assertions.*;

class DocumentToStationConverterTest {

    private final Function<Document, Station> converter;

    DocumentToStationConverterTest() {
        this.converter = new DocumentToStationConverter();
    }

    @Test
    void apply_nullInput() {
        // given
        final Document input = null;
        // when
        final Station output = converter.apply(input);
        // then
        assertNull(output);
    }

    @Test
    void apply() {
        // given
        final Document input = new DummyInstitution().toDocument();
        // when
        final Station output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.get(Field.ID.toString()), output.getId());
        assertEquals(input.get(Field.TAX_CODE.toString()), output.getTaxCode());
        assertEquals(input.get(Field.DESCRIPTION.toString()), output.getDescription());
        assertEquals(input.get(Field.DIGITAL_ADDRESS.toString()), output.getDigitalAddress());
        checkNotNullFields(output, Arrays.stream(Field.values())
                .map(Field::toString)
                .toArray(String[]::new));
    }

}