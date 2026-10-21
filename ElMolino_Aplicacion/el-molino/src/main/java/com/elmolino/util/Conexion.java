package com.elmolino.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Centraliza la apertura de conexiones con la base.
 * Cada DAO usa y cierra la conexion con try-with-resources.
 */
public final class Conexion {

    private Conexion() {
    }

    public static Connection obtener() throws SQLException {
        // Lee los parametros locales sin guardar credenciales dentro del codigo.
        String servidor = Config.get("db.host");
        String puerto = Config.get("db.port");
        // Codifica los espacios del nombre para formar una URL JDBC valida.
        String nombreBase = Config.get("db.name").replace(" ", "%20");

        String direccion = "jdbc:mysql://" + servidor + ":" + puerto + "/" + nombreBase
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

        return DriverManager.getConnection(direccion, Config.get("db.user"), Config.get("db.password"));
    }
}
