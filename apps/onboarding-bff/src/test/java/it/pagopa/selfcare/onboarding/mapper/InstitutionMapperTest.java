package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.onboarding.client.model.InstitutionInfo;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResource;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstitutionMapperTest {

    private final InstitutionMapper institutionMapper = new InstitutionMapperImpl();

    @Test
    void toResource_institutionInfo() {
        InstitutionInfo model = new InstitutionInfo();
        model.setId(UUID.randomUUID().toString());
        model.setDescription("desc");
        model.setTaxCode("tax");
        model.setAddress("addr");
        model.setDigitalAddress("mail");

        InstitutionResource resource = institutionMapper.toResource(model);

        assertNotNull(resource);
        assertEquals(model.getId(), resource.getId().toString());
        assertEquals(model.getDescription(), resource.getDescription());
        assertEquals(model.getTaxCode(), resource.getTaxCode());
        assertEquals(model.getAddress(), resource.getAddress());
        assertEquals(model.getDigitalAddress(), resource.getDigitalAddress());
    }

    @Test
    void toResource_nullInstitutionInfo() {
        assertNull(institutionMapper.toResource((InstitutionInfo) null));
    }
}
