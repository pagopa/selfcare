package it.pagopa.selfcare.onboarding.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class StorageKeysTest {

    @Test
    void products_isExpectedLogicalKey() {
        assertEquals("products", StorageKeys.PRODUCTS);
    }

    @Test
    void constructor_isPrivate() throws Exception {
        Constructor<StorageKeys> constructor = StorageKeys.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
