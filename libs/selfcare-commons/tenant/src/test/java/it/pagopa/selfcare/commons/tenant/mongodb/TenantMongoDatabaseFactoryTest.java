package it.pagopa.selfcare.commons.tenant.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TenantMongoDatabaseFactoryTest {

    @Test
    void selectsClientAndDatabaseFromCurrentTenant() {
        String registryJson = """
                {
                  "AR": {
                    "mongo": {
                      "account": "ar",
                      "database": "groups-ar",
                      "connectionStringEnvVar": "MONGO_AR"
                    }
                  },
                  "PNPG": {
                    "mongo": {
                      "account": "pnpg",
                      "database": "groups-pnpg",
                      "connectionStringEnvVar": "MONGO_PNPG"
                    }
                  }
                }
                """;
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MONGO_AR", "mongodb://localhost:27017")
                .withProperty("MONGO_PNPG", "mongodb://localhost:27018");
        TenantRegistry registry =
                new TenantRegistry(new ObjectMapper(), environment, registryJson, "*", "");
        registry.initialize();
        TenantContext context = new TenantContext();
        TenantMongoDatabaseFactory factory =
                new TenantMongoDatabaseFactory(registry, context);

        context.setTenantId("AR");
        assertEquals("groups-ar", factory.getMongoDatabase().getName());
        context.setTenantId("PNPG");
        assertEquals("groups-pnpg", factory.getMongoDatabase("ignored").getName());
        context.clear();
        assertThrows(IllegalStateException.class, factory::getMongoDatabase);

        factory.close();
    }
}
