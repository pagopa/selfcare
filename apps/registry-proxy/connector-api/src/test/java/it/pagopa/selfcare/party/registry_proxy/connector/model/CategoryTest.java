package it.pagopa.selfcare.party.registry_proxy.connector.model;

import org.junit.jupiter.api.Test;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryTest {

    @Test
    void createId() {
        // given
        final DummyCategory category = mockInstance(new DummyCategory());
        // when
        final String id = Category.createId(category.getOrigin(), category.getCode());
        // then
        assertEquals(category.getOrigin() + "_" + category.getCode(), id);
    }

}