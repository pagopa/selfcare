package it.pagopa.selfcare.document.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "document-ms.pagopa-signature")
public interface PagoPaSignatureConfig {

    String source();

    String signer();

    String location();

    String applyOnboardingTemplateReason();

}
