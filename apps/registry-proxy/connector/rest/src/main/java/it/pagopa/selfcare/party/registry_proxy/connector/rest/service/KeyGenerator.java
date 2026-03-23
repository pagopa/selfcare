package it.pagopa.selfcare.party.registry_proxy.connector.rest.service;

import it.pagopa.selfcare.party.registry_proxy.connector.exception.InternalException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
public class KeyGenerator {
    private KeyGenerator() {}
    public static RSAPrivateKey getPrivateKey(String privateKey)  {
        privateKey = privateKey.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s+","");
        byte [] pKeyEncoded = Base64.decode(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pKeyEncoded);
        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new InternalException(e);
        }
    }

    public static RSAPublicKey getPublicKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        key = key.replaceAll("\\s+","");
        key = key.replace("-----BEGINPUBLICKEY-----", "").replace("-----ENDPUBLICKEY-----", "");
        byte[] keyBytes = Base64.decode(key.getBytes(StandardCharsets.UTF_8));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }
}
