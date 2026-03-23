package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.DummyInsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station.Field;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static org.junit.jupiter.api.Assertions.*;

class DocumentToInsuranceConverterTest {

    private final Function<Document, InsuranceCompany> converter;

    DocumentToInsuranceConverterTest() {
        this.converter = new DocumentToInsuranceCompanyConverter();
    }

    @Test
    void apply_nullInput() {
        // given
        final Document input = null;
        // when
        final InsuranceCompany output = converter.apply(input);
        // then
        assertNull(output);
    }

    @Test
    void apply() {
        // given
        final Document input = new DummyInsuranceCompany().toDocument();
        // when
        final InsuranceCompany output = converter.apply(input);
        // then
        assertNotNull(output);
        assertEquals(input.get(Field.ID.toString()), output.getId());
        assertEquals(input.get(Field.TAX_CODE.toString()), output.getTaxCode());
        assertEquals(input.get(Field.DESCRIPTION.toString()), output.getDescription());
        assertEquals(input.get(Field.DIGITAL_ADDRESS.toString()), output.getDigitalAddress());
        assertEquals(output.getOrigin(), Origin.IVASS);
    }

}