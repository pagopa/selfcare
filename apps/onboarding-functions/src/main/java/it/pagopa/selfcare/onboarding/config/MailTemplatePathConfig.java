package it.pagopa.selfcare.onboarding.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "onboarding-functions.mail-template.path.onboarding")
public interface MailTemplatePathConfig {

    String completePath();
    String completePathFd();
    String completePathPt();
    String completePathUser();
    String completePathAggregate();

    String autocompletePath();

    String delegationNotificationPath();

    String registrationPath();
    String registrationUserPath();
    String registrationUserNewManagerPath();
    String registrationAggregatorPath();

    String onboardingApprovePath();

    String rejectPath();
    String registrationRequestPath();
    String registrationApprovePath();

    @WithDefault("contracts/template/mail/onboarding-deleted-ced/1.0.0.json")
    String deletePath();
}
