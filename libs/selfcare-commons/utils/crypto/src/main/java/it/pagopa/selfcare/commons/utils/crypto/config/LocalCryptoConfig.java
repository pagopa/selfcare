package it.pagopa.selfcare.commons.utils.crypto.config;

import it.pagopa.selfcare.commons.utils.crypto.utils.CryptoUtils;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * {@link Configuration} class to read private and public key to use when performing crypto operations
 */
@Conditional(LocalCryptoCondition.class)
@Configuration
@Getter
public class LocalCryptoConfig {

    private final Certificate certificate;
    private final PrivateKey privateKey;

    public LocalCryptoConfig(
            @Value("${crypto.key.cert}") String cert,
            @Value("${crypto.key.private}") String pKey
    ) {
        try {
            if(!StringUtils.hasText(cert) || !StringUtils.hasText(pKey)){
                throw new IllegalStateException("Define private and cert values in order to perform locally sign operations");
            }

            certificate = CryptoUtils.getCertificate(cert);
            privateKey = CryptoUtils.getPrivateKey(pKey);
        } catch (Exception e) {
            throw new IllegalStateException("Something gone wrong while loading crypto private and public keys", e);
        }
    }
}
