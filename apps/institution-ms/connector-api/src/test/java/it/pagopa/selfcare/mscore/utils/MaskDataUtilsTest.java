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

    @Test
    void testMaskInformationWithAtSymbols() {
        assertEquals("{\"description\":\"E***ple@\"}",
                MaskDataUtils.maskInformation("{\"description\":\"Example@\"}"));
        assertEquals("{\"description\":\"@\"}",
                MaskDataUtils.maskInformation("{\"description\":\"@\"}"));
        assertEquals("{\"description\":\"@example.test\"}",
                MaskDataUtils.maskInformation("{\"description\":\"@example.test\"}"));
        assertEquals("{\"description\":\"E***ple@@\"}",
                MaskDataUtils.maskInformation("{\"description\":\"Example@@\"}"));
        assertEquals("{\"digitalAddress\":\"E***ple@example.test\"}",
                MaskDataUtils.maskInformation("{\"digitalAddress\":\"Example@example.test\"}"));
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
