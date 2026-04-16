package it.pagopa.selfcare.mscore.connector.azure_storage.config;

import it.pagopa.selfcare.mscore.config.AzureStorageConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AzureStorageConfigTest {

    /**
     * Method under test: {@link AzureStorageConfig#getConnectionString()}
     */
    @Test
    void testGetConnectionString2() {

        AzureStorageConfig azureStorageConfig = new AzureStorageConfig();
        azureStorageConfig.setConnectionString("42");
        assertEquals("42", azureStorageConfig.getConnectionString());
    }

    /**
     * Method under test: {@link AzureStorageConfig#getContainer()} ());
    }

    /**
     * Method under test: {@link AzureStorageConfig#getContainer()} ()}
     */
    @Test
    void testGetContractsTemplateContainer3() {

        AzureStorageConfig azureStorageConfig = new AzureStorageConfig();
        azureStorageConfig.setContractPath("42");
        assertEquals("42", azureStorageConfig.getContractPath());
    }

    @Test
    void testGetContractsTemplateContainer4() {

        AzureStorageConfig azureStorageConfig = new AzureStorageConfig();
        azureStorageConfig.setConnectionString("42");
        azureStorageConfig.setContainer("42");
        azureStorageConfig.setContractPath("42");

        assertEquals("AzureStorageConfig(connectionString=42, accountName=null, endpointSuffix=null, container=42, contractPath=42, checkoutTemplateContainer=null)", azureStorageConfig.toString());
    }
}

