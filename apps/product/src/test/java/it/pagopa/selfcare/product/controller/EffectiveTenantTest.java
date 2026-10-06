package it.pagopa.selfcare.product.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.security.identity.SecurityIdentity;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EffectiveTenantTest {

  @Mock TenantContext tenantContext;
  @Mock TenantRegistry tenantRegistry;
  @Mock SecurityIdentity securityIdentity;
  @Mock JsonWebToken jwt;

  private EffectiveTenant effectiveTenant;

  @BeforeEach
  void setUp() {
    effectiveTenant = new EffectiveTenant(tenantContext, tenantRegistry, securityIdentity);
  }

  @Test
  void omittedQueryUsesHeaderTenantAndIgnoresJwtTenantAttribute() {
    when(tenantContext.isInitialized()).thenReturn(true);
    when(tenantContext.getTenantId()).thenReturn("AR");
    when(securityIdentity.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaim(EffectiveTenant.JWT_TENANT_CLAIM)).thenReturn("AR");
    when(tenantRegistry.normalizeTenantId("AR")).thenReturn("AR");

    assertEquals("AR", effectiveTenant.resolve(" "));

    verify(securityIdentity, never()).getAttribute(any());
  }

  @Test
  void omittedQueryUsesTenantIdClaimWhenHeaderIsAbsent() {
    when(tenantContext.isInitialized()).thenReturn(false);
    when(securityIdentity.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaim(EffectiveTenant.JWT_TENANT_CLAIM)).thenReturn("PNPG");

    assertEquals("PNPG", effectiveTenant.resolve(null));

    verify(securityIdentity, never()).getAttribute(any());
  }

  @Test
  void omittedQueryDoesNotDefaultFromJwtTenantAttribute() {
    when(tenantContext.isInitialized()).thenReturn(false);
    when(securityIdentity.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaim(EffectiveTenant.JWT_TENANT_CLAIM)).thenReturn(null);

    assertThrows(BadRequestException.class, () -> effectiveTenant.resolve(null));
    verify(securityIdentity, never()).getAttribute(any());
  }

  @Test
  void omittedQueryRejectsHeaderAndClaimMismatch() {
    when(tenantContext.isInitialized()).thenReturn(true);
    when(tenantContext.getTenantId()).thenReturn("AR");
    when(securityIdentity.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaim(EffectiveTenant.JWT_TENANT_CLAIM)).thenReturn("PNPG");
    when(tenantRegistry.normalizeTenantId("AR")).thenReturn("AR");
    when(tenantRegistry.normalizeTenantId("PNPG")).thenReturn("PNPG");

    assertThrows(BadRequestException.class, () -> effectiveTenant.resolve(null));
  }
}
