package it.pagopa.selfcare.mscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource("classpath:config/azure-storage-config.properties")
@ConfigurationProperties(prefix = "blob-storage")
@Profile("AzureStorage")
public class AzureStorageConfig {

    private String connectionString;
    private String accountName;
    private String endpointSuffix;

    private String container;
    private String contractPath;

    private String checkoutTemplateContainer;
}
