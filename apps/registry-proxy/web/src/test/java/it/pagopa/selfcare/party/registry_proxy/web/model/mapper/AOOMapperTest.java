package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.web.model.AOOResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.AOOsResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyAOO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.commons.utils.TestUtils.reflectionEqualsByName;
import static org.junit.jupiter.api.Assertions.*;

class AOOMapperTest {

    @Test
    void toResource_null() {
        //given
        final AOO aoo = null;
        //when
        final AOOResource resource = AOOMapper.toResource(aoo);
        //then
        assertNull(resource);
    }


    @Test
    void toResource() {
        //given
        final AOO aoo = mockInstance(new DummyAOO());
        //when
        final AOOResource resource = AOOMapper.toResource(aoo);
        //then
        assertNotNull(resource);
        reflectionEqualsByName(aoo, resource);
    }

    @Test
    void toResourceList_null() {
        //given
        final List<AOOResource> aoos = null;
        final long count = 0;
        //when
        final AOOsResource resource = AOOMapper.toResource(aoos, count);
        //then
        assertNull(resource);
    }


    @Test
    void toResourceList() {
        //given
        final List<AOOResource> aoos = List.of(mockInstance(new AOOResource()));
        final long count = 0;
        //when
        final AOOsResource resource = AOOMapper.toResource(aoos, count);
        //then
        assertNotNull(resource);
        assertSame(aoos, resource.getItems());
    }

}