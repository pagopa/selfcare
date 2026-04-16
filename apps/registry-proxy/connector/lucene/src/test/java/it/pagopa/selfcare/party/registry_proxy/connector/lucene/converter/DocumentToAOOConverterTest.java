package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyAOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static org.junit.jupiter.api.Assertions.*;

class DocumentToAOOConverterTest {

    private final Function<Document, AOO> converter;


    DocumentToAOOConverterTest() {
        this.converter = new DocumentToAOOConverter();
    }


    @Test
    void apply_nullInput() {
        // given
        final Document input = null;
        // when
        final AOO output = converter.apply(input);
        // then
        assertNull(output);
    }


    @Test
    void apply() {
        // given
        final Document input = new DummyAOO().toDocument();
        // when
        final AOO output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.get(Field.ORIGIN.toString()), output.getOrigin().toString());
        checkNotNullFields(output, Arrays.stream(Field.values())
                .map(Field::toString)
                .toArray(String[]::new));
    }

}