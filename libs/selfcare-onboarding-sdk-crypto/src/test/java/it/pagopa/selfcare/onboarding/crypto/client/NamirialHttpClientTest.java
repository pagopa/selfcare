package it.pagopa.selfcare.onboarding.crypto.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamirialHttpClientTest {

    @Test
    void constructorBuildsSignUrlFromTenantBaseUrl() {
        NamirialHttpClient client = new NamirialHttpClient("https://tenant.example/namirial");

        assertEquals(
                "https://tenant.example/namirial/SignEngineWeb/rest/service/signPAdES",
                client.signPadesUrl());
    }
}
