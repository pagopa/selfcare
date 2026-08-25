package it.pagopa.selfcare.commons.utils.crypto.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/cryptoConfig.properties")
public class CryptoConfig {
}
