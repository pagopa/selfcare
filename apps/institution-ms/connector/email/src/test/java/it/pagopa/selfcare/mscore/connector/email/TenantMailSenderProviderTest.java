package it.pagopa.selfcare.mscore.connector.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.UnresolvedTenantException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class TenantMailSenderProviderTest {

    @Test
    void selectsAndCachesSmtpConfigurationAndFromPerTenant() {
        TenantContext context = new TenantContext();
        TenantRegistry registry = new TenantRegistry(new ObjectMapper(), new MockEnvironment()
                .withProperty("tenant.mongodb.required", "false")
                .withProperty("USR_AR", "ar-user").withProperty("PSW_AR", "ar-password")
                .withProperty("USR_PNPG", "pnpg-user").withProperty("PSW_PNPG", "pnpg-password"),
                """
                {"AR":{"pec":{"host":"smtp.ar.test","port":465,"sender":"ar@test.it",
                             "usernameEnvVar":"USR_AR","passwordEnvVar":"PSW_AR"}},
                 "PNPG":{"pec":{"host":"smtp.pnpg.test","port":587,"sender":"pnpg@test.it",
                             "usernameEnvVar":"USR_PNPG","passwordEnvVar":"PSW_PNPG"}},
                 "EMPTY":{}}
                """, "*", "");
        registry.initialize();
        TenantMailSenderProvider provider = new TenantMailSenderProvider(registry, context, true, true, "smtps");
        var ar = context.withTenant("AR", provider::currentSender);
        var pnpg = context.withTenant("PNPG", provider::currentSender);
        assertNotSame(ar, pnpg);
        assertSame(ar, context.withTenant("AR", provider::currentSender));
        assertSame(pnpg, context.withTenant("PNPG", provider::currentSender));
        assertEquals("smtp.ar.test", ar.getHost());
        assertEquals(465, ar.getPort());
        assertEquals("ar-user", ar.getUsername());
        assertEquals("ar-password", ar.getPassword());
        assertEquals("smtp.pnpg.test", pnpg.getHost());
        assertEquals(587, pnpg.getPort());
        assertEquals("pnpg-user", pnpg.getUsername());
        assertEquals("pnpg-password", pnpg.getPassword());
        assertEquals("smtps", ar.getProtocol());
        assertEquals("true", ar.getJavaMailProperties().getProperty("mail.smtp.auth"));
        assertEquals("true", pnpg.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
        assertEquals("ar@test.it", context.withTenant("AR", provider::currentFrom));
        assertEquals("pnpg@test.it", context.withTenant("PNPG", provider::currentFrom));
        assertThrows(UnresolvedTenantException.class, provider::currentSender);
        assertThrows(IllegalStateException.class, () -> context.withTenant("EMPTY", provider::currentSender));
    }
}
