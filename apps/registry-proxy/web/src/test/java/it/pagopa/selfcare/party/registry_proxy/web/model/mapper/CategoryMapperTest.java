package it.pagopa.selfcare.party.registry_proxy.web.model.mapper;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.web.model.CategoryResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.DummyCategory;
import org.junit.jupiter.api.Test;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.commons.utils.TestUtils.reflectionEqualsByName;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CategoryMapperTest {

    @Test
    void toResource_null() {
        //given
        final Category category = null;
        //when
        final CategoryResource resource = CategoryMapper.toResource(category);
        //then
        assertNull(resource);
    }


    @Test
    void toResource() {
        //given
        final Category category = mockInstance(new DummyCategory());
        //when
        final CategoryResource resource = CategoryMapper.toResource(category);
        //then
        assertNotNull(resource);
        reflectionEqualsByName(category, resource);
    }

}