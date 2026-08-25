package it.pagopa.selfcare.commons.connector.soap.aruba.sign.service;

import it.pagopa.selfcare.commons.connector.soap.aruba.sign.config.ArubaCryptoCondition;
import it.pagopa.selfcare.commons.utils.crypto.service.Pkcs7HashSignService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Conditional(ArubaCryptoCondition.class)
@Service
public class ArubaPkcs7HashSignServiceImpl implements Pkcs7HashSignService {

    private final ArubaSignService arubaSignService;

    public ArubaPkcs7HashSignServiceImpl(ArubaSignService arubaSignService) {
        this.arubaSignService = arubaSignService;
    }

    @Override
    public byte[] sign(InputStream is) throws IOException {
        return arubaSignService.pkcs7Signhash(is);
    }
}
