package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.web.model.CategoriesResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.CategoryResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.*;

class CategoriesMapperTest {

    @Test
    void toResource_null() {
        //given
        final List<CategoryResource> categories = null;
        //when
        final CategoriesResource resource = CategoriesMapper.toResource(categories);
        //then
        assertNull(resource);
    }


    @Test
    void toResource() {
        //given
        final List<CategoryResource> categories = List.of(mockInstance(new CategoryResource()));
        //when
        final CategoriesResource resource = CategoriesMapper.toResource(categories);
        //then
        assertNotNull(resource);
        assertSame(categories, resource.getItems());
    }

}