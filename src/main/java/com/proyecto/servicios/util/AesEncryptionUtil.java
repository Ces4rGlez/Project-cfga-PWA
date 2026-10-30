package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utilería de Cifrado AES-256-CBC según la especificación de PuntoRed / GestoPago.
 * Todas las peticiones al método sendTx deben enviarse cifradas en el parámetro "signed".
 */
@Slf4j
@Component
public class AesEncryptionUtil {

    private final String secretKeyConfig;
    private final String ivConfig;

    // Valores por defecto de prueba provistos por PuntoRed / GestoPago
    public static final String DEFAULT_SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    public static final String DEFAULT_IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    public AesEncryptionUtil(
            @Value("${gestopago.crypto.secret-key:" + DEFAULT_SECRET_KEY + "}") String secretKeyConfig,
            @Value("${gestopago.crypto.iv:" + DEFAULT_IV + "}") String ivConfig) {
        this.secretKeyConfig = secretKeyConfig;
        this.ivConfig = ivConfig;
    }

    /**
     * Cifra un texto en claro (JSON payload) usando la llave y el IV configurados.
     *
     * @param plainText JSON en texto plano
     * @return Cadena cifrada codificada en Base64
     */
    public String encrypt(String plainText) {
        return encrypt(plainText, this.secretKeyConfig, this.ivConfig);
    }

    /**
     * Descifra un texto cifrado en Base64 usando la llave y el IV configurados.
     *
     * @param cipherText Texto cifrado en Base64
     * @return Texto descifrado en claro
     */
    public String decrypt(String cipherText) {
        return decrypt(cipherText, this.secretKeyConfig, this.ivConfig);
    }

    /**
     * Método estático para cifrar utilizando llaves e IV específicos (AES-256-CBC / PKCS5Padding).
     */
    public static String encrypt(String input, String keyBase64, String ivBase64) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] key = Base64.getDecoder().decode(keyBase64);

            IvParameterSpec ivspec = new IvParameterSpec(iv);
            SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);

            byte[] encryptedBytes = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error al cifrar payload con AES: {}", e.getMessage(), e);
            throw new RuntimeException("Error en proceso de cifrado AES: " + e.getMessage(), e);
        }
    }

    /**
     * Método estático para descifrar utilizando llaves e IV específicos (AES-256-CBC / PKCS5Padding).
     */
    public static String decrypt(String input, String keyBase64, String ivBase64) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] key = Base64.getDecoder().decode(keyBase64);

            IvParameterSpec ivspec = new IvParameterSpec(iv);
            SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);

            byte[] decodedBytes = Base64.getDecoder().decode(input);
            byte[] originalBytes = cipher.doFinal(decodedBytes);
            return new String(originalBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al descifrar payload con AES: {}", e.getMessage(), e);
            throw new RuntimeException("Error en proceso de descifrado AES: " + e.getMessage(), e);
        }
    }
}
