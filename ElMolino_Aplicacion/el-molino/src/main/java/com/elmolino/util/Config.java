package com.elmolino.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Lee config.properties desde la carpeta donde se inicia el programa.
 * Ese archivo guarda los datos locales de conexion y no se incluye en el repositorio.
 */
public final class Config {

    private static final String ARCHIVO = "config.properties";
    private static Properties propiedades;

    private Config() {
    }

    private static synchronized Properties cargar() {
        if (propiedades == null) {
            // Carga el archivo una sola vez y conserva sus valores en memoria.
            Path ruta = Paths.get(ARCHIVO).toAbsolutePath();
            Properties propiedadesCargadas = new Properties();
            try (InputStreamReader lector = new InputStreamReader(
                    new FileInputStream(ruta.toFile()), StandardCharsets.UTF_8)) {
                propiedadesCargadas.load(lector);
            } catch (IOException excepcion) {
                throw new IllegalStateException(
                    "No se encontró el archivo de configuración. Se buscó en: " + ruta
                    + "\nCopiá config.properties.example como config.properties en esa carpeta "
                    + "y completalo con los datos de la base.", excepcion);
            }
            propiedades = propiedadesCargadas;
        }
        return propiedades;
    }

    public static String get(String clave) {
        // Revisa que la clave exista para avisar si falta una configuracion.
        String valor = cargar().getProperty(clave);
        if (valor == null) {
            throw new IllegalStateException("Falta la clave '" + clave + "' en " + ARCHIVO);
        }
        return valor.trim();
    }
}
