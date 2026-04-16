package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyUO;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.UOEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

class UOToDocumentConverterTest {

    private final Function<UO, Document> converter;


    UOToDocumentConverterTest() {
        this.converter = new UOToDocumentConverter();
    }


    @Test
    void apply_nullInput() {
        // given
        final UO input = null;
        // when
        final Document output = converter.apply(input);
        // then
        assertNull(output);
    }


    @Test
    void apply_FullNull() {
        // given
        final UO input = new DummyUO();
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getOrigin().toString(), output.get(Field.ORIGIN.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(24, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }


    @Test
    void apply_FullValued() {
        // given
        final UO input = new DummyUO();
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getOrigin().toString(), output.get(Field.ORIGIN.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(24, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }

    @Test
    void apply_EmptySfe() {
        // given
        final UOEntity input = mockInstance(new UOEntity(), "setCodiceFiscaleSfe");
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getOrigin().toString(), output.get(Field.ORIGIN.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(23, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }
}