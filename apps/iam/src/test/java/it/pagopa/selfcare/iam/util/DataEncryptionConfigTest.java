package it.pagopa.selfcare.iam.util;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DataEncryptionConfigTest {

  @Inject DataEncryptionConfig dataEncryptionConfig;
  @Inject TenantContext tenantContext;

  @Test
  void shouldEncryptWithKeyOfCurrentTenant() {
    tenantContext.setTenantId("AR");
    String ar = dataEncryptionConfig.encrypt("user@example.com");
    tenantContext.setTenantId("PNPG");
    String pnpg = dataEncryptionConfig.encrypt("user@example.com");

    assertNotEquals(ar, pnpg);
    assertEquals("user@example.com", dataEncryptionConfig.decrypt(pnpg));
    tenantContext.setTenantId("AR");
    assertEquals("user@example.com", dataEncryptionConfig.decrypt(ar));
  }

  @Test
  void shouldMatchExistingArCiphertext() {
    tenantContext.setTenantId("AR");
    assertEquals(
        "AEpJ4wSTGoucJLy8OCYfL23M0kSt0WwYo9/cJ9jboUtEgxsJ",
        dataEncryptionConfig.encrypt("existing@example.com"));
  }

  @Test
  void shouldReturnEmptyForBlankValues() {
    tenantContext.setTenantId("AR");
    assertEquals("", dataEncryptionConfig.encrypt(null));
    assertEquals("", dataEncryptionConfig.decrypt(""));
  }

  @Test
  void shouldFailWithoutTenant() {
    assertThrows(RuntimeException.class, () -> dataEncryptionConfig.encrypt("user@example.com"));
  }
}
