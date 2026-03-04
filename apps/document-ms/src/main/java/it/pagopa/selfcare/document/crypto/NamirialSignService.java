package it.pagopa.selfcare.document.crypto;


import java.io.InputStream;

public interface NamirialSignService {
    byte[] pkcs7Signhash(InputStream is);
}
