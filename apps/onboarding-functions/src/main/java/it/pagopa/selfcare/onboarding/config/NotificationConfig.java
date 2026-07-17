package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Set;
import java.util.Map;

@ConfigMapping(prefix = "notification")
public interface NotificationConfig {
    Map<String, Consumer> consumers();
    Integer minutesThresholdForUpdateNotification();
    interface Consumer {
        String topic();
        String name();
        String key();
        Set<String> allowedInstitutionTypes();
        Set<String> allowedOrigins();
        @WithDefault("false")
        boolean includeRelatedDocuments();
    }
}
