package it.pagopa.selfcare.mscore.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/core-config.properties")
@ConfigurationProperties(prefix = "mscore.mail-template.placeholders.onboarding")
@Data
@ToString
public class MailTemplateConfig {

    private String delegationNotificationPath;
    private String delegationUserNotificationPath;
    private String delegationPartnerName;

    private String institutionDescription;

    private String notificationProductName;
}
