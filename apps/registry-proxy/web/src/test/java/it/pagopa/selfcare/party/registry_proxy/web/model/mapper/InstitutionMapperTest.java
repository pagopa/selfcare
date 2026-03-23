package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyInstitution;
import it.pagopa.selfcare.party.registry_proxy.web.model.InstitutionResource;
import org.junit.jupiter.api.Test;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.commons.utils.TestUtils.reflectionEqualsByName;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InstitutionMapperTest {

    @Test
    void toResource_null() {
        //given
        final Institution institution = null;
        //when
        final InstitutionResource resource = InstitutionMapper.toResource(institution);
        //then
        assertNull(resource);
    }


    @Test
    void toResource() {
        //given
        final Institution category = mockInstance(new DummyInstitution());
        //when
        final InstitutionResource resource = InstitutionMapper.toResource(category);
        //then
        assertNotNull(resource);
        reflectionEqualsByName(category, resource);
    }

}