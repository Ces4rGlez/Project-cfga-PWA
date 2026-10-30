package com.proyecto.servicios.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionUtilTest {

    private final AesEncryptionUtil aesEncryptionUtil = new AesEncryptionUtil(
            AesEncryptionUtil.DEFAULT_SECRET_KEY,
            AesEncryptionUtil.DEFAULT_IV
    );

    @Test
    @DisplayName("Debe cifrar y descifrar correctamente un JSON payload con AES-256-CBC")
    void testEncryptAndDecrypt() {
        String testJson = "{\"idServicio\":133,\"idProducto\":582,\"telefono\":\"5555555551\",\"horaLocal\":\"20261023120000\",\"upc\":\"UPC12345\",\"unidad\":\"083\"}";

        String encrypted = aesEncryptionUtil.encrypt(testJson);
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
        assertNotEquals(testJson, encrypted);

        String decrypted = aesEncryptionUtil.decrypt(encrypted);
        assertEquals(testJson, decrypted);
    }
}
