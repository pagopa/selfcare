package it.pagopa.selfcare.commons.connector.soap.aruba.sign.config;

import it.pagopa.selfcare.commons.connector.soap.aruba.sign.generated.client.Auth;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/arubaSignServiceConfiguration.properties")
@ConfigurationProperties("aruba.sign-service")
@Data
public class ArubaSignConfig {
    private String baseUrl;
    private Integer connectTimeoutMs;
    private Integer requestTimeoutMs;
    private Auth auth;
}
