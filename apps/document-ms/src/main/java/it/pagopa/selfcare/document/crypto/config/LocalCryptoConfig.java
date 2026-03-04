package it.pagopa.selfcare.document.crypto.config;

import lombok.Getter;
import lombok.Setter;

import java.security.PrivateKey;
import java.security.cert.Certificate;

@Setter
@Getter
public class LocalCryptoConfig {

    private Certificate certificate;
    private PrivateKey privateKey;

}
