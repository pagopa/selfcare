package it.pagopa.selfcare.party.registry_proxy.connector.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProxyRegistryExceptionTest {
    @Test
    void testConstructor() {
        assertEquals("Code", (new ProxyRegistryException("An error occurred", "Code")).getCode());
    }
}

