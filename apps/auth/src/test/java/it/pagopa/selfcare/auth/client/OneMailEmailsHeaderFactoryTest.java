package it.pagopa.selfcare.auth.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    MultivaluedHashMap<String, String> incoming = new MultivaluedHashMap<>();
    incoming.putSingle("x-api-key", "stale-key");
    MultivaluedHashMap<String, String> outgoing = new MultivaluedHashMap<>();
    outgoing.putSingle("x-api-key", "global-key");

    var arHeaders = factory.update(incoming, outgoing);
    var pnpgHeaders = factory.update(incoming, outgoing);

    assertEquals("ar-key", arHeaders.getFirst("x-api-key"));
    assertEquals("pnpg-key", pnpgHeaders.getFirst("x-api-key"));
    assertEquals(1, arHeaders.size());
    assertEquals(1, pnpgHeaders.size());
    verify(mailConfig).apiKey("AR");
    verify(mailConfig).apiKey("PNPG");
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
