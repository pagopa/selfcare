package it.pagopa.selfcare.institution.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "institution-send-mail.scheduler")
public interface ProductConfig {

    Integer pecNotificationFrequency();

}
