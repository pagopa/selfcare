package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.backoffice.BrokerInfo;
import it.pagopa.selfcare.dashboard.model.product.BrokerResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrokerResourceMapperTest {

    @Test
    void testToResource() {
        BrokerResourceMapperImpl mapper = new BrokerResourceMapperImpl();
        BrokerInfo brokerInfo = new BrokerInfo();
        brokerInfo.setCode("code");
        brokerInfo.setEnabled(true);
        brokerInfo.setDescription("description");
        BrokerResource resource = mapper.toResource(brokerInfo);
        assertNotNull(resource);
        assertEquals(resource.getCode(), brokerInfo.getCode());
        assertEquals(resource.getDescription(), brokerInfo.getDescription());
        assertTrue(resource.getEnabled());
    }

    @Test
    void testToResourceList() {
        BrokerResourceMapperImpl mapper = new BrokerResourceMapperImpl();
        BrokerInfo brokerInfo = new BrokerInfo();
        brokerInfo.setCode("code");
        brokerInfo.setEnabled(true);
        brokerInfo.setDescription("description");
        List<BrokerResource> resources = mapper.toResourceList(List.of(brokerInfo));
        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertNotNull(resources.get(0));
        assertEquals(resources.get(0).getCode(), brokerInfo.getCode());
        assertEquals(resources.get(0).getDescription(), brokerInfo.getDescription());
        assertTrue(resources.get(0).getEnabled());
    }
}
