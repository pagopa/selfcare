package it.pagopa.selfcare.onboarding.crypto;


import java.io.InputStream;

public interface NamirialSignService {
    byte[] pkcs7Signhash(InputStream is);
}