package it.pagopa.selfcare.product.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.Test;

class TenantLogUtilsTest {

  @Test
  void inboundRequest_sanitizesTenantHeader() {
    ContainerRequestContext context = mock(ContainerRequestContext.class);
    when(context.getHeaderString("X-Tenant-Id")).thenReturn("AR/../PNPG");

    assertEquals("AR____PNPG", TenantLogUtils.fromInboundRequest(context));
  }

  @Test
  void clientRequest_returnsUnknownWhenHeaderIsMissing() {
    ClientRequestContext context = mock(ClientRequestContext.class);
    when(context.getHeaderString("X-Tenant-Id")).thenReturn(null);

    assertEquals("unknown", TenantLogUtils.fromClientRequest(context));
  }
}
