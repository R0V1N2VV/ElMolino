package com.elmolino.dao;

import com.elmolino.util.Conexion;
import com.elmolino.util.SeguridadPassword;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consultas parametrizadas de los módulos de gestión del complejo. */
public final class GestionDAO {

    public enum Tipo { TEXTO, ENTERO, DECIMAL, FECHA, HORA, BOOLEANO, OPCION, CLAVE }

    public record Opcion(int id, String nombre) {
        @Override public String toString() { return nombre; }
    }

    public record Campo(String columna, String etiqueta, Tipo tipo, boolean obligatorio,
                        boolean visible, List<String> opciones, String consultaOpciones) {
        public Campo(String columna, String etiqueta, Tipo tipo, boolean obligatorio) {
            this(columna, etiqueta, tipo, obligatorio, true, List.of(), null);
        }
    }

    public record Modulo(String titulo, String tabla, String id, List<Campo> campos) {
        public Modulo {
            campos = List.copyOf(campos);
        }
    }

    private static Campo texto(String col, String label, boolean requerido) {
        return new Campo(col, label, Tipo.TEXTO, requerido);
    }
    private static Campo entero(String col, String label) {
        return new Campo(col, label, Tipo.ENTERO, true);
    }
    private static Campo decimal(String col, String label) {
        return new Campo(col, label, Tipo.DECIMAL, true);
    }
    private static Campo fecha(String col, String label) {
        return new Campo(col, label, Tipo.FECHA, true);
    }
    private static Campo hora(String col, String label) {
        return new Campo(col, label, Tipo.HORA, true);
    }
    private static Campo booleano(String col, String label) {
        return new Campo(col, label, Tipo.BOOLEANO, true);
    }
    private static Campo opcion(String col, String label, List<String> values) {
        return new Campo(col, label, Tipo.OPCION, true, true, values, null);
    }
    private static Campo opcionOpcional(String col, String label, List<String> values) {
        return new Campo(col, label, Tipo.OPCION, false, true, values, null);
    }
    private static Campo relacion(String col, String label, String sql) {
        return new Campo(col, label, Tipo.OPCION, true, true, List.of(), sql);
    }
    private static Campo relacionOpcional(String col, String label, String sql) {
        return new Campo(col, label, Tipo.OPCION, false, true, List.of(), sql);
    }

    public static final Modulo ALOJAMIENTOS = new Modulo("Alojamientos", "alojamientos", "idAlojamiento", List.of(
            texto("numero", "Número", true),
            opcion("tipo", "Tipo", List.of("Habitación", "Cabaña", "Departamento")),
            entero("capacidad", "Capacidad"), decimal("precioPorNoche", "Precio por noche"),
            opcion("estado", "Estado", List.of("Disponible", "Reservado", "Ocupado", "En limpieza", "Mantenimiento"))));

    public static final Modulo ESPACIOS = new Modulo("Espacios recreativos", "EspacioRecreativo", "idEspacio", List.of(
            texto("nombre", "Nombre", true), texto("descripcion", "Descripción", true),
            texto("ubicacion", "Ubicación", true), hora("horarioApertura", "Abre (HH:mm)"),
            hora("horarioCierre", "Cierra (HH:mm)"), booleano("requiereReserva", "Requiere reserva")));

    public static final Modulo CATEGORIAS_ACTIVIDAD = new Modulo(
            "Categorías de actividades", "CategoriaActividad", "idCategoria", List.of(
            texto("nombre", "Categoría", true), texto("descripcion", "Descripción", true),
            booleano("activa", "Activa")));

    public static final Modulo ACTIVIDADES = new Modulo("Actividades", "Actividad", "idActividad", List.of(
            texto("nombre", "Nombre", true),
            relacionOpcional("idCategoria", "Categoría",
                    "SELECT idCategoria, nombre FROM `CategoriaActividad` WHERE activa = 1 ORDER BY nombre"),
            texto("descripcionCorta", "Descripción breve", false),
            texto("descripcion", "Descripción completa", true), texto("requisitos", "Requisitos", false),
            texto("informacionImportante", "Información importante", false),
            texto("horario", "Horario", true), texto("dias", "Días", false),
            opcionOpcional("momentoDia", "Momento del día", List.of("manana", "tarde", "noche")),
            texto("Duracion", "Duración", true), texto("sector", "Sector", false),
            texto("responsable", "Responsable", false),
            opcion("edadRecomendada", "Edad recomendada", List.of("todas", "ninos", "adolescentes", "adultos")),
            opcion("modalidad", "Modalidad", List.of("incluida", "inscripcion")),
            booleano("activa", "Actividad activa"),
            relacionOpcional("idEspacio", "Espacio (opcional)", "SELECT idEspacio, nombre FROM `EspacioRecreativo` ORDER BY nombre"),
            entero("cupoMax", "Cupo máximo"), decimal("precio", "Precio"), texto("pago", "Forma de pago", true)));

