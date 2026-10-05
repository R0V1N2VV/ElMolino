package com.elmolino.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Utilidades para almacenar y verificar contraseñas sin guardarlas en texto visible. */
public final class SeguridadPassword {

    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int ITERACIONES = 120_000;
    private static final int BITS = 256;
    private static final int BYTES_SALT = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SeguridadPassword() {
    }

    public static String crearHash(char[] password) {
        byte[] salt = new byte[BYTES_SALT];
        ALEATORIO.nextBytes(salt);
        byte[] hash = derivar(password, salt, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Acepta hashes PBKDF2 y, temporalmente, claves antiguas guardadas en texto plano. */
    public static boolean verificar(char[] password, String guardada) {
        if (guardada == null) {
            return false;
        }
        if (!guardada.startsWith(PREFIJO + "$")) {
            return MessageDigest.isEqual(
                    new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    guardada.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try {
            String[] partes = guardada.split("\\$", -1);
            if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
                return false;
            }
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < 1 || iteraciones > 2_000_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] hashGuardado = Base64.getDecoder().decode(partes[3]);
            byte[] hashIngresado = derivar(password, salt, iteraciones);
            return MessageDigest.isEqual(hashGuardado, hashIngresado);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean necesitaMigracion(String guardada) {
        return guardada == null || !guardada.startsWith(PREFIJO + "$");
    }

    private static byte[] derivar(char[] password, byte[] salt, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iteraciones, BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo proteger la contraseña", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
