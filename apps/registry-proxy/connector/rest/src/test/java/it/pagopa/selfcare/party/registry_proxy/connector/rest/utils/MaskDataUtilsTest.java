package it.pagopa.selfcare.party.registry_proxy.connector.rest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MaskDataUtilsTest {

    @Test
    void testMaskInformation() {
        assertEquals("Data Buffered", MaskDataUtils.maskInformation("Data Buffered"));
        assertEquals("\"taxId\" : \"U*\"", MaskDataUtils.maskInformation("\"taxId\" : \"UU\""));
        assertEquals("\"taxId\" : \"*,*\"", MaskDataUtils.maskInformation("\"taxId\" : \"U,U\""));
        assertEquals("\"taxId\" : \"*@U\"", MaskDataUtils.maskInformation("\"taxId\" : \"U@U\""));

    }
}

