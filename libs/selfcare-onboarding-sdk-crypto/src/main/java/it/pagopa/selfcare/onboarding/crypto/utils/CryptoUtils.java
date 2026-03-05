package it.pagopa.selfcare.onboarding.crypto.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class CryptoUtils {

    private CryptoUtils() {}

    public static void createParentDirectoryIfNotExists(File destFile) {
        Path destDir = destFile.toPath().getParent();
        if (!Files.exists(destDir)) {
            try {
                Files.createDirectories(destDir);
            } catch (IOException var3) {
                throw new IllegalArgumentException(String.format("Something gone wrong while creating destination folder: %s", destDir), var3);
            }
        }

    }

    public static byte[] getDigest(InputStream is) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(is.readAllBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Something gone wrong selecting digest algorithm", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Something gone wrong while reading inputStream", e);
        }
    }

    public static X509Certificate getCertificate(String cert) throws IOException, CertificateException {
        try(
                InputStream is = new ByteArrayInputStream(cert.getBytes(StandardCharsets.UTF_8))
        ) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        }
    }

    public static RSAPrivateKey getPrivateKey(String privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        String keyStringFormat =  pemToString(privateKey);
        try(
                InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(keyStringFormat))
        ) {
            PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(is.readAllBytes());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(encodedKeySpec);
        }
    }

    public static String pemToString(String target) {
        return target
                .replaceAll("^-----BEGIN[A-Z|\\s]+-----", "")
                .replaceAll("\\n+", "")
                .replaceAll("-----END[A-Z|\\s]+-----$", "");
    }
}
