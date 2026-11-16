package com.elmolino.dao;

import com.elmolino.model.Usuario;
import com.elmolino.util.SesionActual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Guarda y consulta el historial de cambios realizados desde la aplicacion. */
public final class AuditoriaDAO {

    public static final int TAMANO_PAGINA = 500;
    public record Pagina(List<Object[]> filas, int total) { }

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    private AuditoriaDAO() { }

    /**
     * Ejecuta la lógica de “capturar” en AuditoriaDAO.java.
     */
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

    /**
     * Valida y guarda la información en el destino correspondiente.
     */
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

    /**
     * Carga los datos que necesita esta parte del sistema.
     */
    public static Pagina listar(int offset) throws SQLException {
        validarAcceso();
        String consultaSql = "SELECT fechaHora, usuario, rolUsuario, accion, seccion, "
                + "idRegistroAfectado, datosAnteriores, datosNuevos, idRegistro FROM registroAuditoria ORDER BY idRegistro DESC LIMIT ? OFFSET ?";
        List<Object[]> filas = new ArrayList<>();
        try (Connection conexion = com.elmolino.util.Conexion.obtener()) {
            int total;
            try (PreparedStatement cuenta = conexion.prepareStatement("SELECT COUNT(*) FROM registroAuditoria");
                 ResultSet rsCuenta = cuenta.executeQuery()) {
                rsCuenta.next(); total = rsCuenta.getInt(1);
            }
            try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
                sentencia.setInt(1, TAMANO_PAGINA); sentencia.setInt(2, Math.max(0, offset));
                try (ResultSet resultados = sentencia.executeQuery()) {
                    while (resultados.next()) {
                        String accion = resultados.getString("accion");
                        String seccion = resultados.getString("seccion");
                        String id = resultados.getString("idRegistroAfectado");
                        String anterior = resultados.getString("datosAnteriores");
                        String nuevo = resultados.getString("datosNuevos");
                        Map<String, Object> datos = leerJson(nuevo == null || nuevo.isBlank() ? anterior : nuevo);
                        String descripcion = describir(conexion, resultados.getString("usuario"), resultados.getString("rolUsuario"),
                                accion, seccion, id, datos);
                        filas.add(new Object[]{resultados.getTimestamp("fechaHora"), descripcion,
                                resultados.getString("usuario"), resultados.getString("rolUsuario"), accion,
                                seccion, id, anterior, nuevo, resultados.getLong("idRegistro")});
                    }
                }
            }
            return new Pagina(filas, total);
        }
    }

    private static String describir(Connection conexion, String usuario, String rol, String accion,
                                    String seccion, String id, Map<String, Object> datos) throws SQLException {
        String actor = (usuario == null || usuario.isBlank() ? "Sistema" : usuario)
                + (rol == null || rol.isBlank() ? "" : " (" + etiquetaRol(rol) + ")");
        String verbo = switch (accion == null ? "" : accion) {
            case "CREAR" -> "creó"; case "EDITAR" -> "editó"; case "ELIMINAR" -> "eliminó";
            case "CANCELAR" -> "canceló"; case "DESHACER" -> "revirtió"; case "COBRAR_SALDO" -> "registró un cobro en";
            default -> accion == null ? "modificó" : accion.toLowerCase(java.util.Locale.ROOT);
        };
        String referencia = nombreRegistro(conexion, seccion, datos);
        return actor + " " + verbo + " " + seccion + " #" + (id == null ? "—" : id)
                + (referencia.isBlank() ? "" : ": " + referencia);
    }

    private static String etiquetaRol(String rol) {
        return switch (rol.toUpperCase(java.util.Locale.ROOT)) {
            case "RECEPCION" -> "Recepción"; case "ADMINISTRACION" -> "Administración";
            case "ADMINISTRADOR" -> "Administrador"; case "SUPERADMIN" -> "SuperAdmin";
            case "COORDINADOR" -> "Coordinación"; case "GERENCIA" -> "Gerencia";
            case "LIMPIEZA" -> "Limpieza"; default -> rol;
        };
    }

    /** Obtiene una referencia legible desde los datos auditados y, si hace falta, desde las tablas relacionadas. */
    private static String nombreRegistro(Connection conexion, String seccion, Map<String, Object> datos) throws SQLException {
        String nombre = texto(datos, "nombre");
        String apellido = texto(datos, "apellido");
        String persona = (apellido + ", " + nombre).replaceAll("^, |, $", "").trim();
        if (List.of("Clientes", "Usuarios", "Acompañantes").contains(seccion) && !persona.isBlank()) return persona;
        if ("Alojamientos".equals(seccion) && !texto(datos, "numero").isBlank()) return texto(datos, "numero");
        if ("Actividades".equals(seccion) && !nombre.isBlank()) return nombre;
        if ("Inscripciones".equals(seccion)) {
            String cliente = buscarNombre(conexion, "cliente", "idCliente", datos.get("idCliente"), "apellido", "nombre");
            String actividad = buscarValor(conexion, "Actividad", "idActividad", datos.get("idActividad"), "nombre");
            return unir(cliente, actividad, " · ");
        }
        if ("Reservas".equals(seccion)) return buscarNombre(conexion, "cliente", "idCliente", datos.get("idCliente"), "apellido", "nombre");
        if ("Pagos".equals(seccion)) {
            Object idReserva = datos.get("idReserva");
            String cliente = buscarValor(conexion, "reservas", "idReserva", idReserva, "idCliente");
            return buscarNombre(conexion, "cliente", "idCliente", cliente, "apellido", "nombre");
        }
        if ("Acompañantes por reserva".equals(seccion)) {
            return buscarNombre(conexion, "acompañante", "idAcompañante", datos.get("idAcompañante"), "apellido", "nombre");
        }
        return "";
    }

    private static String buscarNombre(Connection conexion, String tabla, String idColumna, Object id,
                                       String apellido, String nombre) throws SQLException {
        if (id == null) return "";
        String sql = "SELECT CONCAT(" + identificador(apellido) + ", ', ', " + identificador(nombre)
                + ") FROM " + identificador(tabla) + " WHERE " + identificador(idColumna) + "=?";
        try (PreparedStatement s = conexion.prepareStatement(sql)) {
            s.setObject(1, id); try (ResultSet rs = s.executeQuery()) { return rs.next() ? rs.getString(1) : ""; }
        }
    }

    private static String buscarValor(Connection conexion, String tabla, String idColumna, Object id,
                                      String columna) throws SQLException {
        if (id == null) return "";
        try (PreparedStatement s = conexion.prepareStatement("SELECT " + identificador(columna) + " FROM "
                + identificador(tabla) + " WHERE " + identificador(idColumna) + "=?")) {
            s.setObject(1, id); try (ResultSet rs = s.executeQuery()) { return rs.next() ? rs.getString(1) : ""; }
        }
    }

    private static String texto(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave); return valor == null ? "" : valor.toString();
    }
    private static String unir(String a, String b, String separador) {
        if (a.isBlank()) return b; if (b.isBlank()) return a; return a + separador + b;
    }

    /** Revierte cambios simples de datos, comprobando que el registro siga en el estado auditado. */
    public static void revertir(long idAuditoria) throws SQLException {
        validarAcceso();
        try (Connection conexion = com.elmolino.util.Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                String accion; String seccion; String idTexto; String anteriorJson; String nuevoJson;
                try (PreparedStatement consulta = conexion.prepareStatement(
                        "SELECT accion,seccion,idRegistroAfectado,datosAnteriores,datosNuevos FROM registroAuditoria WHERE idRegistro=? FOR UPDATE")) {
                    consulta.setLong(1, idAuditoria);
                    try (ResultSet rs = consulta.executeQuery()) {
                        if (!rs.next()) throw new SQLException("El cambio seleccionado ya no existe.");
                        accion = rs.getString(1); seccion = rs.getString(2); idTexto = rs.getString(3);
                        anteriorJson = rs.getString(4); nuevoJson = rs.getString(5);
                    }
                }
                String[] destino = destino(seccion);
                if (destino == null || !"EDITAR".equals(accion)) {
                    throw new SQLException("Por seguridad, el botón revierte ediciones de registros; las altas, bajas y cancelaciones requieren una operación específica.");
                }
                int id;
                try { id = Integer.parseInt(idTexto); }
                catch (NumberFormatException error) { throw new SQLException("El cambio no tiene un identificador simple reversible."); }
                Map<String, Object> anterior = leerJson(anteriorJson);
                Map<String, Object> nuevo = leerJson(nuevoJson);
                Map<String, Object> actual = capturar(conexion, destino[0], destino[1], id);
                if (actual.isEmpty() || !coincide(actual, nuevo) || anterior.isEmpty()) throw new SQLException("El registro cambió después de esta acción; no se revirtió para proteger datos posteriores.");
                if ("reservas".equals(destino[0])) GestionDAO.sincronizarActividadesDeReserva(conexion, id);
                else if ("acompañante".equals(destino[0])) GestionDAO.sincronizarActividadesDeAcompanante(conexion, id);
                actualizar(conexion, destino[0], destino[1], id, anterior);
                if ("reservas".equals(destino[0])) GestionDAO.sincronizarActividadesDeReserva(conexion, id);
                else if ("inscripcionActividad".equals(destino[0])) GestionDAO.sincronizarCargoActividad(conexion, id);
                else if ("acompañante".equals(destino[0])) GestionDAO.sincronizarActividadesDeAcompanante(conexion, id);
                Map<String, Object> restaurado = capturar(conexion, destino[0], destino[1], id);
                registrar(conexion, "DESHACER", seccion, idTexto, actual, restaurado);
                conexion.commit();
            } catch (Exception error) {
                conexion.rollback();
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No se pudo revertir el cambio.", error);
            } finally { conexion.setAutoCommit(true); }
        }
    }

    private static String[] destino(String seccion) {
        return switch (seccion) {
            case "Clientes" -> new String[]{"cliente", "idCliente"};
            case "Alojamientos" -> new String[]{"alojamientos", "idAlojamiento"};
            case "Actividades" -> new String[]{"Actividad", "idActividad"};
            case "Inscripciones" -> new String[]{"inscripcionActividad", "idInscripcion"};
            case "Acompañantes" -> new String[]{"acompañante", "idAcompañante"};
            case "Reservas" -> new String[]{"reservas", "idReserva"};
            default -> null;
        };
    }


    private static void actualizar(Connection c, String tabla, String idColumna, int id, Map<String, Object> datos) throws SQLException {
        Map<String, Object> campos = new LinkedHashMap<>(datos); campos.remove(idColumna);
        if (campos.isEmpty()) throw new SQLException("No hay campos anteriores para restaurar.");
        String sets = campos.keySet().stream().map(k -> identificador(k) + "=?").reduce((a,b) -> a + "," + b).orElseThrow();
        try (PreparedStatement s = c.prepareStatement("UPDATE " + identificador(tabla) + " SET " + sets + " WHERE " + identificador(idColumna) + "=?")) {
            int i=1; for (Object valor : campos.values()) s.setObject(i++, valor); s.setInt(i, id);
            if (s.executeUpdate() != 1) throw new SQLException("No se pudo restaurar el estado anterior.");
        }
    }

    private static boolean coincide(Map<String, Object> actual, Map<String, Object> esperado) {
        for (Map.Entry<String, Object> entrada : esperado.entrySet()) {
            if (!actual.containsKey(entrada.getKey())) continue;
            Object a=actual.get(entrada.getKey()), e=entrada.getValue();
            if (a instanceof Number && e instanceof Number) {
                if (new BigDecimal(a.toString()).compareTo(new BigDecimal(e.toString())) != 0) return false;
            } else if (!Objects.equals(a == null ? null : a.toString(), e == null ? null : e.toString())) return false;
        }
        return true;
    }

    /** Lee los objetos planos serializados por este DAO; los registros auditados no contienen arrays. */
    private static Map<String, Object> leerJson(String json) throws SQLException {
        Map<String, Object> resultado = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return resultado;
        int[] pos={0}; saltar(json,pos); if (tomar(json,pos)!='{') throw new SQLException("Formato de auditoría no compatible.");
        saltar(json,pos);
        while (pos[0] < json.length() && json.charAt(pos[0])!='}') {
            String clave=cadenaJson(json,pos); saltar(json,pos);
            if (tomar(json,pos)!=':') throw new SQLException("Formato de auditoría no compatible.");
            saltar(json,pos); char inicial=tomar(json,pos); Object valor;
            if (inicial=='"') { pos[0]--; valor=cadenaJson(json,pos); }
            else {
                int inicio=pos[0]-1; while(pos[0]<json.length() && json.charAt(pos[0])!=',' && json.charAt(pos[0])!='}') pos[0]++;
                String token=json.substring(inicio,pos[0]).trim();
                if ("null".equals(token)) valor=null; else if ("true".equals(token)||"false".equals(token)) valor=Boolean.valueOf(token);
                else try { valor=new BigDecimal(token); } catch(NumberFormatException ex) { throw new SQLException("Dato de auditoría no compatible."); }
            }
            resultado.put(clave, valor); saltar(json,pos); if (pos[0]<json.length() && json.charAt(pos[0])==',') { pos[0]++; saltar(json,pos); } else break;
        }
        return resultado;
    }
    private static String cadenaJson(String s, int[] p) throws SQLException {
        if (tomar(s,p)!='"') throw new SQLException("Formato de auditoría no compatible."); StringBuilder r=new StringBuilder();
        while(p[0]<s.length()) {
            char c=tomar(s,p);
            if(c=='"') return r.toString();
            if(c=='\\') {
                if(p[0]>=s.length()) break;
                char e=tomar(s,p);
                switch(e) {
                    case 'b' -> r.append('\b'); case 'f' -> r.append('\f'); case 'n' -> r.append('\n');
                    case 'r' -> r.append('\r'); case 't' -> r.append('\t'); case 'u' -> {
                        if (p[0]+4>s.length()) throw new SQLException("Escape Unicode incompleto en auditoría.");
                        try { r.append((char)Integer.parseInt(s.substring(p[0],p[0]+4),16)); }
                        catch (NumberFormatException error) { throw new SQLException("Escape Unicode no válido en auditoría."); }
                        p[0]+=4;
                    }
                    default -> r.append(e);
                }
            } else r.append(c);
        }
        throw new SQLException("Texto de auditoría incompleto.");
    }
    private static char tomar(String s,int[] p) throws SQLException { if(p[0]>=s.length()) throw new SQLException("Formato de auditoría incompleto."); return s.charAt(p[0]++); }
    private static void saltar(String s,int[] p) { while(p[0]<s.length() && Character.isWhitespace(s.charAt(p[0]))) p[0]++; }

    /**
     * Comprueba acceso y evita continuar cuando hay información inválida.
     */
    public static void validarAcceso() throws SQLException {
        Usuario usuario = SesionActual.get();
        if (usuario == null || !List.of("GERENCIA", "ADMINISTRACION", "ADMINISTRADOR", "SUPERADMIN")
                .contains(usuario.getRol())) {
            throw new SQLException("Tu rol no puede consultar el registro de auditoria.");
        }
    }

    /**
     * Ejecuta la lógica de “identificador” en AuditoriaDAO.java.
     */
    private static String identificador(String valor) {
        return "`" + valor.replace("`", "``") + "`";
    }

    /**
     * Comprueba es dato sensible y devuelve verdadero o falso según se cumpla la condición.
     */
    private static boolean esDatoSensible(String columna) {
        String normalizada = columna.toLowerCase();
        return normalizada.contains("password") || normalizada.contains("contrasena")
                || normalizada.contains("clave") || normalizada.contains("hash");
    }

    /**
     * Ejecuta la lógica de “a json” en AuditoriaDAO.java.
     */
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

    /**
     * Ejecuta la lógica de “cadena json” en AuditoriaDAO.java.
     */
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
