package com.elmolino.dao;

import com.elmolino.util.Conexion;
import com.elmolino.util.SeguridadPassword;
import com.elmolino.util.SesionActual;

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

/** Reune las consultas y reglas para los modulos de gestion. */
public final class GestionDAO {

    // Indica que tipo de dato usa un campo y que editor debe mostrar la interfaz.
    public enum Tipo { TEXTO, ENTERO, DECIMAL, FECHA, HORA, BOOLEANO, OPCION, CLAVE }

    // Opcion con identificador para listas que se cargan desde otra tabla.
    public record Opcion(int id, String nombre) {
        @Override public String toString() { return nombre; }
    }

    // Describe un campo, su etiqueta, reglas y posibles opciones de seleccion.
    public record Campo(String columna, String etiqueta, Tipo tipo, boolean obligatorio,
                        boolean visible, List<String> opciones, String consultaOpciones) {
        public Campo(String columna, String etiqueta, Tipo tipo, boolean obligatorio) {
            this(columna, etiqueta, tipo, obligatorio, true, List.of(), null);
        }
    }

    // Agrupa los datos necesarios para presentar y guardar una seccion.
    public record Modulo(String titulo, String tabla, String id, List<Campo> campos) {
        public Modulo {
            campos = List.copyOf(campos);
        }
    }

