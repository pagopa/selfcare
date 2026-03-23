package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyInstitution;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.InstitutionEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

class InstitutionToDocumentConverterTest {

    private final Function<Institution, Document> converter;


    InstitutionToDocumentConverterTest() {
        this.converter = new InstitutionToDocumentConverter();
    }


    @Test
    void apply_nullInput() {
        // given
        final Institution input = null;
        // when
        final Document output = converter.apply(input);
        // then
        assertNull(output);
    }


    @Test
    void apply() {
        // given
        final Institution input = new DummyInstitution();
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getId(), output.get(Field.ID.toString()));
        assertEquals(input.getOriginId(), output.get(Field.ORIGIN_ID.toString()));
        assertEquals(input.getO(), output.get(Field.O.toString()));
        assertEquals(input.getOu(), output.get(Field.OU.toString()));
        assertEquals(input.getAoo(), output.get(Field.AOO.toString()));
        assertEquals(input.getTaxCode(), output.get(Field.TAX_CODE.toString()));
        assertEquals(input.getCategory(), output.get(Field.CATEGORY.toString()));
        assertEquals(input.getDescription(), output.get(Field.DESCRIPTION.toString()));
        assertEquals(input.getDigitalAddress(), output.get(Field.DIGITAL_ADDRESS.toString()));
        assertEquals(input.getAddress(), output.get(Field.ADDRESS.toString()));
        assertEquals(input.getZipCode(), output.get(Field.ZIP_CODE.toString()));
        assertEquals(input.getOrigin().toString(), output.get(Field.ORIGIN.toString()));
        assertEquals(input.getIstatCode().toString(), output.get(Field.ISTAT_CODE.toString()));
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
        final Institution input = mockInstance(new InstitutionEntity(), "setO", "setOu", "setAoo");
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getId(), output.get(Field.ID.toString()));
        assertEquals(input.getOriginId(), output.get(Field.ORIGIN_ID.toString()));
        assertEquals(input.getO(), output.get(Field.O.toString()));
        assertEquals(input.getOu(), output.get(Field.OU.toString()));
        assertEquals(input.getAoo(), output.get(Field.AOO.toString()));
        assertEquals(input.getTaxCode(), output.get(Field.TAX_CODE.toString()));
        assertEquals(input.getCategory(), output.get(Field.CATEGORY.toString()));
        assertEquals(input.getDescription(), output.get(Field.DESCRIPTION.toString()));
        assertEquals(input.getDigitalAddress(), output.get(Field.DIGITAL_ADDRESS.toString()));
        assertEquals(input.getAddress(), output.get(Field.ADDRESS.toString()));
        assertEquals(input.getZipCode(), output.get(Field.ZIP_CODE.toString()));
        assertEquals(input.getOrigin().toString(), output.get(Field.ORIGIN.toString()));
        assertEquals(input.getIstatCode(), output.get(Field.ISTAT_CODE.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(1, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }

}