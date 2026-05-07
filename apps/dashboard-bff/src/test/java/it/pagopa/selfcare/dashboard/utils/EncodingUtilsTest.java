package it.pagopa.selfcare.dashboard.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EncodingUtilsTest {

    @Test
    void isUrlEncodedTest() {
        assertTrue(EncodingUtils.isUrlEncoded(""));
        assertTrue(EncodingUtils.isUrlEncoded(null));
        assertTrue(EncodingUtils.isUrlEncoded("file%3A%2F%2F%2Fhome%2Fuser"));
        assertTrue(EncodingUtils.isUrlEncoded("%E2%9C%93"));
        assertTrue(EncodingUtils.isUrlEncoded("%7B%22key%22%3A123%7D"));
        assertTrue(EncodingUtils.isUrlEncoded("email%40example.com"));
        assertTrue(EncodingUtils.isUrlEncoded("hello%20world"));
        assertTrue(EncodingUtils.isUrlEncoded("hello+world"));
        assertFalse(EncodingUtils.isUrlEncoded("{\"key\":123}"));
        assertFalse(EncodingUtils.isUrlEncoded("✓"));
        assertFalse(EncodingUtils.isUrlEncoded("%ZZ"));
        assertFalse(EncodingUtils.isUrlEncoded("%2"));
        assertFalse(EncodingUtils.isUrlEncoded("hello%"));
        assertFalse(EncodingUtils.isUrlEncoded("%"));
        assertFalse(EncodingUtils.isUrlEncoded("100%"));
        assertFalse(EncodingUtils.isUrlEncoded("50% off"));
        assertFalse(EncodingUtils.isUrlEncoded("hello%2Gworld"));
        assertFalse(EncodingUtils.isUrlEncoded("café"));
        assertFalse(EncodingUtils.isUrlEncoded("hello world"));
    }

    @Test
    void isUUIDTest() {
        assertFalse(EncodingUtils.isUUID(null));
        assertFalse(EncodingUtils.isUUID(""));
        assertFalse(EncodingUtils.isUUID("1234-abcd"));
        assertTrue(EncodingUtils.isUUID("6fa459ea-ee8a-3ca4-894e-db77e1600c4c"));
    }

    @Test
    void toUUIDOrNull() {
        assertNull(EncodingUtils.toUUIDOrNull(null));
        assertNull(EncodingUtils.toUUIDOrNull(""));
        assertNull(EncodingUtils.toUUIDOrNull("1234-abcd"));
        assertEquals(UUID.fromString("6fa459ea-ee8a-3ca4-894e-db77e1600c4c"), EncodingUtils.toUUIDOrNull("6fa459ea-ee8a-3ca4-894e-db77e1600c4c"));
    }

}
