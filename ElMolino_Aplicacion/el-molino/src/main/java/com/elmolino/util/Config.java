package com.elmolino.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Lee config.properties desde la carpeta desde la que se ejecuta el programa
 * (la raiz del proyecto). Ese archivo no se sube a Git, cada integrante tiene el suyo.
 */
public final class Config {

    private static final String ARCHIVO = "config.properties";
    private static Properties props;

    private Config() {
    }

    private static synchronized Properties cargar() {
        if (props == null) {
            Path ruta = Paths.get(ARCHIVO).toAbsolutePath();
            Properties p = new Properties();
            try (InputStreamReader in = new InputStreamReader(
                    new FileInputStream(ruta.toFile()), StandardCharsets.UTF_8)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException(
                    "No se encontró el archivo de configuración. Se buscó en: " + ruta
                    + "\nCopiá config.properties.example como config.properties en esa carpeta "
                    + "y completalo con los datos de la base.", e);
            }
            props = p;
        }
        return props;
    }

    public static String get(String clave) {
        String valor = cargar().getProperty(clave);
        if (valor == null) {
            throw new IllegalStateException("Falta la clave '" + clave + "' en " + ARCHIVO);
        }
        return valor.trim();
    }
}
