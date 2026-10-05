package com.elmolino.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Punto unico para abrir conexiones a la base.
 * Cada DAO pide una conexion, la usa dentro de un try-with-resources y se cierra sola.
 */
public final class Conexion {

    private Conexion() {
    }

    public static Connection obtener() throws SQLException {
        String host = Config.get("db.host");
        String puerto = Config.get("db.port");
        // El nombre de la base tiene un espacio, se codifica para que la URL sea valida
        String base = Config.get("db.name").replace(" ", "%20");

        String url = "jdbc:mysql://" + host + ":" + puerto + "/" + base
                + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

        return DriverManager.getConnection(url, Config.get("db.user"), Config.get("db.password"));
    }
}
