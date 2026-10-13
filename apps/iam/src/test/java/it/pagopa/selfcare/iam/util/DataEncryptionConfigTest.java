package it.pagopa.selfcare.iam.util;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
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
    assertEquals(pnpg, dataEncryptionConfig.encrypt("user@example.com", "PNPG"));
    assertEquals(ar, dataEncryptionConfig.encrypt("user@example.com", "AR"));
  }

  @Test
  void shouldDecryptWithKeyOfAnyTenant() {
    String ar = dataEncryptionConfig.encrypt("user@example.com", "AR");
    String pnpg = dataEncryptionConfig.encrypt("user@example.com", "PNPG");

    tenantContext.setTenantId("PNPG");
    assertEquals("user@example.com", dataEncryptionConfig.decrypt(ar));
    assertEquals("user@example.com", dataEncryptionConfig.decrypt(pnpg));
    tenantContext.setTenantId("AR");
    assertEquals("user@example.com", dataEncryptionConfig.decrypt(pnpg));
  }

  @Test
  void shouldEncryptForEveryTenantCurrentFirst() {
    tenantContext.setTenantId("PNPG");
    Map<String, String> ciphertexts = dataEncryptionConfig.encryptForEveryTenant("a@example.com");

    assertEquals(List.of("PNPG", "AR"), List.copyOf(ciphertexts.keySet()));
    assertEquals(dataEncryptionConfig.encrypt("a@example.com", "AR"), ciphertexts.get("AR"));
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
  void shouldFailForUnknownTenantOrUndecryptableValue() {
    tenantContext.setTenantId("AR");
    assertThrows(
        IllegalStateException.class, () -> dataEncryptionConfig.encrypt("a@example.com", "XX"));
    assertThrows(IllegalStateException.class, () -> dataEncryptionConfig.decrypt("bm90LXZhbGlk"));
  }

  @Test
  void shouldFailWithoutTenant() {
    assertThrows(RuntimeException.class, () -> dataEncryptionConfig.encrypt("user@example.com"));
  }
}
