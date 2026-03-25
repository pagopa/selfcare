package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyInsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.InsuranceCompanyEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

class InsuranceCompanyToDocumentConverterTest {

    private final Function<InsuranceCompany, Document> converter;

    InsuranceCompanyToDocumentConverterTest() {
        this.converter = new InsuranceCompanyToDocumentConverter();
    }

    @Test
    void apply_nullInput() {
        // given
        final InsuranceCompany input = null;
        // when
        final Document output = converter.apply(input);
        // then
        assertNull(output);
    }

    @Test
    void apply() {
        // given
        final InsuranceCompany input = new DummyInsuranceCompany();
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getId(), output.get(Field.ID.toString()));
        assertEquals(input.getTaxCode(), output.get(Field.TAX_CODE.toString()));
        assertEquals(input.getDescription(), output.get(Field.DESCRIPTION.toString()));
        assertEquals(input.getDigitalAddress(), output.get(Field.DIGITAL_ADDRESS.toString()));
        assertEquals(input.getRegisterType(), output.get(Field.REGISTER_TYPE.toString()));
        assertEquals(input.getAddress(), output.get(Field.ADDRESS.toString()));
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
        final InsuranceCompany input = mockInstance(new InsuranceCompanyEntity());
        // when
        final Document output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.getId(), output.get(Field.ID.toString()));
        assertEquals(input.getTaxCode(), output.get(Field.TAX_CODE.toString()));
        assertEquals(input.getOrigin().toString(), output.get(Institution.Field.ORIGIN.toString()));
        assertEquals(input.getDescription(), output.get(Field.DESCRIPTION.toString()));
        assertEquals(input.getDigitalAddress(), output.get(Field.DIGITAL_ADDRESS.toString()));
        assertEquals(input.getAddress(), output.get(Field.ADDRESS.toString()));
        assertEquals(input.getRegisterType(), output.get(Field.REGISTER_TYPE.toString()));
        final Set<String> fieldValues = Arrays.stream(Field.values())
                .map(Field::toString)
                .collect(Collectors.toSet());
        assertEquals(1, output.getFields().stream()
                .filter(field -> !fieldValues.contains(field.name()))
                .count());
    }

}