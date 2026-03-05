package it.pagopa.selfcare.onboarding.crypto;

import java.io.IOException;
import java.io.InputStream;

public class ArubaPkcs7HashSignServiceImpl implements Pkcs7HashSignService {

    private final ArubaSignService arubaSignService;

    public ArubaPkcs7HashSignServiceImpl(ArubaSignService arubaSignService) {
        this.arubaSignService = arubaSignService;
    }

    @Override
    public byte[] sign(InputStream is) throws IOException {
        return arubaSignService.pkcs7Signhash(is);
    }

    @Override
    public boolean returnsFullPdf() {
        return false;
    }
}
