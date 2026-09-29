package it.pagopa.selfcare.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.auth.conf.TenantOutboundMailConfig;
import it.pagopa.selfcare.auth.context.AuthTenantContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

class OneMailEmailsHeaderFactoryTest {

  @Test
  void selectsTheCurrentTenantsKeyWithoutReusingThePreviousOne() {
    TenantOutboundMailConfig mailConfig = mock(TenantOutboundMailConfig.class);
    AuthTenantContext tenantContext = mock(AuthTenantContext.class);
    OneMailEmailsHeaderFactory factory = new OneMailEmailsHeaderFactory();
    factory.mailConfig = mailConfig;
    factory.tenantContext = tenantContext;
    when(tenantContext.getTenantId()).thenReturn("AR", "PNPG");
    when(mailConfig.apiKey("AR")).thenReturn("ar-key");
    when(mailConfig.apiKey("PNPG")).thenReturn("pnpg-key");

    assertEquals("ar-key", factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>())
        .getFirst("x-api-key"));
    assertEquals("pnpg-key", factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>())
        .getFirst("x-api-key"));
  }

  @Test
  void rejectsMissingTenant() {
    OneMailEmailsHeaderFactory factory = new OneMailEmailsHeaderFactory();
    factory.mailConfig = mock(TenantOutboundMailConfig.class);
    factory.tenantContext = mock(AuthTenantContext.class);
    when(factory.tenantContext.getTenantId()).thenThrow(new IllegalStateException("No tenant"));

    assertThrows(IllegalStateException.class,
        () -> factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>()));
  }
}
