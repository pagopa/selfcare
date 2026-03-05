package it.pagopa.selfcare.onboarding.crypto;

import java.io.InputStream;

/** It allows to perform signature requests towards Aruba. */
public interface ArubaSignService {
    byte[] hashSign(InputStream is);
    byte[] pkcs7Signhash(InputStream is);
}
