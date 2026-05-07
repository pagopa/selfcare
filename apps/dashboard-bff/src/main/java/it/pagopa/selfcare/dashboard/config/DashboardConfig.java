package it.pagopa.selfcare.dashboard.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/dashboard-config.properties")
@ConfigurationProperties(prefix = "dashboard")
@Data
@ToString
public class DashboardConfig {

    private BlobStorage blobStorage;

    @Data
    public static class BlobStorage {
        private String containerProduct;
        private String filepathProduct;
        private String connectionStringProduct;
    }
}
