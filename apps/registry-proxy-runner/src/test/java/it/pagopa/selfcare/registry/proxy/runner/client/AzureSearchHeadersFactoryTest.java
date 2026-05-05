package it.pagopa.selfcare.registry.proxy.runner.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

class AzureSearchHeadersFactoryTest {

    @Test
    void testHeadersFactory() {
        AzureSearchHeadersFactory factory = new AzureSearchHeadersFactory();
        factory.apiKey = "secret-key";

        MultivaluedMap<String, String> outHeaders = new MultivaluedHashMap<>();
        outHeaders.add("Existing", "Value");

        MultivaluedMap<String, String> updated = factory.update(new MultivaluedHashMap<>(), outHeaders);

        assertEquals("Value", updated.getFirst("Existing"));
        assertEquals("secret-key", updated.getFirst("api-key"));
        assertEquals("application/json", updated.getFirst("Content-Type"));
    }
}
