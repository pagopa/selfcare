package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyCategory;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static org.junit.jupiter.api.Assertions.*;

class DocumentToCategoryConverterTest {

    private final Function<Document, Category> converter;


    DocumentToCategoryConverterTest() {
        this.converter = new DocumentToCategoryConverter();
    }


    @Test
    void apply_nullInput() {
        // given
        final Document input = null;
        // when
        final Category output = converter.apply(input);
        // then
        assertNull(output);
    }


    @Test
    void apply() {
        // given
        final Document input = new DummyCategory().toDocument();
        // when
        final Category output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.get(Field.ID.toString()), output.getId());
        assertEquals(input.get(Field.CODE.toString()), output.getCode());
        assertEquals(input.get(Field.NAME.toString()), output.getName());
        assertEquals(input.get(Field.KIND.toString()), output.getKind());
        assertEquals(input.get(Field.ORIGIN.toString()), output.getOrigin().toString());
        checkNotNullFields(output, Arrays.stream(Field.values())
                .map(Field::toString)
                .toArray(String[]::new));
    }

}