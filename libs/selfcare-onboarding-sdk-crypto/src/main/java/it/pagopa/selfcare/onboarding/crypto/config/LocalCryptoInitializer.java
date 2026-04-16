package it.pagopa.selfcare.onboarding.crypto.config;

import it.pagopa.selfcare.onboarding.crypto.utils.CryptoUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocalCryptoInitializer {

    private final static Logger log = LoggerFactory.getLogger(LocalCryptoInitializer.class);

    private LocalCryptoInitializer(){}

    public static LocalCryptoConfig initializeConfig() {
        LocalCryptoConfig config = new LocalCryptoConfig();
        String cert = System.getenv("CRYPTO_CERT");
        String pKey = System.getenv("CRYPTO_PRIVATE_KEY");

        try {
            if (StringUtils.isBlank(cert) || StringUtils.isBlank(pKey)) {
                log.error("Define private and cert values in order to perform locally sign operations");
            } else {

                config.setCertificate(CryptoUtils.getCertificate(cert));
                config.setPrivateKey(CryptoUtils.getPrivateKey(pKey));
            }
        } catch (Exception e) {
            log.error("Something gone wrong while loading crypto private and public keys", e);
        }

        return config;
    }
}
