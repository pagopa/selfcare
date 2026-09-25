package it.pagopa.selfcare.product.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StorageKeysTest {

  @Test
  void contractsKeyIsStable() {
    assertEquals("contracts", StorageKeys.CONTRACTS);
  }
}
