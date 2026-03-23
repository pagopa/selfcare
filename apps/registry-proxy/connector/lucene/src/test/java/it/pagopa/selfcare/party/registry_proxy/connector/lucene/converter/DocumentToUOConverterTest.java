package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyUO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static org.junit.jupiter.api.Assertions.*;

class DocumentToUOConverterTest {

    private final Function<Document, UO> converter;


    DocumentToUOConverterTest() {
        this.converter = new DocumentToUOConverter();
    }


    @Test
    void apply_nullInput() {
        // given
        final Document input = null;
        // when
        final UO output = converter.apply(input);
        // then
        assertNull(output);
    }


    @Test
    void apply() {
        // given
        final Document input = new DummyUO().toDocument();
        // when
        final UO output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.get(Field.ORIGIN.toString()), output.getOrigin().toString());
        checkNotNullFields(output, Arrays.stream(Field.values())
                .map(Field::toString)
                .toArray(String[]::new));
    }

}