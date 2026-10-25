package com.elmolino.dao;

import com.elmolino.model.Usuario;
import com.elmolino.util.SesionActual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Guarda y consulta el historial de cambios realizados desde la aplicacion. */
public final class AuditoriaDAO {

    private AuditoriaDAO() { }

    public static Map<String, Object> capturar(Connection conexion, String tabla, String columnaId, int id)
            throws SQLException {
        String consultaSql = "SELECT * FROM " + identificador(tabla) + " WHERE "
                + identificador(columnaId) + " = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) return Map.of();
                ResultSetMetaData metadatos = resultados.getMetaData();
                Map<String, Object> fila = new LinkedHashMap<>();
                for (int indice = 1; indice <= metadatos.getColumnCount(); indice++) {
                    String columna = metadatos.getColumnLabel(indice);
                    if (!esDatoSensible(columna)) fila.put(columna, resultados.getObject(indice));
                }
                return fila;
            }
        }
    }

    public static void registrar(Connection conexion, String accion, String seccion, String idAfectado,
                                 Map<String, ?> anterior, Map<String, ?> nuevo) throws SQLException {
        Usuario usuario = SesionActual.get();
        String nombre = usuario == null ? "Sistema" : (usuario.getNombre() + " " + usuario.getApellido()).trim();
        String rol = usuario == null ? "SISTEMA" : usuario.getRol();
        Integer idUsuario = usuario == null ? null : usuario.getIdUsuario();
        String consultaSql = "INSERT INTO registroAuditoria "
                + "(idUsuario, usuario, rolUsuario, accion, seccion, idRegistroAfectado, datosAnteriores, datosNuevos) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            if (idUsuario == null) sentencia.setNull(1, java.sql.Types.INTEGER);
            else sentencia.setInt(1, idUsuario);
            sentencia.setString(2, nombre.isBlank() ? "Sistema" : nombre);
            sentencia.setString(3, rol);
            sentencia.setString(4, accion);
            sentencia.setString(5, seccion);
            sentencia.setString(6, idAfectado);
            sentencia.setString(7, anterior == null || anterior.isEmpty() ? null : aJson(anterior));
            sentencia.setString(8, nuevo == null || nuevo.isEmpty() ? null : aJson(nuevo));
            sentencia.executeUpdate();
        }
    }

    public static List<Object[]> listar() throws SQLException {
        validarAcceso();
        String consultaSql = "SELECT fechaHora, usuario, rolUsuario, accion, seccion, "
                + "idRegistroAfectado, datosAnteriores, datosNuevos FROM registroAuditoria ORDER BY idRegistro DESC";
        List<Object[]> filas = new ArrayList<>();
        try (Connection conexion = com.elmolino.util.Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql);
             ResultSet resultados = sentencia.executeQuery()) {
            while (resultados.next()) {
                filas.add(new Object[]{resultados.getTimestamp("fechaHora"), resultados.getString("usuario"),
                        resultados.getString("rolUsuario"), resultados.getString("accion"),
                        resultados.getString("seccion"), resultados.getString("idRegistroAfectado"),
                        resultados.getString("datosAnteriores"), resultados.getString("datosNuevos")});
            }
        }
        return filas;
    }

    public static void validarAcceso() throws SQLException {
        Usuario usuario = SesionActual.get();
        if (usuario == null || !List.of("GERENCIA", "ADMINISTRACION", "ADMINISTRADOR", "SUPERADMIN")
                .contains(usuario.getRol())) {
            throw new SQLException("Tu rol no puede consultar el registro de auditoria.");
        }
    }

    private static String identificador(String valor) {
        return "`" + valor.replace("`", "``") + "`";
    }

    private static boolean esDatoSensible(String columna) {
        String normalizada = columna.toLowerCase();
        return normalizada.contains("password") || normalizada.contains("contrasena")
                || normalizada.contains("clave") || normalizada.contains("hash");
    }

    private static String aJson(Map<String, ?> datos) {
        StringBuilder json = new StringBuilder("{");
        boolean primero = true;
        for (Map.Entry<String, ?> entrada : datos.entrySet()) {
            if (esDatoSensible(entrada.getKey())) continue;
            if (!primero) json.append(',');
            json.append(cadenaJson(entrada.getKey())).append(':');
            Object valor = entrada.getValue();
            if (valor == null) json.append("null");
            else if (valor instanceof Number || valor instanceof Boolean) json.append(valor);
            else json.append(cadenaJson(valor.toString()));
            primero = false;
        }
        return json.append('}').toString();
    }

    private static String cadenaJson(String valor) {
        StringBuilder texto = new StringBuilder("\"");
        for (int indice = 0; indice < valor.length(); indice++) {
            char caracter = valor.charAt(indice);
            switch (caracter) {
                case '"' -> texto.append("\\\"");
                case '\\' -> texto.append("\\\\");
                case '\n' -> texto.append("\\n");
                case '\r' -> texto.append("\\r");
                case '\t' -> texto.append("\\t");
                default -> {
                    if (caracter < 0x20) texto.append(String.format("\\u%04x", (int) caracter));
                    else texto.append(caracter);
                }
            }
        }
        return texto.append('"').toString();
    }
}
