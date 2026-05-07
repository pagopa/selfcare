package it.pagopa.selfcare.dashboard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt.exchange")
public class ExchangeTokenProperties {
    private String signingKey;
    private String duration;
    private String kid;
    private String issuer;
    private String billingUrl;
    private String billingAudience;
}
