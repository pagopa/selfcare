package it.pagopa.selfcare.user.util;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class GeneralUtilsTest {

    private enum TestEnum { ALFA, BETA, GAMMA }

    @Test
    void parseEnumList() {
        final List<TestEnum> fullList = GeneralUtils.parseEnumList(List.of("GAMMA", "BETA", "ALFA", "BETA"), TestEnum.class);
        assertEquals(4, fullList.size());
        assertEquals(TestEnum.GAMMA, fullList.get(0));
        assertEquals(TestEnum.BETA, fullList.get(1));
        assertEquals(TestEnum.ALFA, fullList.get(2));
        assertEquals(TestEnum.BETA, fullList.get(3));

        final List<String> invalidList = List.of("GAMMA", "BETA", "DELTA", "ALFA");
        final InvalidRequestException ex = assertThrowsExactly(InvalidRequestException.class, () -> GeneralUtils.parseEnumList(invalidList, TestEnum.class));
        assertEquals("Invalid value DELTA for TestEnum", ex.getMessage());

        final List<TestEnum> emptyList = GeneralUtils.parseEnumList(null, TestEnum.class);
        assertTrue(emptyList.isEmpty());
    }

}
