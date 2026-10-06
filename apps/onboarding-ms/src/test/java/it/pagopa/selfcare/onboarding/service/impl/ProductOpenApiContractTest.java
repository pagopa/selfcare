package it.pagopa.selfcare.onboarding.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductOpenApiContractTest {

    @Test
    void consumerContractMatchesProductServer() throws Exception {
        Path module = Path.of(System.getProperty("basedir", "."));
        ObjectMapper mapper = new ObjectMapper();

        assertEquals(
                mapper.readTree(module.resolve("../product/src/main/docs/openapi.json").toFile()),
                mapper.readTree(module.resolve("src/main/openapi/product.json").toFile()),
                "Refresh the consumer specification from the Product MS OpenAPI contract");
    }
}
