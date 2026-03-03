package it.pagopa.selfcare.onboarding.crypto.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DataEncryptionUtils {

    private static String defaultKey;
    private static String defaultIv;

    private static SecretKey getKey(String key) {
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(raw, "AES");
    }

    private static byte[] getIv(String iv) {
        return iv.getBytes(StandardCharsets.UTF_8);
    }

    public static void setDefaultIv(String iv) {
        defaultIv = iv;
    }

    public static void setDefaultKey(String key) {
        defaultKey = key;
    }

    public static String encrypt(String plain) {
        return encrypt(plain, defaultKey, defaultIv);
    }

    public static String decrypt(String cipherText) {
        return decrypt(cipherText, defaultKey, defaultIv);
    }

    public static String encrypt(String plain, String key, String iv) {
        if (plain == null) return null;
        try {
            byte[] ivBytes = getIv(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(key), new GCMParameterSpec(128, ivBytes));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(ct);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String cipherText, String key, String iv) {
        if (cipherText == null) return null;
        try {
            byte[] ct = Base64.getDecoder().decode(cipherText);
            byte[] ivBytes = getIv(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(key), new GCMParameterSpec(128, ivBytes));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
