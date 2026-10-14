package it.pagopa.selfcare.security;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.TenantDefinition;
import java.util.HashMap;
import java.util.Map;

final class TestJwtTenantValidators {

  private TestJwtTenantValidators() {}

  /** Validator backed by a registry that defines AR and PNPG, with PNPG as default tenant. */
  static JwtTenantValidator supportingArAndPnpg() {
    Map<String, TenantDefinition> definitions = new HashMap<>();
    definitions.put("AR", null);
    definitions.put("PNPG", null);
    TenantRegistry tenantRegistry = mock(TenantRegistry.class);
    lenient().when(tenantRegistry.definitions()).thenReturn(definitions);
    return new JwtTenantValidator(tenantRegistry, "PNPG");
  }
}
