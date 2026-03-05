package it.pagopa.selfcare.onboarding.crypto;

import java.io.InputStream;

public class NamirialPkcs7HashSignServiceImpl implements Pkcs7HashSignService {

    private final NamirialSignService namirialSignService;

    public NamirialPkcs7HashSignServiceImpl(NamirialSignService namirialSignService) {
        this.namirialSignService = namirialSignService;
    }


    @Override
    public byte[] sign(InputStream is) {
        return namirialSignService.pkcs7Signhash(is);
    }


    @Override
    public boolean returnsFullPdf() {
        return true;
    }
}