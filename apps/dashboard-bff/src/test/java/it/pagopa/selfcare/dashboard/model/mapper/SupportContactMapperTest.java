package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.SupportContactResource;
import it.pagopa.selfcare.dashboard.model.institution.SupportContact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SupportContactMapperTest {
        @Test
        void toResource_notNull(){
            // given
            SupportContact model = new SupportContact();
            // when
            SupportContactResource resource = SupportContactMapper.toResource(model);
            // then
            assertEquals(model.getSupportEmail(), resource.getSupportEmail());
            assertEquals(model.getSupportPhone(), resource.getSupportPhone());
        }

        @Test
        void toResource_null() {
            // given
            SupportContact model = null;
            // when
            SupportContactResource resource = SupportContactMapper.toResource(model);
            // then
            assertNull(resource);
        }
}
