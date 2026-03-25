package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyAOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AOOToDocumentConverterTest {

    private final Function<AOO, Document> converter;


    AOOToDocumentConverterTest() {
        this.converter = new AOOToDocumentConverter();
    }


    @Test
    void apply_nullInput() {
        // given
        final AOO input = null;
        // when
        final Document output = converter.apply(input);
        // then
        assertNull(output);
    }


    @Test
    void apply_FullNull() {
        // given
        final AOO input = new DummyAOO();
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


    @Test
    void apply_FullValued() {
        // given
        final AOO input = new DummyAOO();
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