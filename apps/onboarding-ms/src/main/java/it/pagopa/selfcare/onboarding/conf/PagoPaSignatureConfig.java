package it.pagopa.selfcare.onboarding.conf;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "onboarding-ms.pagopa-signature")
public interface PagoPaSignatureConfig {

    String source();

    String signer();

    String location();

    String applyOnboardingTemplateReason();

}
