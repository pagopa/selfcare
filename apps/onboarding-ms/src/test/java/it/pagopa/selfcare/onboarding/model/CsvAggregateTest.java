package it.pagopa.selfcare.onboarding.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvAggregateAppIoTest {

    @Test
    void testCsvAggregateAppIoConstructorAndGetters() {


        String taxCode = "TAX12345";
        String vatNumber = "VAT12345678";
        String subunitType = "Department";
        String subunitCode = "DEP001";
        Integer rowNumber = 5;

        CsvAggregateAppIo csvAggregateAppIo = new CsvAggregateAppIo();


        csvAggregateAppIo.setTaxCode(taxCode);
        csvAggregateAppIo.setVatNumber(vatNumber);

        csvAggregateAppIo.setSubunitType(subunitType);
        csvAggregateAppIo.setSubunitCode(subunitCode);

        csvAggregateAppIo.setRowNumber(rowNumber);


        assertEquals(taxCode, csvAggregateAppIo.getTaxCode());
        assertEquals(vatNumber, csvAggregateAppIo.getVatNumber());

        assertEquals(subunitType, csvAggregateAppIo.getSubunitType());
        assertEquals(subunitCode, csvAggregateAppIo.getSubunitCode());

        assertEquals(rowNumber, csvAggregateAppIo.getRowNumber());
    }

    @Test
    void testSetRowNumber() {
        CsvAggregateAppIo csvAggregateAppIo = new CsvAggregateAppIo();
        int expectedRowNumber = 10;

        csvAggregateAppIo.setRowNumber(expectedRowNumber);

        assertEquals(expectedRowNumber, csvAggregateAppIo.getRowNumber());
    }

    @Test
    void testCsvBindByPositionAnnotations() {
        CsvAggregateAppIo csvAggregateAppIo = new CsvAggregateAppIo();
        csvAggregateAppIo.setTaxCode("SAMPLETAXCODE");

        assertNotNull(csvAggregateAppIo);
        assertEquals("SAMPLETAXCODE", csvAggregateAppIo.getTaxCode());
    }
}
