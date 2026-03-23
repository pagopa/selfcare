package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import it.pagopa.selfcare.party.registry_proxy.connector.exception.InternalException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyGeneratorTest {

    private String privateKey;
    private String publicKey;

    @Test
    void getPrivateKeySuccess() throws IOException {
        privateKey = new String(Files.readAllBytes(Paths.get("src/test/resources/private_key.txt")));
        assertNotNull(KeyGenerator.getPrivateKey(privateKey));
    }

    @Test
    void getPrivateKeyError() {
        privateKey = "testtesttesttesttesttesttesttesttest";
        assertThrows(InternalException.class, () -> KeyGenerator.getPrivateKey(privateKey));
    }

    @Test
    void getPublicKeySuccess() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        publicKey = new String(Files.readAllBytes(Paths.get("src/test/resources/public_key.txt")));
        assertNotNull(KeyGenerator.getPublicKey(publicKey));
    }

    @Test
    void getPublicKeyError() {
        publicKey = "testtesttesttesttesttesttesttesttest";
        assertThrows(InvalidKeySpecException.class, () -> KeyGenerator.getPublicKey(publicKey));
    }
}