    // Estos metodos cortos crean campos requeridos, opcionales y relacionados.
    private static Campo texto(String columna, String etiqueta, boolean requerido) {
        return new Campo(columna, etiqueta, Tipo.TEXTO, requerido);
    }
    private static Campo entero(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.ENTERO, true);
    }
    private static Campo decimal(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.DECIMAL, true);
    }
    private static Campo fecha(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.FECHA, true);
    }
    private static Campo hora(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.HORA, true);
    }
    private static Campo booleano(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.BOOLEANO, true);
    }
    private static Campo opcion(String columna, String etiqueta, List<String> valores) {
        return new Campo(columna, etiqueta, Tipo.OPCION, true, true, valores, null);
    }
    private static Campo opcionOpcional(String columna, String etiqueta, List<String> valores) {
        return new Campo(columna, etiqueta, Tipo.OPCION, false, true, valores, null);
    }
    private static Campo relacion(String columna, String etiqueta, String consultaSql) {
        return new Campo(columna, etiqueta, Tipo.OPCION, true, true, List.of(), consultaSql);
    }
    private static Campo relacionOpcional(String columna, String etiqueta, String consultaSql) {
        return new Campo(columna, etiqueta, Tipo.OPCION, false, true, List.of(), consultaSql);
    }

    // Catalogo de campos por modulo; los nombres de tabla y columna deben coincidir con la base.
    public static final Modulo ALOJAMIENTOS = new Modulo("Alojamientos", "alojamientos", "idAlojamiento", List.of(
            texto("numero", "Número", true),
            opcion("tipo", "Tipo", List.of("Habitación", "Cabaña", "Departamento")),
            entero("capacidad", "Capacidad"), decimal("precioPorNoche", "Precio por noche"),
            texto("descripcion", "Descripción", false),
            opcion("estado", "Estado", List.of("Disponible", "Reservado", "Ocupado", "En limpieza", "Mantenimiento"))));

    public static final Modulo ESPACIOS = new Modulo("Espacios recreativos", "EspacioRecreativo", "idEspacio", List.of(
            texto("nombre", "Nombre", true), texto("descripcion", "Descripción", true),
            texto("ubicacion", "Ubicación", true), hora("horarioApertura", "Abre (HH:mm)"),
            hora("horarioCierre", "Cierra (HH:mm)"), booleano("requiereReserva", "Requiere reserva")));

    public static final Modulo ACTIVIDADES = new Modulo("Actividades", "Actividad", "idActividad", List.of(
            texto("nombre", "Nombre", true), texto("descripcionCorta", "Descripción breve", false),
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
            opcion("rol", "Rol", List.of("ADMINISTRADOR", "RECEPCION", "COORDINADOR", "GERENCIA", "LIMPIEZA")),
            new Campo("password_hash", "Contraseña (dejar vacía para conservarla)", Tipo.CLAVE, false, false, List.of(), null)));

    private GestionDAO() { }

    // Valida el permiso antes de consultar o cambiar cualquier modulo.
    public static void validarAcceso(Modulo modulo) throws SQLException {
        if (SesionActual.get() == null || !tieneAccesoTabla(modulo.tabla(), SesionActual.get().getRol())) {
            throw new SQLException("Tu rol no tiene acceso a la sección " + modulo.titulo() + ".");
        }
    }

    public static void validarAccesoClientes() throws SQLException {
        validarAccesoTabla("cliente", "Clientes");
    }

    public static void validarAccesoAcompanantesReserva() throws SQLException {
        validarAccesoTabla("reservaAcompañante", "Acompañantes por reserva");
    }

    private static void validarAccesoTabla(String tabla, String seccion) throws SQLException {
        if (SesionActual.get() == null || !tieneAccesoTabla(tabla, SesionActual.get().getRol())) {
            throw new SQLException("Tu rol no tiene acceso a la sección " + seccion + ".");
        }
    }

    private static boolean tieneAccesoTabla(String tabla, String rol) {
        // Esta lista tambien protege las operaciones que llegan directo al DAO.
        return switch (rol) {
            case "ADMINISTRADOR" -> List.of("cliente", "usuarios", "reservas", "pago").contains(tabla);
            case "GERENCIA" -> List.of("cliente", "reservas", "pago", "alojamientos").contains(tabla);
            case "RECEPCION" -> List.of("cliente", "alojamientos", "pago", "EspacioRecreativo").contains(tabla);
            case "ADMINISTRACION" -> List.of("cliente", "acompañante", "alojamientos", "reservas", "pago", "reservaAcompañante").contains(tabla);
            case "COORDINADOR" -> List.of("EspacioRecreativo", "Actividad", "inscripcionActividad").contains(tabla);
            case "LIMPIEZA" -> "alojamientos".equals(tabla);
            default -> false;
        };
    }
    public static List<Campo> camposVisibles(Modulo modulo) {
        return modulo.campos().stream().filter(Campo::visible).toList();
    }

    public static List<Opcion> opciones(String consultaSql) throws SQLException {
        // Ejecuta la consulta que llena una lista desplegable.
        List<Opcion> resultado = new ArrayList<>();
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql);
             ResultSet resultados = sentencia.executeQuery()) {
            while (resultados.next()) {
                resultado.add(new Opcion(resultados.getInt(1), resultados.getString(2)));
            }
        }
        return resultado;
    }

    public static List<Object[]> listar(Modulo modulo) throws SQLException {
        // Arma la consulta de lista segun la definicion del modulo.
        validarAcceso(modulo);
        if ("Actividad".equals(modulo.tabla())) return listarActividades();
        List<Campo> visibles = camposVisibles(modulo);
        String columnas = visibles.stream().map(campo -> q(campo.columna())).reduce((acumulado, siguiente) -> acumulado + ", " + siguiente).orElse("");
        String consultaSql = "SELECT " + q(modulo.id()) + (columnas.isEmpty() ? "" : ", " + columnas)
                + " FROM " + q(modulo.tabla());
        if ("alojamientos".equals(modulo.tabla()) && esLimpieza()) {
            consultaSql += " WHERE tipo = 'Habitación'";
        }
        consultaSql += " ORDER BY " + q(modulo.id()) + " DESC";
        List<Object[]> filas = new ArrayList<>();
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql);
             ResultSet resultados = sentencia.executeQuery()) {
            while (resultados.next()) {
                Object[] fila = new Object[visibles.size() + 1];
                fila[0] = resultados.getInt(1);
                for (int indice = 0; indice < visibles.size(); indice++) {
                    fila[indice + 1] = resultados.getObject(indice + 2);
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    private static List<Object[]> listarActividades() throws SQLException {
        // Incluye cupos y estado calculados para mostrar informacion util de actividades.
        String consultaSql = "SELECT a.idActividad, a.nombre, a.descripcionCorta, a.horario, a.dias, a.momentoDia, "
                + "a.Duracion, a.sector, a.responsable, a.edadRecomendada, a.modalidad, a.activa, e.nombre, a.cupoMax, "
                + "COUNT(i.idInscripcion), GREATEST(a.cupoMax - COUNT(i.idInscripcion), 0), "
                + "CASE WHEN COUNT(i.idInscripcion) >= a.cupoMax THEN 'Cupo lleno' "
                + "WHEN a.activa = 0 THEN 'Inactiva' "
                + "WHEN a.cupoMax - COUNT(i.idInscripcion) <= 2 THEN 'Últimos lugares' "
                + "ELSE 'Disponible' END, a.precio, a.pago "
                + "FROM `Actividad` a "
                + "LEFT JOIN `EspacioRecreativo` e ON e.idEspacio = a.idEspacio "
                + "LEFT JOIN `inscripcionActividad` i ON i.idActividad = a.idActividad "
                + "GROUP BY a.idActividad, a.nombre, a.descripcionCorta, a.horario, a.dias, a.momentoDia, "
                + "a.Duracion, a.sector, a.responsable, a.edadRecomendada, a.modalidad, a.activa, "
                + "e.nombre, a.cupoMax, a.precio, a.pago "
                + "ORDER BY a.idActividad DESC";
        List<Object[]> filas = new ArrayList<>();
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql);
             ResultSet resultados = sentencia.executeQuery()) {
            while (resultados.next()) {
                int cupoMax = resultados.getInt(14);
                int inscriptos = resultados.getInt(15);
                filas.add(new Object[]{resultados.getInt(1), resultados.getString(2), resultados.getString(3), resultados.getString(4),
                        resultados.getString(5), etiquetaMomentoDia(resultados.getString(6)), resultados.getString(7), resultados.getString(8),
                        resultados.getString(9), etiquetaEdad(resultados.getString(10)), etiquetaModalidad(resultados.getString(11)),
                        resultados.getBoolean(12) ? "Sí" : "No", resultados.getString(13) == null ? "Sin asignar" : resultados.getString(13),
                        inscriptos + " / " + cupoMax, resultados.getInt(16), resultados.getString(17), resultados.getBigDecimal(18), resultados.getString(19)});
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
        // Carga todos los valores de una fila para abrir el formulario de edicion.
        validarAcceso(modulo);
        String columnas = modulo.campos().stream().map(campo -> q(campo.columna()))
                .reduce((acumulado, siguiente) -> acumulado + ", " + siguiente).orElse("");
        String consultaSql = "SELECT " + columnas + " FROM " + q(modulo.tabla())
                + " WHERE " + q(modulo.id()) + " = ?";
        if ("alojamientos".equals(modulo.tabla()) && esLimpieza()) {
            consultaSql += " AND tipo = 'Habitación'";
        }
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            sentencia.setInt(1, id);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) return Map.of();
                Map<String, Object> fila = new LinkedHashMap<>();
                for (Campo campo : modulo.campos()) {
                    Object valor = campo.tipo() == Tipo.BOOLEANO
                            ? resultados.getBoolean(campo.columna()) : resultados.getObject(campo.columna());
                    fila.put(campo.columna(), valor);
                }
                return fila;
            }
        }
    }

    public static void guardar(Modulo modulo, Integer id, Map<String, String> valores) throws SQLException {
        // Revisa permisos, convierte datos y guarda una fila nueva o sus cambios.
        validarAcceso(modulo);
        boolean rolLimpieza = esLimpieza();
        if (rolLimpieza && !"alojamientos".equals(modulo.tabla())) {
            throw new SQLException("El rol Limpieza solo puede actualizar habitaciones.");
        }
        if ("alojamientos".equals(modulo.tabla()) && id == null) {
            throw new SQLException("Los alojamientos se agregan desde la página web.");
        }
        // Una transaccion permite deshacer el guardado si falla una validacion o consulta.
        try (Connection conexion = Conexion.obtener()) {
            if (rolLimpieza) validarHabitacionDeLimpieza(conexion, id);
            conexion.setAutoCommit(false);
            try {
                Map<String, Object> datos = new LinkedHashMap<>();
                for (Campo campo : modulo.campos()) {
                    if (rolLimpieza && !"estado".equals(campo.columna())) continue;
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
                if ("alojamientos".equals(modulo.tabla()) && !rolLimpieza) {
                    validarEstadoSinPermiso(conexion, id, datos.get("estado"));
                }
                if ("reservas".equals(modulo.tabla())) validarReserva(conexion, datos, id);
                if ("Actividad".equals(modulo.tabla())) validarActividad(datos);
                if ("inscripcionActividad".equals(modulo.tabla()) && id == null) validarCupo(conexion, datos);
                if ("usuarios".equals(modulo.tabla())) validarEmail(valores.get("email"));

                boolean crear = id == null;
                String asignaciones = datos.keySet().stream().map(k -> q(k) + " = ?")
                        .reduce((acumulado, siguiente) -> acumulado + ", " + siguiente).orElseThrow();
                String consultaSql;
                if (crear) {
                    consultaSql = "INSERT INTO " + q(modulo.tabla()) + " SET " + asignaciones;
                } else {
                    consultaSql = "UPDATE " + q(modulo.tabla()) + " SET " + asignaciones + " WHERE " + q(modulo.id()) + " = ?";
                }
                try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
                    int indice = 1;
                    for (Object valor : datos.values()) sentencia.setObject(indice++, valor);
                    if (!crear) sentencia.setInt(indice, id);
                    sentencia.executeUpdate();
                }
                conexion.commit();
            } catch (Exception excepcion) {
                conexion.rollback();
                if (excepcion instanceof SQLException excepcionSql) throw excepcionSql;
                if (excepcion instanceof IllegalArgumentException excepcionArgumento) throw excepcionArgumento;
                throw new SQLException("No se pudo guardar el registro", excepcion);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    public static void eliminar(Modulo modulo, int id) throws SQLException {
        // Verifica permisos y relaciones antes de borrar el registro.
        validarAcceso(modulo);
        if (esLimpieza() && "alojamientos".equals(modulo.tabla())) {
            throw new SQLException("El rol Limpieza no puede eliminar habitaciones.");
        }
        if ("Actividad".equals(modulo.tabla())) {
            try (Connection conexion = Conexion.obtener();
                 PreparedStatement sentencia = conexion.prepareStatement(
                         "SELECT COUNT(*) FROM inscripcionActividad WHERE idActividad = ?")) {
                sentencia.setInt(1, id);
                try (ResultSet resultados = sentencia.executeQuery()) {
                    if (resultados.next() && resultados.getInt(1) > 0) {
                        throw new SQLException("No se puede eliminar una actividad que tiene inscripciones.");
                    }
                }
            }
        }
        String consultaSql = "DELETE FROM " + q(modulo.tabla()) + " WHERE " + q(modulo.id()) + " = ?";
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            sentencia.setInt(1, id);
            sentencia.executeUpdate();
        }
    }

    public static void cancelarReserva(int idReserva) throws SQLException {
        // Conserva la reserva y cambia su estado para mantener el historial.
        validarAcceso(RESERVAS);
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(
                     "UPDATE reservas SET estadoReserva = 'CANCELADA' WHERE idReserva = ?")) {
            sentencia.setInt(1, idReserva);
            if (sentencia.executeUpdate() == 0) throw new SQLException("No se encontró la reserva.");
        }
    }

    private static boolean esLimpieza() {
        return SesionActual.get() != null && "LIMPIEZA".equals(SesionActual.get().getRol());
    }

    private static void validarHabitacionDeLimpieza(Connection conexion, int id) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT tipo FROM alojamientos WHERE idAlojamiento = ?")) {
            sentencia.setInt(1, id);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next() || !"Habitación".equals(resultados.getString(1))) {
                    throw new IllegalArgumentException("El rol Limpieza solo puede actualizar habitaciones.");
                }
            }
        }
    }

    private static void validarEstadoSinPermiso(Connection conexion, Integer id, Object nuevoEstado) throws SQLException {
        if (id == null) return;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT estado FROM alojamientos WHERE idAlojamiento = ?")) {
            sentencia.setInt(1, id);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("No se encontró la habitación.");
                if (!java.util.Objects.equals(resultados.getString(1), nuevoEstado)) {
                    throw new IllegalArgumentException("Solo el rol Limpieza puede cambiar el estado de las habitaciones.");
                }
            }
        }
    }

    private static Object convertir(Campo campo, String valor) {
        // Convierte el texto del formulario al tipo que espera JDBC.
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

    private static void validarEmail(String correo) {
        if (correo == null || !correo.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Ingresá un email válido.");
        }
    }

    private static void validarReserva(Connection conexion, Map<String, Object> datos, Integer id) throws SQLException {
        // Evita fechas invalidas, exceso de ocupantes y cruces con otras reservas.
        LocalDate ingreso = ((Date) datos.get("fechaIngreso")).toLocalDate();
        LocalDate egreso = ((Date) datos.get("fechaEgreso")).toLocalDate();
        if (!egreso.isAfter(ingreso)) throw new IllegalArgumentException("El egreso debe ser posterior al ingreso.");
        int personas = (Integer) datos.get("cantidadPersonas");
        int idAlojamiento = (Integer) datos.get("idAlojamiento");
        BigDecimal precioNoche;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT capacidad, precioPorNoche FROM alojamientos WHERE idAlojamiento = ? FOR UPDATE")) {
            sentencia.setInt(1, idAlojamiento);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("El alojamiento seleccionado no existe.");
                if (personas < 1 || personas > resultados.getInt(1)) throw new IllegalArgumentException("La cantidad de personas supera la capacidad del alojamiento.");
                precioNoche = resultados.getBigDecimal(2);
            }
        }
        String consultaSql = "SELECT COUNT(*) FROM reservas WHERE idAlojamiento = ? AND estadoReserva NOT IN ('CANCELADA','CANCELADO') "
                + "AND fechaIngreso < ? AND fechaEgreso > ?" + (id == null ? "" : " AND idReserva <> ?");
        try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            sentencia.setInt(1, idAlojamiento);
            sentencia.setDate(2, Date.valueOf(egreso));
            sentencia.setDate(3, Date.valueOf(ingreso));
            if (id != null) sentencia.setInt(4, id);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next() && resultados.getInt(1) > 0) throw new IllegalArgumentException("Ese alojamiento ya tiene una reserva para parte de esas fechas.");
            }
        }
        BigDecimal total = (BigDecimal) datos.get("precioTotal");
        if (total.signum() < 0) throw new IllegalArgumentException("El precio total no puede ser negativo.");
        if (total.signum() == 0) {
            long noches = ChronoUnit.DAYS.between(ingreso, egreso);
            datos.put("precioTotal", precioNoche.multiply(BigDecimal.valueOf(noches)));
        }
    }

    private static void validarCupo(Connection conexion, Map<String, Object> datos) throws SQLException {
        // Bloquea la inscripcion si la actividad esta inactiva o ya no tiene lugares.
        int cupoMax;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT cupoMax, activa FROM Actividad WHERE idActividad = ? FOR UPDATE")) {
            sentencia.setInt(1, (Integer) datos.get("idActividad"));
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("La actividad seleccionada no existe.");
                cupoMax = resultados.getInt(1);
                if (!resultados.getBoolean(2)) throw new IllegalArgumentException("La actividad no está habilitada para inscripciones.");
            }
        }
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COUNT(*) FROM inscripcionActividad WHERE idActividad = ?")) {
            sentencia.setInt(1, (Integer) datos.get("idActividad"));
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next() && resultados.getInt(1) >= cupoMax) {
                    throw new IllegalArgumentException("La actividad alcanzó su cupo máximo.");
                }
            }
        }
    }

    private static void validarActividad(Map<String, Object> datos) {
        // Aplica limites basicos antes de guardar los datos de la actividad.
        if (((Integer) datos.get("cupoMax")) < 1) {
            throw new IllegalArgumentException("El cupo máximo debe ser al menos 1.");
        }
        if (((BigDecimal) datos.get("precio")).signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
    }

    private static String q(String identificador) {
        // Acepta solo nombres SQL simples antes de agregarlos a una consulta.
        if (!identificador.matches("[\\p{L}_][\\p{L}\\p{N}_]*")) {
            throw new IllegalArgumentException("Identificador SQL no válido.");
        }
        return "`" + identificador + "`";
    }
}
