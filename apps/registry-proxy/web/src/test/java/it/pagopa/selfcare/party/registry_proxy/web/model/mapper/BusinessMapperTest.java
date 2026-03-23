package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries.Business;
import it.pagopa.selfcare.party.registry_proxy.web.model.BusinessResource;
import org.junit.jupiter.api.Test;

class BusinessMapperTest {
    /**
     * Method under test: {@link BusinessMapper#toResource(Business)}
     */
    @Test
    void testToResource() {
        Business business = new Business();
        business.setBusinessName("Business Name");
        business.setBusinessTaxId("42");
        BusinessResource actualToResourceResult = BusinessMapper.toResource(business);
        assertEquals("Business Name", actualToResourceResult.getBusinessName());
        assertEquals("42", actualToResourceResult.getBusinessTaxId());
    }

    /**
     * Method under test: {@link BusinessMapper#toResource(Business)}
     */
    @Test
    void testToResourceNull() { assertNull(BusinessMapper.toResource(null)); }
}

