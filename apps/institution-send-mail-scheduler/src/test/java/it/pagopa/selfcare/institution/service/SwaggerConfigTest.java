package it.pagopa.selfcare.institution.service;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SwaggerConfigTest {

    @Test
    void swaggerSpringPlugin() {
        // Empty test required to avoid errors on Swagger Detect Rules and Conflict:
        // No tests matching pattern "SwaggerConfigTest#swaggerSpringPlugin"
        // TODO: consider to add -Dsurefire.failIfNoSpecifiedTests=false to the github action template
        assertTrue(true);
    }

}
