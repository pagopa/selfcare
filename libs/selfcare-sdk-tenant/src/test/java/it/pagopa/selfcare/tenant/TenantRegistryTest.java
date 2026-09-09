package it.pagopa.selfcare.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TenantRegistryTest {

    @Test
    void sanitizeConnectionString_decodesHtmlAmpersands() {
        String encoded =
                "mongodb://user:pwd@selc-d-cosmosdb-mongodb-account.mongo.cosmos.azure.com:10255/"
                        + "selcOnboarding?ssl=true&amp;replicaSet=globaldb&amp;retrywrites=false"
                        + "&amp;maxIdleTimeMS=120000&amp;appName=@selc-d-cosmosdb-mongodb-account@";

        String sanitized = TenantRegistry.sanitizeConnectionString(encoded);

        assertEquals(
                "mongodb://user:pwd@selc-d-cosmosdb-mongodb-account.mongo.cosmos.azure.com:10255/"
                        + "selcOnboarding?ssl=true&replicaSet=globaldb&retrywrites=false"
                        + "&maxIdleTimeMS=120000&appName=@selc-d-cosmosdb-mongodb-account@",
                sanitized);
    }
}
