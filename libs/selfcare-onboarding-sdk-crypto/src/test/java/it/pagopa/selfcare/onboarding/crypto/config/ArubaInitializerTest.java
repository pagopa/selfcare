package it.pagopa.selfcare.onboarding.crypto.config;

import it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.Auth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArubaInitializerTest {

    @Test
    void initializeConfig_acceptsTenantBaseUrlAndCredentials() {
        Auth auth = new Auth();
        auth.setTypeOtpAuth("tenant-type");
        auth.setOtpPwd("tenant-otp");
        auth.setUser("tenant-user");
        auth.setDelegatedUser("tenant-delegated-user");
        auth.setDelegatedPassword("tenant-delegated-password");
        auth.setDelegatedDomain("tenant-domain");

        ArubaSignConfig config = ArubaInitializer.initializeConfig(
                "https://tenant.example/aruba", 10, 20, auth);

        assertEquals("https://tenant.example/aruba", config.getBaseUrl());
        assertEquals(10, config.getConnectTimeoutMs());
        assertEquals(20, config.getRequestTimeoutMs());
        assertEquals("tenant-type", config.getAuth().getTypeOtpAuth());
        assertEquals("tenant-otp", config.getAuth().getOtpPwd());
        assertEquals("tenant-user", config.getAuth().getUser());
        assertEquals("tenant-delegated-user", config.getAuth().getDelegatedUser());
        assertEquals("tenant-delegated-password", config.getAuth().getDelegatedPassword());
        assertEquals("tenant-domain", config.getAuth().getDelegatedDomain());
        assertEquals("COSIGN", config.getAuth().getTypeHSM());
    }
}
