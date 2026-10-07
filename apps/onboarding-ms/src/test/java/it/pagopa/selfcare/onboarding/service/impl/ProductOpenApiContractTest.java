package it.pagopa.selfcare.onboarding.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductOpenApiContractTest {

    @Test
    void consumerContractMatchesProductServer() throws Exception {
        // given
        Path module = Path.of(System.getProperty("basedir", "."));
        ObjectMapper mapper = new ObjectMapper();
        var serverContract =
                mapper.readTree(module.resolve("../product/src/main/docs/openapi.json").toFile());
        var consumerContract =
                mapper.readTree(module.resolve("src/main/openapi/product.json").toFile());

        // when
        String requiredDocumentsHeaderType = serverContract
                .at("/paths/~1product~1{productId}~1required-documents~1enabled/head/responses/200/headers/X-Required-Documents-Enabled/schema/type")
                .asText();

        // then
        assertEquals(
                serverContract,
                consumerContract,
                "Refresh the consumer specification from the Product MS OpenAPI contract");
        assertEquals("boolean", requiredDocumentsHeaderType,
                "The required-documents response header must declare its OpenAPI schema");
    }
}
