package it.pagopa.selfcare.mscore.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MaskDataUtilsTest {
    /**
     * Method under test: {@link MaskDataUtils#maskInformation(String)}
     */
    @Test
    void testMaskInformation() {
        assertEquals("Data Buffered", MaskDataUtils.maskInformation("Data Buffered"));
        assertEquals("\"elencoCf\" : [\"U*\"", MaskDataUtils.maskInformation("\"elencoCf\" : [\"UU\""));
        assertEquals("\"elencoCf\" : [\"V***********ssi, **\"", MaskDataUtils.maskInformation("\"elencoCf\" : [\"Via Mario Rossi, 47\""));
        assertEquals("\"elencoCf\" : [\"m******ssi@mario.rossi\"", MaskDataUtils.maskInformation("\"elencoCf\" : [\"mariorossi@mario.rossi\""));

    }

    /**
     * Method under test: {@link MaskDataUtils#maskString(String)}
     */
    @Test
    void testMaskString() {
        assertEquals("S****ext", MaskDataUtils.maskString("Str Text"));
        assertEquals("", MaskDataUtils.maskString(""));
    }
}

