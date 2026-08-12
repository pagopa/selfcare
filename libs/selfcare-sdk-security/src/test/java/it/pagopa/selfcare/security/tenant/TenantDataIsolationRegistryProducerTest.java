package it.pagopa.selfcare.security.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TenantDataIsolationRegistryProducerTest {

  @Test
  void shouldProduceARegistryFromConfiguration() {
    TenantDataIsolationRegistryProducer producer = new TenantDataIsolationRegistryProducer();
    producer.rawRegistry = "{\"PNPG\": {\"storage_account_infix\": \"pnpg\"}}";

    assertEquals("pnpg", producer.tenantDataIsolationRegistry().storageAccountInfix(TenantId.PNPG));
  }

  @Test
  void shouldProduceAFailClosedRegistryWhenTheVariableIsNotSet() {
    TenantDataIsolationRegistryProducer producer = new TenantDataIsolationRegistryProducer();
    producer.rawRegistry = "{}";

    TenantDataIsolationRegistry registry = producer.tenantDataIsolationRegistry();

    assertThrows(
        UnresolvedTenantMappingException.class, () -> registry.storageAccountInfix(TenantId.AR));
  }

  @Test
  void shouldFailAtStartupOnAMalformedRegistry() {
    TenantDataIsolationRegistryProducer producer = new TenantDataIsolationRegistryProducer();
    producer.rawRegistry = "not-json";

    assertThrows(IllegalStateException.class, producer::tenantDataIsolationRegistry);
  }
}
