package it.pagopa.selfcare.onboarding.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CsvAggregatePagoPaTest {

    @Test
    void testCsvAggregatePagoPaConstructorAndGetters() {
        String taxCode = "PAGOTAX12345";
        String vatNumber = "PAGOVAT12345678";
        String iban = "IT60X0542811101000000123456";
        Integer rowNumber = 2;

        CsvAggregatePagoPa csvAggregatePagoPa = new CsvAggregatePagoPa();
        csvAggregatePagoPa.setTaxCode(taxCode);
        csvAggregatePagoPa.setVatNumber(vatNumber);
        csvAggregatePagoPa.setIban(iban);
        csvAggregatePagoPa.setRowNumber(rowNumber);

        assertEquals(taxCode, csvAggregatePagoPa.getTaxCode());
        assertEquals(vatNumber, csvAggregatePagoPa.getVatNumber());
        assertEquals(iban, csvAggregatePagoPa.getIban());
        assertEquals(rowNumber, csvAggregatePagoPa.getRowNumber());
    }

    @Test
    void testSetRowNumber() {
        CsvAggregatePagoPa csvAggregatePagoPa = new CsvAggregatePagoPa();
        int expectedRowNumber = 15;

        csvAggregatePagoPa.setRowNumber(expectedRowNumber);

        assertEquals(expectedRowNumber, csvAggregatePagoPa.getRowNumber());
    }

    @Test
    void testCsvBindByPositionAnnotations() {
        CsvAggregatePagoPa csvAggregatePagoPa = new CsvAggregatePagoPa();
        csvAggregatePagoPa.setTaxCode("TESTTAXCODE");
        csvAggregatePagoPa.setIban("IT60X0542811101000000123456");

        assertNotNull(csvAggregatePagoPa);
        assertEquals("TESTTAXCODE", csvAggregatePagoPa.getTaxCode());
        assertEquals("IT60X0542811101000000123456", csvAggregatePagoPa.getIban());
    }
}
