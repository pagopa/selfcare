package it.pagopa.selfcare.user.conf;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@QuarkusTest
class CloudTemplateLoaderTest {
    @Inject
    CloudTemplateLoader cloudTemplateLoader;

    @Test
    void testFindTemplateSource() throws IOException {
        String name = (String) cloudTemplateLoader.findTemplateSource("test_en_US.txt");
        Assertions.assertEquals("test.txt", name);
    }
}
