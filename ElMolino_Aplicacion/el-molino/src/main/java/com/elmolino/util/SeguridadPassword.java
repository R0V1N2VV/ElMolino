package com.elmolino.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Crea y comprueba claves protegidas sin guardarlas como texto visible. */
public final class SeguridadPassword {

    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int ITERACIONES = 120_000;
    private static final int CANTIDAD_BITS = 256;
    private static final int CANTIDAD_BYTES_SAL = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SeguridadPassword() {
    }

    public static String crearHash(char[] contrasena) {
        // Genera una sal distinta y deriva una huella para guardar en la base.
        byte[] sal = new byte[CANTIDAD_BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] huella = derivar(contrasena, sal, ITERACIONES);
        return PREFIJO + "$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(huella);
    }

    /** Comprueba el formato seguro y permite migrar claves antiguas al iniciar sesion. */
    public static boolean verificar(char[] contrasena, String guardada) {
        if (guardada == null) {
            return false;
        }
        if (!guardada.startsWith(PREFIJO + "$")) {
            return MessageDigest.isEqual(
                    new String(contrasena).getBytes(java.nio.charset.StandardCharsets.UTF_8),
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
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] huellaGuardada = Base64.getDecoder().decode(partes[3]);
            byte[] huellaIngresada = derivar(contrasena, sal, iteraciones);
            return MessageDigest.isEqual(huellaGuardada, huellaIngresada);
        } catch (IllegalArgumentException excepcion) {
            return false;
        }
    }

    public static boolean necesitaMigracion(String guardada) {
        // Indica si la clave guardada aun necesita pasar al formato seguro.
        return guardada == null || !guardada.startsWith(PREFIJO + "$");
    }

    private static byte[] derivar(char[] contrasena, byte[] sal, int iteraciones) {
        // Aplica PBKDF2 con la sal y la cantidad de iteraciones indicada.
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, sal, iteraciones, CANTIDAD_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException excepcion) {
            throw new IllegalStateException("No se pudo proteger la contraseña", excepcion);
        } finally {
            especificacion.clearPassword();
        }
    }
}
