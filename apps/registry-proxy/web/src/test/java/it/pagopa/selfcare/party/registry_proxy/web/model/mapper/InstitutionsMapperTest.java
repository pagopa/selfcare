package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.web.model.InstitutionResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.InstitutionsResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

class InstitutionsMapperTest {

    @Test
    void toResource_null() {
        //given
        final List<InstitutionResource> institutions = null;
        final int count = 0;
        //when
        final InstitutionsResource resource = InstitutionsMapper.toResource(institutions, count);
        //then
        assertNull(resource);
    }


    @Test
    void toResource() {
        //given
        final List<InstitutionResource> institutions = List.of(mockInstance(new InstitutionResource()));
        final int count = 0;
        //when
        final InstitutionsResource resource = InstitutionsMapper.toResource(institutions, count);
        //then
        assertNotNull(resource);
        assertSame(institutions, resource.getItems());
        assertEquals(count, resource.getCount());
    }

}