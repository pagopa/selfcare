package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static org.junit.jupiter.api.Assertions.*;

class DocumentToInstitutionConverterTest {

    private final Function<Document, Institution> converter;


    DocumentToInstitutionConverterTest() {
        this.converter = new DocumentToInstitutionConverter();
    }


    @Test
    void apply_nullInput() {
        // given
        final Document input = null;
        // when
        final Institution output = converter.apply(input);
        // then
        assertNull(output);
    }


    @Test
    void apply() {
        // given
        final Document input = new DummyInstitution().toDocument();
        // when
        final Institution output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.get(Field.ID.toString()), output.getId());
        assertEquals(input.get(Field.ORIGIN_ID.toString()), output.getOriginId());
        assertEquals(input.get(Field.O.toString()), output.getO());
        assertEquals(input.get(Field.OU.toString()), output.getOu());
        assertEquals(input.get(Field.AOO.toString()), output.getAoo());
        assertEquals(input.get(Field.TAX_CODE.toString()), output.getTaxCode());
        assertEquals(input.get(Field.CATEGORY.toString()), output.getCategory());
        assertEquals(input.get(Field.DESCRIPTION.toString()), output.getDescription());
        assertEquals(input.get(Field.DIGITAL_ADDRESS.toString()), output.getDigitalAddress());
        assertEquals(input.get(Field.ADDRESS.toString()), output.getAddress());
        assertEquals(input.get(Field.ZIP_CODE.toString()), output.getZipCode());
        assertEquals(input.get(Field.ORIGIN.toString()), output.getOrigin().toString());
        assertEquals(input.get(Field.ISTAT_CODE.toString()), output.getIstatCode().toString());
        checkNotNullFields(output, Arrays.stream(Field.values())
                .map(Field::toString)
                .toArray(String[]::new));
    }

}