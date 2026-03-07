package com.example.app.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class AesPasswordUtil {
    private static final String PREFIX = "ENC:";

    private AesPasswordUtil() {
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static String encrypt(String plainText, String secret) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }

        try {
            SecretKeySpec key = buildKey(secret);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Passwort konnte nicht verschlüsselt werden", e);
        }
    }

    public static String decrypt(String encryptedText, String secret) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }

        if (!isEncrypted(encryptedText)) {
            return encryptedText;
        }

        try {
            String payload = encryptedText.substring(PREFIX.length());
            SecretKeySpec key = buildKey(secret);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(payload));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Passwort konnte nicht entschlüsselt werden", e);
        }
    }

    private static SecretKeySpec buildKey(String secret) throws Exception {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("AES Secret fehlt. Bitte APP_AES_SECRET setzen.");
        }

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));

        byte[] key16 = new byte[16];
        System.arraycopy(key, 0, key16, 0, 16);

        return new SecretKeySpec(key16, "AES");
    }
}
