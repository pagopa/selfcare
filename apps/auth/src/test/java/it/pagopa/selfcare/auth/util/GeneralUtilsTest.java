package it.pagopa.selfcare.auth.util;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GeneralUtilsTest {

  private enum TestEnum {
    ALFA,
    BETA,
    GAMMA
  }

  @Test
  void parseEnumList() {
    final List<TestEnum> fullList =
        GeneralUtils.parseEnumList(List.of("GAMMA", "BETA", "ALFA", "BETA"), TestEnum.class);
    assertEquals(4, fullList.size());
    assertEquals(TestEnum.GAMMA, fullList.get(0));
    assertEquals(TestEnum.BETA, fullList.get(1));
    assertEquals(TestEnum.ALFA, fullList.get(2));
    assertEquals(TestEnum.BETA, fullList.get(3));

    final List<String> invalidList = List.of("GAMMA", "BETA", "DELTA", "ALFA");
    final InvalidRequestException ex =
        assertThrowsExactly(
            InvalidRequestException.class,
            () -> GeneralUtils.parseEnumList(invalidList, TestEnum.class));
    assertEquals("Invalid value DELTA for TestEnum", ex.getMessage());

    final List<TestEnum> emptyList = GeneralUtils.parseEnumList(null, TestEnum.class);
    assertTrue(emptyList.isEmpty());
  }
}
