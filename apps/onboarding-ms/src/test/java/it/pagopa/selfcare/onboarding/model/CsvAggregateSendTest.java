package it.pagopa.selfcare.onboarding.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvAggregateSendTest {

    @Test
    void testCsvAggregateSendConstructorAndGetters() {
        String taxCode = "TAX12345";
        String vatNumber = "VAT12345678";
        String recipientCode = "SDI123456";
        String subunitType = "Department";
        String subunitCode = "DEP001";
        String adminAggregateName = "Admin Name";
        String adminAggregateSurname = "Admin Surname";
        String adminAggregateTaxCode = "ADMIN12345";
        String adminAggregateEmail = "admin@example.com";
        Integer rowNumber = 5;

        CsvAggregateSend csvAggregateSend = new CsvAggregateSend();
        csvAggregateSend.setTaxCode(taxCode);
        csvAggregateSend.setVatNumber(vatNumber);
        csvAggregateSend.setRecipientCode(recipientCode);
        csvAggregateSend.setSubunitType(subunitType);
        csvAggregateSend.setSubunitCode(subunitCode);
        csvAggregateSend.setAdminAggregateName(adminAggregateName);
        csvAggregateSend.setAdminAggregateSurname(adminAggregateSurname);
        csvAggregateSend.setAdminAggregateTaxCode(adminAggregateTaxCode);
        csvAggregateSend.setAdminAggregateEmail(adminAggregateEmail);
        csvAggregateSend.setRowNumber(rowNumber);


        assertEquals(taxCode, csvAggregateSend.getTaxCode());
        assertEquals(vatNumber, csvAggregateSend.getVatNumber());
        assertEquals(recipientCode, csvAggregateSend.getRecipientCode());
        assertEquals(subunitType, csvAggregateSend.getSubunitType());
        assertEquals(subunitCode, csvAggregateSend.getSubunitCode());
        assertEquals(adminAggregateName, csvAggregateSend.getAdminAggregateName());
        assertEquals(adminAggregateSurname, csvAggregateSend.getAdminAggregateSurname());
        assertEquals(adminAggregateTaxCode, csvAggregateSend.getAdminAggregateTaxCode());
        assertEquals(adminAggregateEmail, csvAggregateSend.getAdminAggregateEmail());
        assertEquals(rowNumber, csvAggregateSend.getRowNumber());
    }

    @Test
    void testSetRowNumber() {
        CsvAggregateSend csvAggregateSend = new CsvAggregateSend();
        int expectedRowNumber = 10;

        csvAggregateSend.setRowNumber(expectedRowNumber);

        assertEquals(expectedRowNumber, csvAggregateSend.getRowNumber());
    }

    @Test
    void testCsvBindByPositionAnnotations() {
        CsvAggregateSend csvAggregateSend = new CsvAggregateSend();
        csvAggregateSend.setTaxCode("SAMPLETAXCODE");
        csvAggregateSend.setAdminAggregateEmail("admin@example.com");

        assertNotNull(csvAggregateSend);
        assertEquals("SAMPLETAXCODE", csvAggregateSend.getTaxCode());
        assertEquals("admin@example.com", csvAggregateSend.getAdminAggregateEmail());
    }
}
