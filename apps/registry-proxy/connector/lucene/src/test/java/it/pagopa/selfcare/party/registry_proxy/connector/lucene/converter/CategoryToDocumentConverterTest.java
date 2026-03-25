package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyCategory;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CategoryToDocumentConverterTest {

    private final Function<Category, Document> converter;


    CategoryToDocumentConverterTest() {
        this.converter = new CategoryToDocumentConverter();
    }


    @Test
    void apply_nullInput() {
        // given
        final Category input = null;
        // when
        final Document output = converter.apply(input);
        // then
        assertNull(output);
    }


    @Test
    void apply_FullNull() {
        // given
        final Category input = new DummyCategory();
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getId(), output.get(Field.ID.toString()));
        assertEquals(input.getCode(), output.get(Field.CODE.toString()));
        assertEquals(input.getName(), output.get(Field.NAME.toString()));
        assertEquals(input.getKind(), output.get(Field.KIND.toString()));
        assertEquals(input.getOrigin().toString(), output.get(Field.ORIGIN.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(1, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }


    @Test
    void apply_FullValued() {
        // given
        final Category input = new DummyCategory();
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getId(), output.get(Field.ID.toString()));
        assertEquals(input.getCode(), output.get(Field.CODE.toString()));
        assertEquals(input.getName(), output.get(Field.NAME.toString()));
        assertEquals(input.getKind(), output.get(Field.KIND.toString()));
        assertEquals(input.getOrigin().toString(), output.get(Field.ORIGIN.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(1, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }

}