    public static final Modulo RESERVAS = new Modulo("Reservas", "reservas", "idReserva", List.of(
            relacion("idCliente", "Cliente", "SELECT idCliente, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `cliente` ORDER BY apellido, nombre"),
            relacion("idAlojamiento", "Alojamiento", "SELECT idAlojamiento, CONCAT(numero, ' - ', tipo) FROM `alojamientos` ORDER BY numero"),
            fecha("fechaIngreso", "Ingreso (AAAA-MM-DD)"), fecha("fechaEgreso", "Egreso (AAAA-MM-DD)"),
            entero("cantidadPersonas", "Cantidad de personas"), texto("temporada", "Temporada", false),
            opcion("estadoReserva", "Estado", List.of("PENDIENTE", "CONFIRMADA", "CANCELADA", "FINALIZADA")),
            decimal("precioTotal", "Precio total (0 calcula base)")));

    public static final Modulo ACTIVIDAD_INSCRIPTOS = new Modulo("Inscripciones", "inscripcionActividad", "idInscripcion", List.of(
            relacion("idCliente", "Cliente", "SELECT idCliente, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `cliente` ORDER BY apellido, nombre"),
            relacion("idActividad", "Actividad", "SELECT idActividad, nombre FROM `Actividad` ORDER BY nombre"),
            fecha("fechaInscripcion", "Fecha (AAAA-MM-DD)")));

    public static final Modulo PAGOS = new Modulo("Pagos", "pago", "idPago", List.of(
            relacion("idReserva", "Reserva", "SELECT idReserva, CONCAT('Reserva ', idReserva, ' - cliente ', idCliente) FROM `reservas` ORDER BY idReserva DESC"),
            decimal("montoTotal", "Monto"), opcion("metodoPago", "Medio de pago", List.of("Efectivo", "Transferencia", "Tarjeta", "Otro")),
            fecha("fechaPago", "Fecha (AAAA-MM-DD)"), texto("comprobante", "Comprobante", false)));

    public static final Modulo ACOMPANANTES = new Modulo("Acompañantes", "acompañante", "idAcompañante", List.of(
            relacion("idCliente", "Cliente", "SELECT idCliente, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `cliente` ORDER BY apellido, nombre"),
            entero("dni", "DNI"), texto("nombre", "Nombre", true), texto("apellido", "Apellido", true),
            entero("edad", "Edad"), texto("parentesco", "Parentesco", true)));

    public static final Modulo USUARIOS = new Modulo("Usuarios", "usuarios", "idUsuario", List.of(
            texto("nombre", "Nombre", true), texto("apellido", "Apellido", true),
            new Campo("email", "Email", Tipo.TEXTO, true),
            opcion("rol", "Rol", List.of("cliente", "coordinador_actividades", "ADMINISTRADOR",
                    "RECEPCION", "ADMINISTRACION", "COORDINADOR", "GERENCIA")),
            new Campo("password_hash", "Contraseña (dejar vacía para conservarla)", Tipo.CLAVE, false, false, List.of(), null)));

    private GestionDAO() { }

    public static List<Campo> camposVisibles(Modulo modulo) {
        return modulo.campos().stream().filter(Campo::visible).toList();
    }

