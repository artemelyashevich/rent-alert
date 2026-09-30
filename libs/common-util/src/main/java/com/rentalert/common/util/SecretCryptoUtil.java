package com.rentalert.common.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretCryptoUtil {

    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    @SneakyThrows
    public static KeyPair generateKeyPair() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        return generator.generateKeyPair();
    }

    @SneakyThrows
    public static EncryptedPayload encrypt(String plainText, PublicKey publicKey) {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);
        SecretKey aesKey = keyGenerator.generateKey();
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] encryptedData = aesCipher.doFinal(plainText.getBytes());
        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());
        return new EncryptedPayload(
            Base64.getEncoder().encodeToString(encryptedAesKey),
            Base64.getEncoder().encodeToString(iv),
            Base64.getEncoder().encodeToString(encryptedData)
        );
    }

    @SneakyThrows
    public static String decrypt(EncryptedPayload payload, PrivateKey privateKey) {
        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION);
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] aesKeyBytes = rsaCipher.doFinal(Base64.getDecoder().decode(payload.encryptedKey()));
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        byte[] iv = Base64.getDecoder().decode(payload.iv());
        byte[] cipherText = Base64.getDecoder().decode(payload.encryptedData());
        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] plainBytes = aesCipher.doFinal(cipherText);
        return new String(plainBytes);
    }

    public record EncryptedPayload(
        String encryptedKey,
        String iv,
        String encryptedData
    ) {

    }
}