    public static List<Opcion> opciones(String sql) throws SQLException {
        List<Opcion> resultado = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(new Opcion(rs.getInt(1), rs.getString(2)));
            }
        }
        return resultado;
    }

    public static List<Object[]> listar(Modulo modulo) throws SQLException {
        if ("Actividad".equals(modulo.tabla())) return listarActividades();
        if ("inscripcionActividad".equals(modulo.tabla())) return listarInscripciones();
        List<Campo> visibles = camposVisibles(modulo);
        String columnas = visibles.stream().map(c -> q(c.columna())).reduce((a, b) -> a + ", " + b).orElse("");
        String sql = "SELECT " + q(modulo.id()) + (columnas.isEmpty() ? "" : ", " + columnas)
                + " FROM " + q(modulo.tabla()) + " ORDER BY " + q(modulo.id()) + " DESC";
        List<Object[]> filas = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] fila = new Object[visibles.size() + 1];
                fila[0] = rs.getInt(1);
                for (int i = 0; i < visibles.size(); i++) {
                    fila[i + 1] = rs.getObject(i + 2);
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    private static List<Object[]> listarActividades() throws SQLException {
        String sql = "SELECT a.idActividad, a.nombre, c.nombre, a.descripcionCorta, a.horario, a.dias, a.momentoDia, "
                + "a.Duracion, a.sector, a.responsable, a.edadRecomendada, a.modalidad, a.activa, e.nombre, a.cupoMax, "
                + "COUNT(i.idInscripcion), GREATEST(a.cupoMax - COUNT(i.idInscripcion), 0), "
                + "CASE WHEN COUNT(i.idInscripcion) >= a.cupoMax THEN 'Cupo lleno' "
                + "WHEN a.activa = 0 THEN 'Inactiva' "
                + "WHEN a.cupoMax - COUNT(i.idInscripcion) <= 2 THEN 'Últimos lugares' "
                + "ELSE 'Disponible' END, a.precio, a.pago "
                + "FROM `Actividad` a "
                + "LEFT JOIN `CategoriaActividad` c ON c.idCategoria = a.idCategoria "
                + "LEFT JOIN `EspacioRecreativo` e ON e.idEspacio = a.idEspacio "
                + "LEFT JOIN `inscripcionActividad` i ON i.idActividad = a.idActividad "
                + "GROUP BY a.idActividad, a.nombre, c.nombre, a.descripcionCorta, a.horario, a.dias, a.momentoDia, "
                + "a.Duracion, a.sector, a.responsable, a.edadRecomendada, a.modalidad, a.activa, "
                + "e.nombre, a.cupoMax, a.precio, a.pago "
                + "ORDER BY a.idActividad DESC";
        List<Object[]> filas = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int cupoMax = rs.getInt(15);
                int inscriptos = rs.getInt(16);
                filas.add(new Object[]{rs.getInt(1), rs.getString(2),
                        rs.getString(3) == null ? "Sin categoría" : rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), etiquetaMomentoDia(rs.getString(7)), rs.getString(8),
                        rs.getString(9), rs.getString(10), etiquetaEdad(rs.getString(11)),
                        etiquetaModalidad(rs.getString(12)), rs.getBoolean(13) ? "Sí" : "No",
                        rs.getString(14) == null ? "Sin asignar" : rs.getString(14),
                        inscriptos + " / " + cupoMax, rs.getInt(17), rs.getString(18),
                        rs.getBigDecimal(19), rs.getString(20)});
            }
        }
        return filas;
    }

    private static List<Object[]> listarInscripciones() throws SQLException {
        String sql = "SELECT i.idInscripcion, "
                + "CONCAT(c.apellido, ', ', c.nombre, ' - DNI ', c.dni), "
                + "a.nombre, i.fechaInscripcion "
                + "FROM `inscripcionActividad` i "
                + "INNER JOIN `cliente` c ON c.idCliente = i.idCliente "
                + "INNER JOIN `Actividad` a ON a.idActividad = i.idActividad "
                + "ORDER BY i.idInscripcion DESC";
        List<Object[]> filas = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDate(4)});
            }
        }
        return filas;
    }

    private static String etiquetaMomentoDia(String valor) {
        if (valor == null) return "";
        return switch (valor) {
            case "manana" -> "Mañana";
            case "tarde" -> "Tarde";
            case "noche" -> "Noche";
            default -> valor;
        };
    }

    private static String etiquetaEdad(String valor) {
        if (valor == null) return "";
        return switch (valor) {
            case "todas" -> "Todas las edades";
            case "ninos" -> "Niños";
            case "adolescentes" -> "Adolescentes";
            case "adultos" -> "Adultos";
            default -> valor;
        };
    }

    private static String etiquetaModalidad(String valor) {
        if (valor == null) return "";
        return switch (valor) {
            case "incluida" -> "Incluida";
            case "inscripcion" -> "Con inscripción";
            default -> valor;
        };
    }

    public static Map<String, Object> buscarPorId(Modulo modulo, int id) throws SQLException {
        String columnas = modulo.campos().stream().map(c -> q(c.columna()))
                .reduce((a, b) -> a + ", " + b).orElse("");
        String sql = "SELECT " + columnas + " FROM " + q(modulo.tabla())
                + " WHERE " + q(modulo.id()) + " = ?";
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Map.of();
                Map<String, Object> fila = new LinkedHashMap<>();
                for (Campo campo : modulo.campos()) {
                    Object valor = campo.tipo() == Tipo.BOOLEANO
                            ? rs.getBoolean(campo.columna()) : rs.getObject(campo.columna());
                    fila.put(campo.columna(), valor);
                }
                return fila;
            }
        }
    }

    public static void guardar(Modulo modulo, Integer id, Map<String, String> valores) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                Map<String, Object> datos = new LinkedHashMap<>();
                for (Campo campo : modulo.campos()) {
                    String valor = valores.getOrDefault(campo.columna(), "").trim();
                    if (campo.tipo() == Tipo.CLAVE && id != null && valor.isEmpty()) continue;
                    if (campo.tipo() == Tipo.CLAVE && valor.isEmpty()) {
                        throw new IllegalArgumentException("La contraseña es obligatoria al crear el usuario.");
                    }
                    if (valor.isEmpty() && campo.obligatorio()) {
                        throw new IllegalArgumentException("Completá el campo " + campo.etiqueta() + ".");
                    }
                    if (valor.isEmpty()) {
                        datos.put(campo.columna(), null);
                    } else {
                        datos.put(campo.columna(), convertir(campo, valor));
                    }
                }
                if (id == null && "inscripcionActividad".equals(modulo.tabla())) {
                    datos.put("asistencia", false);
                }
                if ("reservas".equals(modulo.tabla())) validarReserva(con, datos, id);
                if ("Actividad".equals(modulo.tabla())) validarActividad(datos);
                if ("inscripcionActividad".equals(modulo.tabla()) && id == null) validarCupo(con, datos);
                if ("usuarios".equals(modulo.tabla())) validarEmail(valores.get("email"));

                boolean crear = id == null;
                String asignaciones = datos.keySet().stream().map(k -> q(k) + " = ?")
                        .reduce((a, b) -> a + ", " + b).orElseThrow();
                String sql;
                if (crear) {
                    sql = "INSERT INTO " + q(modulo.tabla()) + " SET " + asignaciones;
                } else {
                    sql = "UPDATE " + q(modulo.tabla()) + " SET " + asignaciones + " WHERE " + q(modulo.id()) + " = ?";
                }
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    int indice = 1;
                    for (Object valor : datos.values()) ps.setObject(indice++, valor);
                    if (!crear) ps.setInt(indice, id);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                if (ex instanceof SQLException sqlEx) throw sqlEx;
                if (ex instanceof IllegalArgumentException argEx) throw argEx;
                throw new SQLException("No se pudo guardar el registro", ex);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public static void eliminar(Modulo modulo, int id) throws SQLException {
        if ("Actividad".equals(modulo.tabla())) {
            try (Connection con = Conexion.obtener();
                 PreparedStatement ps = con.prepareStatement(
                         "UPDATE Actividad SET activa = 0 WHERE idActividad = ?")) {
                ps.setInt(1, id);
                if (ps.executeUpdate() == 0) throw new SQLException("No se encontró la actividad.");
            }
            return;
        }
        String sql = "DELETE FROM " + q(modulo.tabla()) + " WHERE " + q(modulo.id()) + " = ?";
        try (Connection con = Conexion.obtener(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static void cancelarReserva(int idReserva) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE reservas SET estadoReserva = 'CANCELADA' WHERE idReserva = ?")) {
            ps.setInt(1, idReserva);
            if (ps.executeUpdate() == 0) throw new SQLException("No se encontró la reserva.");
        }
    }

    private static Object convertir(Campo campo, String valor) {
        return switch (campo.tipo()) {
            case TEXTO -> {
                if ("email".equals(campo.columna())) validarEmail(valor);
                yield valor;
            }
            case ENTERO -> Integer.valueOf(valor);
            case DECIMAL -> new BigDecimal(valor.replace(',', '.'));
            case FECHA -> Date.valueOf(LocalDate.parse(valor));
            case HORA -> Time.valueOf(LocalTime.parse(valor.length() == 5 ? valor + ":00" : valor));
            case BOOLEANO -> Boolean.valueOf(valor);
            case OPCION -> {
                if (campo.consultaOpciones() != null) yield Integer.valueOf(valor);
                if (!campo.opciones().contains(valor)) throw new IllegalArgumentException("Opción no válida para " + campo.etiqueta());
                yield valor;
            }
            case CLAVE -> SeguridadPassword.crearHash(valor.toCharArray());
        };
    }

    private static void validarEmail(String email) {
        if (email == null || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Ingresá un email válido.");
        }
    }

    private static void validarReserva(Connection con, Map<String, Object> datos, Integer id) throws SQLException {
        LocalDate ingreso = ((Date) datos.get("fechaIngreso")).toLocalDate();
        LocalDate egreso = ((Date) datos.get("fechaEgreso")).toLocalDate();
        if (!egreso.isAfter(ingreso)) throw new IllegalArgumentException("El egreso debe ser posterior al ingreso.");
        int personas = (Integer) datos.get("cantidadPersonas");
        int idAlojamiento = (Integer) datos.get("idAlojamiento");
        BigDecimal precioNoche;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT capacidad, precioPorNoche FROM alojamientos WHERE idAlojamiento = ? FOR UPDATE")) {
            ps.setInt(1, idAlojamiento);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("El alojamiento seleccionado no existe.");
                if (personas < 1 || personas > rs.getInt(1)) throw new IllegalArgumentException("La cantidad de personas supera la capacidad del alojamiento.");
                precioNoche = rs.getBigDecimal(2);
            }
        }
        String sql = "SELECT COUNT(*) FROM reservas WHERE idAlojamiento = ? AND estadoReserva NOT IN ('CANCELADA','CANCELADO') "
                + "AND fechaIngreso < ? AND fechaEgreso > ?" + (id == null ? "" : " AND idReserva <> ?");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAlojamiento);
            ps.setDate(2, Date.valueOf(egreso));
            ps.setDate(3, Date.valueOf(ingreso));
            if (id != null) ps.setInt(4, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) throw new IllegalArgumentException("Ese alojamiento ya tiene una reserva para parte de esas fechas.");
            }
        }
        BigDecimal total = (BigDecimal) datos.get("precioTotal");
        if (total.signum() < 0) throw new IllegalArgumentException("El precio total no puede ser negativo.");
        if (total.signum() == 0) {
            long noches = ChronoUnit.DAYS.between(ingreso, egreso);
            datos.put("precioTotal", precioNoche.multiply(BigDecimal.valueOf(noches)));
        }
    }

    private static void validarCupo(Connection con, Map<String, Object> datos) throws SQLException {
        int cupoMax;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT cupoMax, activa FROM Actividad WHERE idActividad = ? FOR UPDATE")) {
            ps.setInt(1, (Integer) datos.get("idActividad"));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("La actividad seleccionada no existe.");
                cupoMax = rs.getInt(1);
                if (!rs.getBoolean(2)) throw new IllegalArgumentException("La actividad no está habilitada para inscripciones.");
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM inscripcionActividad WHERE idActividad = ?")) {
            ps.setInt(1, (Integer) datos.get("idActividad"));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) >= cupoMax) {
                    throw new IllegalArgumentException("La actividad alcanzó su cupo máximo.");
                }
            }
        }
    }

    private static void validarActividad(Map<String, Object> datos) {
        if (((Integer) datos.get("cupoMax")) < 1) {
            throw new IllegalArgumentException("El cupo máximo debe ser al menos 1.");
        }
        if (((BigDecimal) datos.get("precio")).signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
    }

    private static String q(String identificador) {
        if (!identificador.matches("[\\p{L}_][\\p{L}\\p{N}_]*")) {
            throw new IllegalArgumentException("Identificador SQL no válido.");
        }
        return "`" + identificador + "`";
    }
}
