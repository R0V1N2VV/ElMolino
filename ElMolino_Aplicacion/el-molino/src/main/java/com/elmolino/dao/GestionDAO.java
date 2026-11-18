package com.elmolino.dao;

import com.elmolino.util.Conexion;
import com.elmolino.util.Config;
import com.elmolino.util.RelojDemostracion;
import com.elmolino.util.SeguridadPassword;
import com.elmolino.util.SesionActual;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reune las consultas y reglas para los modulos de gestion. */
public final class GestionDAO {

    private static final LocalTime HORA_INGRESO = LocalTime.of(14, 0);
    private static final LocalTime HORA_EGRESO = LocalTime.of(10, 0);
    private static final Pattern HORARIO_CON_FIN = Pattern.compile(
            "\\b([01]?\\d|2[0-3]):([0-5]\\d)\\s*(?:a|hasta|-)\\s*([01]?\\d|2[0-3]):([0-5]\\d)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public enum Operacion { CREAR, EDITAR, ELIMINAR }

    // Indica que tipo de dato usa un campo y que editor debe mostrar la interfaz.
    public enum Tipo { TEXTO, ENTERO, DECIMAL, FECHA, HORA, BOOLEANO, OPCION, CLAVE }

    // Opcion con identificador para listas que se cargan desde otra tabla.
    public record Opcion(int id, String nombre) {
        /**
         * Devuelve el texto que representa al objeto cuando se muestra en listas, tablas o mensajes.
         */
        @Override public String toString() { return nombre; }
    }

    /** Resume el importe que se cobrará al terminar una estadía. */
    public record ResumenCobroFinal(String cliente, BigDecimal saldoAlojamiento,
                                    BigDecimal actividadesPendientes, BigDecimal total) { }

    // Describe un campo, su etiqueta, reglas y posibles opciones de seleccion.
    public record Campo(String columna, String etiqueta, Tipo tipo, boolean obligatorio,
                        boolean visible, List<String> opciones, String consultaOpciones) {
        /**
         * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
         */
        public Campo(String columna, String etiqueta, Tipo tipo, boolean obligatorio) {
            this(columna, etiqueta, tipo, obligatorio, true, List.of(), null);
        }
    }

    // Agrupa los datos necesarios para presentar y guardar una seccion.
    public record Modulo(String titulo, String tabla, String id, List<Campo> campos) {
        /**
         * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
         */
        public Modulo {
            campos = List.copyOf(campos);
        }
    }

    // Estos metodos cortos crean campos requeridos, opcionales y relacionados.
    /**
     * Ejecuta la lógica de “texto” en GestionDAO.java.
     */
    private static Campo texto(String columna, String etiqueta, boolean requerido) {
        return new Campo(columna, etiqueta, Tipo.TEXTO, requerido);
    }
    /**
     * Ejecuta la lógica de “entero” en GestionDAO.java.
     */
    private static Campo entero(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.ENTERO, true);
    }
    /**
     * Ejecuta la lógica de “decimal” en GestionDAO.java.
     */
    private static Campo decimal(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.DECIMAL, true);
    }
    /**
     * Ejecuta la lógica de “fecha” en GestionDAO.java.
     */
    private static Campo fecha(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.FECHA, true);
    }
    /**
     * Ejecuta la lógica de “hora” en GestionDAO.java.
     */
    private static Campo hora(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.HORA, true);
    }
    /**
     * Ejecuta la lógica de “booleano” en GestionDAO.java.
     */
    private static Campo booleano(String columna, String etiqueta) {
        return new Campo(columna, etiqueta, Tipo.BOOLEANO, true);
    }
    /**
     * Ejecuta la lógica de “opcion” en GestionDAO.java.
     */
    private static Campo opcion(String columna, String etiqueta, List<String> valores) {
        return new Campo(columna, etiqueta, Tipo.OPCION, true, true, valores, null);
    }
    /**
     * Ejecuta la lógica de “opcion opcional” en GestionDAO.java.
     */
    private static Campo opcionOpcional(String columna, String etiqueta, List<String> valores) {
        return new Campo(columna, etiqueta, Tipo.OPCION, false, true, valores, null);
    }
    /**
     * Ejecuta la lógica de “relacion” en GestionDAO.java.
     */
    private static Campo relacion(String columna, String etiqueta, String consultaSql) {
        return new Campo(columna, etiqueta, Tipo.OPCION, true, true, List.of(), consultaSql);
    }
    /**
     * Ejecuta la lógica de “relacion opcional” en GestionDAO.java.
     */
    private static Campo relacionOpcional(String columna, String etiqueta, String consultaSql) {
        return new Campo(columna, etiqueta, Tipo.OPCION, false, true, List.of(), consultaSql);
    }

    // Catalogo de campos por modulo; los nombres de tabla y columna deben coincidir con la base.
    public static final Modulo ALOJAMIENTOS = new Modulo("Alojamientos", "alojamientos", "idAlojamiento", List.of(
            texto("numero", "Número", true),
            opcion("tipo", "Tipo", List.of("Habitación", "Cabaña", "Hotel")),
            entero("capacidad", "Capacidad"), decimal("precioPorNoche", "Precio por noche"),
            texto("descripcion", "Descripción", false),
            opcion("estado", "Estado", List.of("Disponible", "Reservado", "Ocupado", "En limpieza", "En mantenimiento", "Fuera de servicio"))));

    public static final Modulo ACTIVIDADES = new Modulo("Actividades", "Actividad", "idActividad", List.of(
            texto("slug", "Identificador web (sin espacios)", true), texto("nombre", "Nombre", true),
            relacion("idCategoria", "Categoría", "SELECT idCategoria, nombre FROM `CategoriaActividad` WHERE activa = 1 ORDER BY nombre"),
            texto("descripcionCorta", "Descripción breve", false),
            texto("descripcion", "Descripción completa", true), texto("requisitos", "Requisitos", false),
            texto("informacionImportante", "Información importante", false),
            texto("horario", "Horario", true), texto("dias", "Días", true),
            opcionOpcional("momentoDia", "Momento del día", List.of("manana", "tarde", "noche")),
            texto("Duracion", "Duración", true), texto("sector", "Sector", false),
            texto("responsable", "Responsable", false),
            opcion("edadRecomendada", "Edad recomendada", List.of("todas", "ninos", "adolescentes", "adultos")),
            opcion("modalidad", "Modalidad", List.of("incluida", "inscripcion")),
            booleano("activa", "Actividad activa"),
            relacionOpcional("idEspacio", "Espacio (opcional)", "SELECT idEspacio, nombre FROM `EspacioRecreativo` ORDER BY nombre"),
            entero("cupoMax", "Cupo máximo"), decimal("precio", "Precio")));

    public static final Modulo RESERVAS = new Modulo("Reservas", "reservas", "idReserva", List.of(
            relacion("idCliente", "Cliente", "SELECT idCliente, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `cliente` ORDER BY apellido, nombre"),
            relacion("idAlojamiento", "Alojamiento", "SELECT idAlojamiento, CONCAT(numero, ' - ', tipo) FROM `alojamientos` ORDER BY numero"),
            fecha("fechaIngreso", "Ingreso (AAAA-MM-DD)"), fecha("fechaEgreso", "Egreso (AAAA-MM-DD)"),
            entero("cantidadPersonas", "Cantidad de personas"), texto("temporada", "Temporada", false),
            opcion("estadoReserva", "Estado", List.of("PENDIENTE_SENA", "CONFIRMADA", "CANCELADA", "FINALIZADA")),
            opcion("estadoLlegada", "Situación de llegada", List.of("PENDIENTE_LLEGADA", "EN_ESTADIA", "NO_SE_PRESENTO", "FINALIZADA")),
            decimal("precioTotal", "Precio total (0 calcula base)")));

    public static final Modulo ACTIVIDAD_INSCRIPTOS = new Modulo("Inscripciones", "inscripcionActividad", "idInscripcion", List.of(
            relacion("idCliente", "Cliente", "SELECT idCliente, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `cliente` ORDER BY apellido, nombre"),
            relacion("idActividad", "Actividad", "SELECT idActividad, nombre FROM `Actividad` ORDER BY nombre"),
            fecha("fechaActividad", "Día elegido (AAAA-MM-DD)"),
            fecha("fechaInscripcion", "Fecha de inscripción (AAAA-MM-DD)"),
            booleano("asistencia", "Asistió"),
            opcion("estado", "Estado", List.of("ACTIVA", "CANCELADA"))));

    public static final Modulo PAGOS = new Modulo("Pagos", "pago", "idPago", List.of(
            relacion("idReserva", "Reserva", "SELECT r.idReserva, CONCAT('Reserva ', r.idReserva, ' - ', c.apellido, ', ', c.nombre) "
                    + "FROM reservas r JOIN cliente c ON c.idCliente=r.idCliente ORDER BY r.idReserva DESC"),
            decimal("montoTotal", "Monto"), opcion("metodoPago", "Medio de pago", List.of("Efectivo", "Transferencia", "Tarjeta", "Otro")),
            opcion("tipoPago", "Concepto", List.of("SENA", "SALDO", "ACTIVIDAD", "REEMBOLSO")),
            opcion("estado", "Estado", List.of("PENDIENTE", "CONFIRMADO", "ANULADO")),
            fecha("fechaPago", "Fecha (AAAA-MM-DD)"), texto("comprobante", "Número de operación", false),
            texto("archivoComprobante", "Archivo del comprobante", false), texto("observacion", "Observación para el cliente", false)));

    public static final Modulo REEMBOLSOS = new Modulo("Reintegros", "reembolso", "idReembolso", List.of(
            relacion("idPedido", "Pedido", "SELECT idPedido, CONCAT('Pedido ',idPedido,' - cliente ',idCliente) FROM pedido_alojamiento ORDER BY idPedido DESC"),
            decimal("monto", "Importe a devolver"), opcion("estado", "Estado", List.of("PENDIENTE", "CONFIRMADO", "RECHAZADO")),
            texto("motivo", "Motivo", true), texto("metodo", "Medio de devolución", false),
            texto("comprobante", "Comprobante", false)));

    public static final Modulo ACOMPANANTES = new Modulo("Acompañantes", "acompañante", "idAcompañante", List.of(
            relacion("idCliente", "Cliente", "SELECT idCliente, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `cliente` ORDER BY apellido, nombre"),
            entero("dni", "DNI"), texto("nombre", "Nombre", true), texto("apellido", "Apellido", true),
            entero("edad", "Edad"), texto("parentesco", "Parentesco", true),
            booleano("activo", "Visible en la cuenta")));

    public static final Modulo USUARIOS = new Modulo("Usuarios", "usuarios", "idUsuario", List.of(
            texto("nombre", "Nombre", true), texto("apellido", "Apellido", true),
            new Campo("email", "Email", Tipo.TEXTO, true),
            opcion("rol", "Rol", List.of("SUPERADMIN", "ADMINISTRADOR", "RECEPCION", "ADMINISTRACION", "COORDINADOR", "GERENCIA", "LIMPIEZA")),
            new Campo("password_hash", "Contraseña (dejar vacía para conservarla)", Tipo.CLAVE, false, false, List.of(), null)));

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    private GestionDAO() { }

    // Valida el permiso antes de consultar o cambiar cualquier modulo.
    /**
     * Comprueba acceso y evita continuar cuando hay información inválida.
     */
    public static void validarAcceso(Modulo modulo) throws SQLException {
        if (SesionActual.get() == null || !tieneAccesoTabla(modulo.tabla(), SesionActual.get().getRol())) {
            throw new SQLException("Tu rol no tiene acceso a la sección " + modulo.titulo() + ".");
        }
    }

    /** Valida las escrituras en la capa de datos, aunque se invoque fuera de la interfaz. */
    public static void validarPermisoEscritura(String tabla, String seccion, Operacion operacion) throws SQLException {
        String rol = SesionActual.get() == null ? "" : SesionActual.get().getRol();
        boolean permitido = switch (operacion) {
            case CREAR -> switch (rol) {
                case "SUPERADMIN" -> tieneAccesoTabla(tabla, rol) && !"cliente".equals(tabla);
                case "ADMINISTRADOR" -> "usuarios".equals(tabla) || "pago".equals(tabla);
                // Coordinación crea y organiza las actividades, además de
                // poder cargar inscripciones manuales cuando corresponde.
                case "COORDINADOR" -> List.of("Actividad", "inscripcionActividad").contains(tabla);
                case "ADMINISTRACION" -> "reservaAcompañante".equals(tabla) || "pago".equals(tabla);
                case "RECEPCION", "GERENCIA" -> "pago".equals(tabla);
                default -> false;
            };
            case EDITAR -> tieneAccesoTabla(tabla, rol)
                    // Recepción únicamente puede resolver una seña pendiente: la validación
                    // posterior impide cambiar importe, reserva o comprobante.
                    && (!"pago".equals(tabla) || List.of("SUPERADMIN", "ADMINISTRADOR", "ADMINISTRACION", "RECEPCION", "GERENCIA").contains(rol))
                    && (!"usuarios".equals(tabla) || List.of("ADMINISTRADOR", "SUPERADMIN").contains(rol))
                    && (!"inscripcionActividad".equals(tabla) || List.of("SUPERADMIN", "COORDINADOR").contains(rol));
            case ELIMINAR -> tieneAccesoTabla(tabla, rol)
                    && (!List.of("pago", "inscripcionActividad").contains(tabla) || "SUPERADMIN".equals(rol))
                    && (!"usuarios".equals(tabla) || List.of("ADMINISTRADOR", "SUPERADMIN").contains(rol))
                    && (!"alojamientos".equals(tabla) || !List.of("RECEPCION", "LIMPIEZA").contains(rol));
        };
        if (!permitido) throw new SQLException("Tu rol no puede " + operacion.name().toLowerCase(Locale.ROOT)
                + " registros de " + seccion + ".");
    }

    /**
     * Indica si la sesión actual puede mostrar una acción de alta, edición o baja.
     * La ventana usa esta misma regla que protege el DAO, para no ofrecer botones
     * que de todas maneras serían rechazados al intentar usarlos.
     */
    public static boolean puedeEscribir(Modulo modulo, Operacion operacion) {
        try {
            validarAcceso(modulo);
            validarPermisoEscritura(modulo.tabla(), modulo.titulo(), operacion);
            return true;
        } catch (SQLException error) {
            return false;
        }
    }

    /**
     * Cambia el estado operativo de una unidad desde Recepción o Limpieza.
     * El motivo no altera la tabla de alojamientos: se guarda junto al cambio
     * en la auditoría para saber quién lo realizó, cuándo y por qué.
     */
    public static void cambiarEstadoAlojamiento(int idAlojamiento, String nuevoEstado, String motivo) throws SQLException {
        validarAcceso(ALOJAMIENTOS);
        validarPermisoEscritura("alojamientos", ALOJAMIENTOS.titulo(), Operacion.EDITAR);
        boolean limpieza = esLimpieza();
        boolean recepcion = esRecepcion();
        if (!limpieza && !recepcion && !esSuperAdmin()) {
            throw new SQLException("Solo Recepción, Limpieza o SuperAdmin pueden actualizar el estado operativo.");
        }
        motivo = motivo == null ? "" : motivo.trim();
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                String numero;
                String estadoAnterior;
                try (PreparedStatement consulta = conexion.prepareStatement(
                        "SELECT numero,estado FROM alojamientos WHERE idAlojamiento=? FOR UPDATE")) {
                    consulta.setInt(1, idAlojamiento);
                    try (ResultSet resultados = consulta.executeQuery()) {
                        if (!resultados.next()) throw new IllegalArgumentException("El alojamiento seleccionado no existe.");
                        numero = resultados.getString("numero");
                        estadoAnterior = resultados.getString("estado");
                    }
                }
                validarEstadoOperativo(nuevoEstado, limpieza, recepcion);
                if (recepcion && List.of("En mantenimiento", "Fuera de servicio").contains(nuevoEstado) && motivo.isBlank()) {
                    throw new IllegalArgumentException("Indicá el motivo antes de dejar un alojamiento fuera de servicio.");
                }
                if (!Objects.equals(estadoAnterior, nuevoEstado)) {
                    try (PreparedStatement actualizar = conexion.prepareStatement(
                            "UPDATE alojamientos SET estado=? WHERE idAlojamiento=?")) {
                        actualizar.setString(1, nuevoEstado);
                        actualizar.setInt(2, idAlojamiento);
                        actualizar.executeUpdate();
                    }
                    Map<String, Object> anterior = new LinkedHashMap<>();
                    anterior.put("numero", numero);
                    anterior.put("estado", estadoAnterior);
                    Map<String, Object> nuevo = new LinkedHashMap<>();
                    nuevo.put("numero", numero);
                    nuevo.put("estado", nuevoEstado);
                    if (!motivo.isBlank()) nuevo.put("motivo", motivo);
                    AuditoriaDAO.registrar(conexion, "CAMBIAR_ESTADO", "Alojamientos",
                            Integer.toString(idAlojamiento), anterior, nuevo);
                }
                conexion.commit();
            } catch (Exception error) {
                conexion.rollback();
                if (error instanceof SQLException sql) throw sql;
                if (error instanceof IllegalArgumentException argumento) throw argumento;
                throw new SQLException("No se pudo actualizar el estado del alojamiento", error);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /** Cuenta las transferencias informadas que todavía requieren revisión humana. */
    public static int contarSeniasPorConfirmar() throws SQLException {
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COUNT(DISTINCT d.idPedido) FROM pago p JOIN detalle_pedido_alojamiento d ON d.idReserva=p.idReserva "
                        + "WHERE UPPER(p.tipoPago)='SENA' AND UPPER(p.estado)='PENDIENTE'" );
             ResultSet resultados = sentencia.executeQuery()) {
            return resultados.next() ? resultados.getInt(1) : 0;
        }
    }

    /**
     * Comprueba acceso clientes y evita continuar cuando hay información inválida.
     */
    public static void validarAccesoClientes() throws SQLException {
        validarAccesoTabla("cliente", "Clientes");
    }

    /**
     * Comprueba acceso acompanantes reserva y evita continuar cuando hay información inválida.
     */
    public static void validarAccesoAcompanantesReserva() throws SQLException {
        validarAccesoTabla("reservaAcompañante", "Acompañantes por reserva");
    }

    /**
     * Comprueba acceso tabla y evita continuar cuando hay información inválida.
     */
    private static void validarAccesoTabla(String tabla, String seccion) throws SQLException {
        if (SesionActual.get() == null || !tieneAccesoTabla(tabla, SesionActual.get().getRol())) {
            throw new SQLException("Tu rol no tiene acceso a la sección " + seccion + ".");
        }
    }

    /**
     * Comprueba tiene acceso tabla y devuelve verdadero o falso según se cumpla la condición.
     */
    private static boolean tieneAccesoTabla(String tabla, String rol) {
        // Esta lista tambien protege las operaciones que llegan directo al DAO.
        return switch (rol) {
            case "SUPERADMIN" -> List.of("cliente", "usuarios", "reservas", "pago", "reembolso", "alojamientos",
                    "acompañante", "reservaAcompañante", "Actividad", "inscripcionActividad").contains(tabla);
            case "ADMINISTRADOR" -> List.of("cliente", "usuarios", "reservas", "pago", "reembolso").contains(tabla);
            case "GERENCIA" -> List.of("cliente", "reservas", "pago", "reembolso", "alojamientos").contains(tabla);
            case "RECEPCION" -> List.of("cliente", "alojamientos", "reservas", "pago", "reembolso").contains(tabla);
            case "ADMINISTRACION" -> List.of("cliente", "acompañante", "alojamientos", "reservas", "pago", "reembolso", "reservaAcompañante").contains(tabla);
            case "COORDINADOR" -> List.of("Actividad", "inscripcionActividad").contains(tabla);
            case "LIMPIEZA" -> "alojamientos".equals(tabla);
            default -> false;
        };
    }
    /**
     * Ejecuta la lógica de “campos visibles” en GestionDAO.java.
     */
    public static List<Campo> camposVisibles(Modulo modulo) {
        return modulo.campos().stream().filter(Campo::visible).toList();
    }

    /**
     * Ejecuta la lógica de “opciones” en GestionDAO.java.
     */
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

    /** Lista pedidos cuyas reservas activas ya llegaron a su fecha de egreso.
     * Un pedido con varias reservas se cobra como conjunto: nunca se liquida una
     * parte y se deja otra con el dinero mezclado. */
    public static List<Opcion> reservasParaCobroFinal() throws SQLException {
        validarAcceso(PAGOS);
        String consultaSql = "SELECT MIN(r.idReserva), CONCAT('Pedido ',pa.idPedido,' · ',c.apellido,', ',c.nombre) "
                + "FROM reservas r JOIN cliente c ON c.idCliente=r.idCliente "
                + "JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                + "JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido "
                + "WHERE (r.fechaEgreso<? OR (r.fechaEgreso=? AND ?>='10:00:00')) AND UPPER(r.estadoReserva)='CONFIRMADA' "
                + "AND UPPER(pa.estadoPago) IN ('SENA_PAGADA','PAGADO') "
                + "AND NOT EXISTS (SELECT 1 FROM reservas rx JOIN detalle_pedido_alojamiento dx ON dx.idReserva=rx.idReserva "
                + "WHERE dx.idPedido=pa.idPedido AND UPPER(rx.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA') "
                + "AND (rx.fechaEgreso>? OR (rx.fechaEgreso=? AND ?<'10:00:00'))) "
                + "GROUP BY pa.idPedido,c.apellido,c.nombre ORDER BY MAX(r.fechaEgreso) DESC, pa.idPedido DESC";
        List<Opcion> resultado = new ArrayList<>();
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            Date fecha = Date.valueOf(fechaOperativa());
            sentencia.setDate(1, fecha);
            sentencia.setDate(2, fecha);
            sentencia.setTime(3, Time.valueOf(horaOperativa()));
            sentencia.setDate(4, fecha);
            sentencia.setDate(5, fecha);
            sentencia.setTime(6, Time.valueOf(horaOperativa()));
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) resultado.add(new Opcion(resultados.getInt(1), resultados.getString(2)));
            }
        }
        return resultado;
    }

    /** Calcula en forma segura el alojamiento y las actividades que quedan pendientes. */
    public static ResumenCobroFinal resumenCobroFinal(int idReserva) throws SQLException {
        validarAcceso(PAGOS);
        try (Connection conexion = Conexion.obtener()) {
            return leerResumenCobroFinal(conexion, idReserva, false);
        }
    }

    /** Registra el cierre: un movimiento de saldo y, si corresponde, otro de actividades. */
    public static void registrarCobroFinal(int idReserva, String metodoPago, String comprobante) throws SQLException {
        validarAcceso(PAGOS);
        validarPermisoEscritura("pago", "Pagos", Operacion.CREAR);
        if (!List.of("Efectivo", "Transferencia", "Tarjeta", "Otro").contains(metodoPago)) {
            throw new IllegalArgumentException("Elegí un medio de pago válido.");
        }
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                ResumenCobroFinal resumen = leerResumenCobroFinal(conexion, idReserva, true);
                int idPedido = pedidoDeReserva(conexion, idReserva);
                List<Map<String, Object>> estadosAntesDeLimpieza = estadosAlojamientoPedido(conexion, idPedido);
                try (PreparedStatement insertar = conexion.prepareStatement(
                        "INSERT INTO pago(idReserva,montoTotal,metodoPago,tipoPago,estado,fechaPago,comprobante) VALUES (?,?,? ,?, 'CONFIRMADO',?,?)")) {
                    if (resumen.saldoAlojamiento().signum() > 0) {
                        insertar.setInt(1, idReserva); insertar.setBigDecimal(2, resumen.saldoAlojamiento());
                        insertar.setString(3, metodoPago); insertar.setString(4, "SALDO");
                        insertar.setDate(5, Date.valueOf(fechaOperativa())); insertar.setString(6, comprobante);
                        insertar.executeUpdate();
                    }
                    if (resumen.actividadesPendientes().signum() > 0) {
                        insertar.setInt(1, idReserva); insertar.setBigDecimal(2, resumen.actividadesPendientes());
                        insertar.setString(3, metodoPago); insertar.setString(4, "ACTIVIDAD");
                        insertar.setDate(5, Date.valueOf(fechaOperativa())); insertar.setString(6, comprobante);
                        insertar.executeUpdate();
                    }
                }
                try (PreparedStatement cargos = conexion.prepareStatement(
                        "UPDATE cargo_actividad SET estado='PAGADO', actualizadoEn=NOW() WHERE idPedido=? AND fechaActividad<=? AND UPPER(estado)='PENDIENTE'")) {
                    cargos.setInt(1, idPedido);
                    cargos.setDate(2, Date.valueOf(fechaOperativa()));
                    cargos.executeUpdate();
                }
                try (PreparedStatement finalizar = conexion.prepareStatement(
                        "UPDATE reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                        + "SET r.estadoReserva='FINALIZADA', r.estadoLlegada='FINALIZADA', "
                        + "r.checkOutReal=COALESCE(r.checkOutReal,?) "
                        + "WHERE d.idPedido=? AND UPPER(r.estadoReserva)='CONFIRMADA'")) {
                    finalizar.setTimestamp(1, momentoOperativo());
                    finalizar.setInt(2, idPedido);
                    finalizar.executeUpdate();
                }
                // Al cerrar la estadía, el alojamiento deja de estar ocupado y pasa a limpieza.
                // No se lo libera automáticamente: el personal de limpieza debe marcarlo como Disponible.
                try (PreparedStatement limpieza = conexion.prepareStatement(
                        "UPDATE alojamientos a JOIN reservas r ON r.idAlojamiento=a.idAlojamiento "
                        + "JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                        + "SET a.estado='En limpieza' WHERE d.idPedido=? "
                        + "AND a.estado NOT IN ('En mantenimiento','Fuera de servicio')")) {
                    limpieza.setInt(1, idPedido); limpieza.executeUpdate();
                }
                for (Map<String, Object> anterior : estadosAntesDeLimpieza) {
                    String estadoAnterior = String.valueOf(anterior.get("estado"));
                    if (List.of("En limpieza", "En mantenimiento", "Fuera de servicio").contains(estadoAnterior)) continue;
                    Map<String, Object> nuevo = new LinkedHashMap<>(anterior);
                    nuevo.put("estado", "En limpieza");
                    nuevo.put("motivo", "Check-out registrado");
                    AuditoriaDAO.registrar(conexion, "CAMBIAR_ESTADO", "Alojamientos",
                            String.valueOf(anterior.get("idAlojamiento")), anterior, nuevo);
                }
                sincronizarPedidoPorId(conexion, idPedido);
                AuditoriaDAO.registrar(conexion, "COBRAR_SALDO", "Pagos", Integer.toString(idReserva), Map.of(),
                        Map.of("cliente", resumen.cliente(), "total", resumen.total(), "medio", metodoPago));
                conexion.commit();
            } catch (Exception error) {
                conexion.rollback();
                if (error instanceof SQLException sql) throw sql;
                if (error instanceof IllegalArgumentException argumento) throw argumento;
                throw new SQLException("No se pudo registrar el cobro final", error);
            } finally { conexion.setAutoCommit(true); }
        }
    }

    /**
     * Carga los datos que necesita esta parte del sistema.
     */
    public static List<Object[]> listar(Modulo modulo) throws SQLException {
        // Arma la consulta de lista segun la definicion del modulo.
        validarAcceso(modulo);
        if ("Actividad".equals(modulo.tabla())) return listarActividades();
        List<Campo> visibles = camposVisibles(modulo);
        String columnas = visibles.stream().map(campo -> expresionVisible(modulo, campo))
                .reduce((acumulado, siguiente) -> acumulado + ", " + siguiente).orElse("");
        String consultaSql = "SELECT " + q(modulo.id()) + (columnas.isEmpty() ? "" : ", " + columnas)
                + " FROM " + q(modulo.tabla());
        if ("usuarios".equals(modulo.tabla())) {
            consultaSql += " WHERE UPPER(rol) IN (" + String.join(",", java.util.Collections.nCopies(UsuarioDAO.ROLES_EMPLEADO.size(), "?")) + ")";
        } else if ("alojamientos".equals(modulo.tabla()) && esLimpieza()) {
            // Limpieza solo ve las unidades pendientes después de un check-out.
            consultaSql += " WHERE estado='En limpieza'";
        }
        consultaSql += " ORDER BY " + q(modulo.id()) + " DESC";
        List<Object[]> filas = new ArrayList<>();
        try (Connection conexion = Conexion.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
            if ("usuarios".equals(modulo.tabla())) {
                int parametro = 1;
                for (String rol : UsuarioDAO.ROLES_EMPLEADO) sentencia.setString(parametro++, rol);
            }
            try (ResultSet resultados = sentencia.executeQuery()) {
            while (resultados.next()) {
                Object[] fila = new Object[visibles.size() + 1];
                fila[0] = resultados.getInt(1);
                for (int indice = 0; indice < visibles.size(); indice++) {
                    Campo campo = visibles.get(indice);
                    Object valor = campo.tipo() == Tipo.BOOLEANO
                            ? resultados.getBoolean(indice + 2) : resultados.getObject(indice + 2);
                    fila[indice + 1] = formatearValor(campo, valor);
                }
                filas.add(fila);
            }
            }
        }
        return filas;
    }

    /** Muestra nombres legibles en lugar de claves foráneas en los listados de gestión. */
    private static String expresionVisible(Modulo modulo, Campo campo) {
        if ("idCliente".equals(campo.columna())) {
            return "(SELECT CONCAT(c.apellido, ', ', c.nombre) FROM cliente c WHERE c.idCliente = "
                    + q(modulo.tabla()) + ".idCliente) AS " + q(campo.columna());
        }
        if ("inscripcionActividad".equals(modulo.tabla()) && "idActividad".equals(campo.columna())) {
            return "(SELECT a.nombre FROM Actividad a WHERE a.idActividad = inscripcionActividad.idActividad) AS idActividad";
        }
        if ("reservas".equals(modulo.tabla()) && "idAlojamiento".equals(campo.columna())) {
            return "(SELECT CONCAT(a.numero, ' - ', a.tipo) FROM alojamientos a WHERE a.idAlojamiento=reservas.idAlojamiento) AS idAlojamiento";
        }
        if ("pago".equals(modulo.tabla()) && "idReserva".equals(campo.columna())) {
            return "(SELECT CONCAT('Reserva ', r.idReserva, ' - ', c.apellido, ', ', c.nombre) "
                    + "FROM reservas r JOIN cliente c ON c.idCliente=r.idCliente WHERE r.idReserva=pago.idReserva) AS idReserva";
        }
        return q(campo.columna());
    }

    /** Da formato de lectura a fechas, booleanos y valores monetarios sin alterar el dato persistido. */
    private static Object formatearValor(Campo campo, Object valor) {
        if (valor == null) return "";
        if (campo.tipo() == Tipo.FECHA && valor instanceof Date fecha) {
            return fecha.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-AR")));
        }
        if (campo.tipo() == Tipo.BOOLEANO) return Boolean.TRUE.equals(valor) ? "Sí" : "No";
        if (campo.tipo() == Tipo.DECIMAL && valor instanceof Number numero) {
            return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(numero);
        }
        return valor;
    }

    /**
     * Carga actividades que necesita esta parte del sistema.
     */
    private static List<Object[]> listarActividades() throws SQLException {
        // El cupo se calcula por fecha e incluye al titular y a sus acompañantes.
        // De todas las fechas se muestra la más ocupada, no una suma histórica.
        String consultaSql = "SELECT a.idActividad, a.nombre, a.descripcionCorta, a.horario, a.dias, a.momentoDia, "
                + "a.Duracion, a.sector, a.responsable, a.edadRecomendada, a.modalidad, a.activa, e.nombre, a.cupoMax, "
                + "COALESCE(o.maximoFecha, 0), GREATEST(a.cupoMax - COALESCE(o.maximoFecha, 0), 0), "
                + "CASE WHEN COALESCE(o.maximoFecha, 0) >= a.cupoMax THEN 'Alguna fecha llena' "
                + "WHEN a.activa = 0 THEN 'Inactiva' "
                + "WHEN a.cupoMax - COALESCE(o.maximoFecha, 0) <= 2 THEN 'Últimos lugares en una fecha' "
                + "ELSE 'Disponible' END, a.precio, a.pago "
                + "FROM `Actividad` a "
                + "LEFT JOIN `EspacioRecreativo` e ON e.idEspacio = a.idEspacio "
                + "LEFT JOIN ("
                + "  SELECT ocupacion.idActividad, MAX(ocupacion.totalFecha) AS maximoFecha "
                + "  FROM ("
                + "    SELECT ia.idActividad, ia.fechaActividad, "
                + "           COUNT(DISTINCT CASE WHEN ia.participaTitular = 1 THEN ia.idInscripcion END) "
                + "           + COUNT(iaa.idInscripcionAcompanante) AS totalFecha "
                + "    FROM `inscripcionActividad` ia "
                + "    LEFT JOIN `inscripcionActividadAcompanante` iaa "
                + "      ON iaa.idClienteTitular = ia.idCliente AND iaa.idActividad = ia.idActividad AND iaa.fechaActividad = ia.fechaActividad "
                + "     AND iaa.activo = 1 "
                + "    WHERE UPPER(ia.estado) = 'ACTIVA' "
                + "    GROUP BY ia.idActividad, ia.fechaActividad"
                + "  ) ocupacion GROUP BY ocupacion.idActividad"
                + ") o ON o.idActividad = a.idActividad "
                + "GROUP BY a.idActividad, a.nombre, a.descripcionCorta, a.horario, a.dias, a.momentoDia, "
                + "a.Duracion, a.sector, a.responsable, a.edadRecomendada, a.modalidad, a.activa, "
                + "e.nombre, a.cupoMax, o.maximoFecha, a.precio, a.pago "
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
                        inscriptos + " / " + cupoMax, resultados.getInt(16), resultados.getString(17),
                        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(resultados.getBigDecimal(18)),
                        resultados.getString(19)});
            }
        }
        return filas;
    }

    /**
     * Ejecuta la lógica de “etiqueta momento dia” en GestionDAO.java.
     */
    private static String etiquetaMomentoDia(String valor) {
        if (valor == null) return "";
        return switch (valor) {
            case "manana" -> "Mañana";
            case "tarde" -> "Tarde";
            case "noche" -> "Noche";
            default -> valor;
        };
    }

    /**
     * Ejecuta la lógica de “etiqueta edad” en GestionDAO.java.
     */
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

    /**
     * Ejecuta la lógica de “etiqueta modalidad” en GestionDAO.java.
     */
    private static String etiquetaModalidad(String valor) {
        if (valor == null) return "";
        return switch (valor) {
            case "incluida" -> "Incluida";
            case "inscripcion" -> "Con inscripción";
            default -> valor;
        };
    }

    /**
     * Busca por id y devuelve el resultado encontrado.
     */
    public static Map<String, Object> buscarPorId(Modulo modulo, int id) throws SQLException {
        // Carga todos los valores de una fila para abrir el formulario de edicion.
        validarAcceso(modulo);
        String columnas = modulo.campos().stream().map(campo -> q(campo.columna()))
                .reduce((acumulado, siguiente) -> acumulado + ", " + siguiente).orElse("");
        String consultaSql = "SELECT " + columnas + " FROM " + q(modulo.tabla())
                + " WHERE " + q(modulo.id()) + " = ?";
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

    /**
     * Valida y guarda la información en el destino correspondiente.
     */
    public static void guardar(Modulo modulo, Integer id, Map<String, String> valores) throws SQLException {
        // Revisa permisos, convierte datos y guarda una fila nueva o sus cambios.
        validarAcceso(modulo);
        validarPermisoEscritura(modulo.tabla(), modulo.titulo(), id == null ? Operacion.CREAR : Operacion.EDITAR);
        boolean rolLimpieza = esLimpieza();
        boolean rolRecepcion = esRecepcion();
        boolean rolCoordinador = esCoordinador();
        boolean gestionAlojamientos = "alojamientos".equals(modulo.tabla());
        if (rolLimpieza && !gestionAlojamientos) {
            throw new SQLException("El rol Limpieza solo puede actualizar el estado de limpieza de alojamientos.");
        }
        if (gestionAlojamientos && id == null && !esSuperAdmin()) {
            throw new SQLException("Los alojamientos se agregan desde la página web.");
        }
        // Una transaccion permite deshacer el guardado si falla una validacion o consulta.
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                if ("usuarios".equals(modulo.tabla()) && id != null) validarCuentaEmpleado(conexion, id);
                Map<String, Object> datos = new LinkedHashMap<>();
                for (Campo campo : modulo.campos()) {
                    // El coordinador registra si asistió; no puede alterar al cliente,
                    // la actividad elegida, la fecha ni el estado de su inscripción.
                    if (rolCoordinador && "inscripcionActividad".equals(modulo.tabla())
                            && id != null && !"asistencia".equals(campo.columna())) continue;
                    if (rolLimpieza && !"estado".equals(campo.columna())) continue;
                    if (rolRecepcion && gestionAlojamientos && !"estado".equals(campo.columna())) continue;
                    // En Llegadas y estadías recepción no edita una reserva completa:
                    // únicamente informa la llegada. No se deben exigir Cliente,
                    // Alojamiento, fechas ni importes que ni siquiera se muestran.
                    if (rolRecepcion && "reservas".equals(modulo.tabla())
                            && !"estadoLlegada".equals(campo.columna())) continue;
                    if (gestionAlojamientos && !esSuperAdmin() && !rolLimpieza && !rolRecepcion
                            && "estado".equals(campo.columna())) continue;
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
                if (gestionAlojamientos && (rolRecepcion || rolLimpieza)) {
                    validarCambioEstadoAlojamiento(id, (String) datos.get("estado"), rolLimpieza);
                }
                if ("usuarios".equals(modulo.tabla())) validarRolUsuario(datos, id);
                if ("reservas".equals(modulo.tabla())) {
                    if (esRecepcion()) validarEstadoLlegada(conexion, id, String.valueOf(datos.get("estadoLlegada")));
                    else validarReserva(conexion, datos, id);
                }
                if ("Actividad".equals(modulo.tabla())) validarActividad(conexion, datos, id);
                if ("inscripcionActividad".equals(modulo.tabla()) && id == null) validarCupo(conexion, datos);
                if (rolCoordinador && "inscripcionActividad".equals(modulo.tabla()) && id != null) {
                    validarRegistroAsistencia(conexion, id);
                }
                if ("usuarios".equals(modulo.tabla())) validarEmail(valores.get("email"));
                if (("pago".equals(modulo.tabla()) || "reembolso".equals(modulo.tabla()))
                        && ((BigDecimal) datos.get("pago".equals(modulo.tabla()) ? "montoTotal" : "monto")).signum() <= 0) {
                    throw new IllegalArgumentException("El importe debe ser mayor que cero.");
                }
                if ("reembolso".equals(modulo.tabla()) && "CONFIRMADO".equals(datos.get("estado"))
                        && (datos.get("metodo") == null || datos.get("comprobante") == null)) {
                    throw new IllegalArgumentException("Para confirmar un reintegro indicá el medio y el comprobante.");
                }

                boolean crear = id == null;
                String asignaciones = datos.keySet().stream().map(k -> q(k) + " = ?")
                        .reduce((acumulado, siguiente) -> acumulado + ", " + siguiente).orElseThrow();
                String consultaSql;
                if (crear) {
                    consultaSql = "INSERT INTO " + q(modulo.tabla()) + " SET " + asignaciones;
                } else {
                    consultaSql = "UPDATE " + q(modulo.tabla()) + " SET " + asignaciones + " WHERE " + q(modulo.id()) + " = ?";
                }
                Map<String, Object> datosAnteriores = crear ? Map.of()
                        : AuditoriaDAO.capturar(conexion, modulo.tabla(), modulo.id(), id);
                if ("pago".equals(modulo.tabla())) {
                    if (crear) throw new IllegalArgumentException("Los pagos nuevos se registran desde «Cobrar estadía final».");
                    validarRevisionDeSena(datosAnteriores, datos);
                }
                int idGuardado = id == null ? -1 : id;
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        consultaSql, crear ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
                    int indice = 1;
                    for (Object valor : datos.values()) sentencia.setObject(indice++, valor);
                    if (!crear) sentencia.setInt(indice, id);
                    sentencia.executeUpdate();
                    if (crear) {
                        try (ResultSet claves = sentencia.getGeneratedKeys()) {
                            if (claves.next()) idGuardado = claves.getInt(1);
                        }
                    }
                }
                Map<String, Object> datosNuevos = idGuardado < 0 ? datos
                        : AuditoriaDAO.capturar(conexion, modulo.tabla(), modulo.id(), idGuardado);
                if ("reembolso".equals(modulo.tabla()) && "CONFIRMADO".equals(datos.get("estado")) && idGuardado > 0) {
                    try (PreparedStatement resolucion = conexion.prepareStatement(
                            "UPDATE reembolso SET fechaResolucion=COALESCE(fechaResolucion,NOW()) WHERE idReembolso=?")) {
                        resolucion.setInt(1, idGuardado);
                        resolucion.executeUpdate();
                    }
                }
                if ("pago".equals(modulo.tabla())) sincronizarPagoPedido(conexion, datos);
                if ("pago".equals(modulo.tabla()) && datosAnteriores.get("idReserva") instanceof Number reservaAnterior
                        && !reservaAnterior.equals(datos.get("idReserva"))) {
                    sincronizarPagoPedido(conexion, Map.of("idReserva", reservaAnterior));
                }
                if ("reembolso".equals(modulo.tabla())) sincronizarPedidoPorId(conexion, ((Number) datos.get("idPedido")).intValue());
                if ("inscripcionActividad".equals(modulo.tabla()) && idGuardado > 0) {
                    sincronizarCargoActividad(conexion, idGuardado);
                }
                if ("reservas".equals(modulo.tabla()) && idGuardado > 0) {
                    sincronizarActividadesDeReserva(conexion, idGuardado);
                }
                if ("acompañante".equals(modulo.tabla()) && idGuardado > 0) {
                    sincronizarActividadesDeAcompanante(conexion, idGuardado);
                }
                AuditoriaDAO.registrar(conexion, crear ? "CREAR" : "EDITAR", modulo.titulo(),
                        idGuardado < 0 ? null : Integer.toString(idGuardado), datosAnteriores, datosNuevos);
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

    /**
     * Elimina el elemento indicado del sistema.
     */
    public static void eliminar(Modulo modulo, int id) throws SQLException {
        // Verifica permisos y relaciones antes de borrar el registro.
        validarAcceso(modulo);
        validarPermisoEscritura(modulo.tabla(), modulo.titulo(), Operacion.ELIMINAR);
        if ("usuarios".equals(modulo.tabla())) {
            try (Connection conexion = Conexion.obtener()) { validarCuentaEmpleado(conexion, id); }
        }
        if ("inscripcionActividad".equals(modulo.tabla())) {
            cancelarInscripcionActividad(id);
            return;
        }
        if ("Actividad".equals(modulo.tabla())) {
            darDeBajaActividad(id);
            return;
        }
        if (esRecepcion() && "alojamientos".equals(modulo.tabla())) {
            throw new SQLException("Recepción no puede eliminar alojamientos.");
        }
        if ("usuarios".equals(modulo.tabla()) && !esSuperAdmin()) {
            try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT rol FROM usuarios WHERE idUsuario = ?")) {
                sentencia.setInt(1, id);
                try (ResultSet resultados = sentencia.executeQuery()) {
                    if (resultados.next() && "SUPERADMIN".equals(resultados.getString(1))) {
                        throw new SQLException("Solo un SuperAdmin puede eliminar una cuenta SuperAdmin.");
                    }
                }
            }
        }
        if (esLimpieza() && "alojamientos".equals(modulo.tabla())) {
            throw new SQLException("El rol Limpieza no puede eliminar habitaciones.");
        }
        String consultaSql = "DELETE FROM " + q(modulo.tabla()) + " WHERE " + q(modulo.id()) + " = ?";
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                Map<String, Object> datosAnteriores = AuditoriaDAO.capturar(
                        conexion, modulo.tabla(), modulo.id(), id);
                try (PreparedStatement sentencia = conexion.prepareStatement(consultaSql)) {
                    sentencia.setInt(1, id);
                    sentencia.executeUpdate();
                }
                if ("pago".equals(modulo.tabla()) && datosAnteriores.get("idReserva") instanceof Number reservaAnterior) {
                    sincronizarPagoPedido(conexion, Map.of("idReserva", reservaAnterior));
                }
                if ("reembolso".equals(modulo.tabla()) && datosAnteriores.get("idPedido") instanceof Number pedidoAnterior) {
                    sincronizarPedidoPorId(conexion, pedidoAnterior.intValue());
                }
                AuditoriaDAO.registrar(conexion, "ELIMINAR", modulo.titulo(), Integer.toString(id),
                        datosAnteriores, Map.of());
                conexion.commit();
            } catch (Exception excepcion) {
                conexion.rollback();
                if (excepcion instanceof SQLException excepcionSql) throw excepcionSql;
                throw new SQLException("No se pudo eliminar el registro", excepcion);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /** Impide editar o eliminar cuentas web ajenas al conjunto de roles del personal. */
    private static void validarCuentaEmpleado(Connection conexion, int idUsuario) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement("SELECT rol FROM usuarios WHERE idUsuario=?")) {
            sentencia.setInt(1, idUsuario);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next() || resultados.getString(1) == null
                        || !UsuarioDAO.ROLES_EMPLEADO.contains(resultados.getString(1).toUpperCase(Locale.ROOT))) {
                    throw new SQLException("Solo se pueden editar o eliminar cuentas del personal de la aplicación.");
                }
            }
        }
    }

    /**
     * Da de baja una actividad sin borrar su historial. Las inscripciones de
     * fechas futuras, sus acompañantes y sus cargos pendientes se anulan en la
     * misma transacción para que un cupo no quede bloqueado por error.
     */
    private static void darDeBajaActividad(int idActividad) throws SQLException {
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                Map<String, Object> anterior = AuditoriaDAO.capturar(
                        conexion, "Actividad", "idActividad", idActividad);
                try (PreparedStatement actividad = conexion.prepareStatement(
                        "SELECT idActividad FROM Actividad WHERE idActividad=? AND activa=1 FOR UPDATE")) {
                    actividad.setInt(1, idActividad);
                    try (ResultSet resultado = actividad.executeQuery()) {
                        if (!resultado.next()) {
                            throw new SQLException("La actividad no existe o ya fue dada de baja.");
                        }
                    }
                }

                Date hoy = Date.valueOf(fechaOperativa());
                try (PreparedStatement cargos = conexion.prepareStatement(
                        "UPDATE cargo_actividad ca JOIN inscripcionActividad ia ON ia.idInscripcion=ca.idInscripcion "
                                + "SET ca.estado='ANULADO',ca.anuladoEn=NOW(),ca.actualizadoEn=NOW() "
                                + "WHERE ia.idActividad=? AND ia.fechaActividad>=? "
                                + "AND UPPER(ia.estado)='ACTIVA' AND UPPER(ca.estado)='PENDIENTE'")) {
                    cargos.setInt(1, idActividad);
                    cargos.setDate(2, hoy);
                    cargos.executeUpdate();
                }
                try (PreparedStatement acompanantes = conexion.prepareStatement(
                        "UPDATE inscripcionActividadAcompanante iaa JOIN inscripcionActividad ia "
                                + "ON ia.idCliente=iaa.idClienteTitular AND ia.idActividad=iaa.idActividad "
                                + "AND ia.fechaActividad=iaa.fechaActividad "
                                + "SET iaa.activo=0,iaa.canceladaEn=NOW() WHERE ia.idActividad=? "
                                + "AND ia.fechaActividad>=? AND UPPER(ia.estado)='ACTIVA' AND iaa.activo=1")) {
                    acompanantes.setInt(1, idActividad);
                    acompanantes.setDate(2, hoy);
                    acompanantes.executeUpdate();
                }
                try (PreparedStatement inscripciones = conexion.prepareStatement(
                        "UPDATE inscripcionActividad SET estado='CANCELADA',canceladaEn=NOW(),"
                                + "motivoCancelacion='ACTIVIDAD_CANCELADA_POR_COORDINACION' "
                                + "WHERE idActividad=? AND fechaActividad>=? AND UPPER(estado)='ACTIVA'")) {
                    inscripciones.setInt(1, idActividad);
                    inscripciones.setDate(2, hoy);
                    inscripciones.executeUpdate();
                }

                // La tabla de espera pertenece a una actualización posterior;
                // una instalación antigua no debe impedir la baja de la actividad.
                try (Statement tablas = conexion.createStatement();
                     ResultSet existe = tablas.executeQuery("SHOW TABLES LIKE 'listaEsperaActividad'")) {
                    if (existe.next()) {
                        try (PreparedStatement espera = conexion.prepareStatement(
                                "UPDATE listaEsperaActividad SET estado='CANCELADA',canceladaEn=NOW() "
                                        + "WHERE idActividad=? AND estado='PENDIENTE'")) {
                            espera.setInt(1, idActividad);
                            espera.executeUpdate();
                        }
                    }
                }
                try (PreparedStatement baja = conexion.prepareStatement(
                        "UPDATE Actividad SET activa=0 WHERE idActividad=?")) {
                    baja.setInt(1, idActividad);
                    baja.executeUpdate();
                }
                AuditoriaDAO.registrar(conexion, "BAJA", "Actividades", Integer.toString(idActividad),
                        anterior, AuditoriaDAO.capturar(conexion, "Actividad", "idActividad", idActividad));
                conexion.commit();
            } catch (Exception error) {
                conexion.rollback();
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No se pudo dar de baja la actividad", error);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /**
     * Cancela reserva de manera controlada.
     */
    public static void cancelarReserva(int idReserva) throws SQLException {
        // Conserva la reserva y cambia su estado para mantener el historial.
        validarAcceso(RESERVAS);
        validarPermisoEscritura("reservas", RESERVAS.titulo(), Operacion.ELIMINAR);
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                Map<String, Object> anterior = AuditoriaDAO.capturar(conexion, "reservas", "idReserva", idReserva);
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "UPDATE reservas SET estadoReserva = 'CANCELADA' "
                                + "WHERE idReserva = ? AND fechaIngreso > CURRENT_DATE "
                                + "AND UPPER(estadoReserva) NOT IN ('CANCELADA', 'CANCELADO')")) {
                    sentencia.setInt(1, idReserva);
                    if (sentencia.executeUpdate() == 0) {
                        throw new SQLException("La reserva no existe, ya fue cancelada o la estadía ya comenzó.");
                    }
                }
                sincronizarActividadesDeReserva(conexion, idReserva);
                solicitarReembolsoCancelacion(conexion, idReserva);
                Map<String, Object> nuevo = AuditoriaDAO.capturar(conexion, "reservas", "idReserva", idReserva);
                AuditoriaDAO.registrar(conexion, "CANCELAR", "Reservas", Integer.toString(idReserva), anterior, nuevo);
                conexion.commit();
            } catch (Exception excepcion) {
                conexion.rollback();
                if (excepcion instanceof SQLException excepcionSql) throw excepcionSql;
                throw new SQLException("No se pudo cancelar la reserva", excepcion);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /** Recalcula el pedido y genera el reintegro que corresponda al cancelar. */
    private static void solicitarReembolsoCancelacion(Connection conexion, int idReserva) throws SQLException {
        int idPedido;
        try (PreparedStatement s=conexion.prepareStatement("SELECT idPedido FROM detalle_pedido_alojamiento WHERE idReserva=?")) {
            s.setInt(1,idReserva); try(ResultSet r=s.executeQuery()){ if(!r.next()) return; idPedido=r.getInt(1); }
        }
        BigDecimal totalAnterior;
        try(PreparedStatement s=conexion.prepareStatement("SELECT total FROM pedido_alojamiento WHERE idPedido=? FOR UPDATE")){
            s.setInt(1,idPedido);try(ResultSet r=s.executeQuery()){r.next();totalAnterior=r.getBigDecimal(1);}
        }
        try(PreparedStatement s=conexion.prepareStatement(
                "UPDATE pago p JOIN detalle_pedido_alojamiento d ON d.idReserva=p.idReserva SET p.estado='ANULADO',"
                        + "p.observacion='La pre-reserva fue cancelada antes de confirmar el pago.' WHERE d.idPedido=? AND p.estado='PENDIENTE'")) {
            s.setInt(1,idPedido); s.executeUpdate();
        }
        sincronizarPedidoPorId(conexion,idPedido);
        BigDecimal pagado=BigDecimal.ZERO, totalNuevo=BigDecimal.ZERO, senaNueva=BigDecimal.ZERO;
        int activas=0;
        try (PreparedStatement s=conexion.prepareStatement(
                "SELECT GREATEST(0,COALESCE(SUM(CASE WHEN p.estado='CONFIRMADO' AND p.tipoPago IN ('SENA','SALDO') THEN p.montoTotal ELSE 0 END),0)-COALESCE((SELECT SUM(monto) FROM reembolso WHERE idPedido=? AND estado='CONFIRMADO'),0)), "
                        + "pa.total,pa.montoSena,(SELECT COUNT(*) FROM detalle_pedido_alojamiento da JOIN reservas ra ON ra.idReserva=da.idReserva WHERE da.idPedido=? AND UPPER(ra.estadoReserva) NOT IN ('CANCELADA','CANCELADO')) "
                        + "FROM pedido_alojamiento pa JOIN detalle_pedido_alojamiento d ON d.idPedido=pa.idPedido LEFT JOIN pago p ON p.idReserva=d.idReserva WHERE pa.idPedido=? GROUP BY pa.total,pa.montoSena")) {
            s.setInt(1,idPedido);s.setInt(2,idPedido);s.setInt(3,idPedido);
            try(ResultSet r=s.executeQuery()){if(r.next()){pagado=r.getBigDecimal(1);totalNuevo=r.getBigDecimal(2);senaNueva=r.getBigDecimal(3);activas=r.getInt(4);}}
        }
        BigDecimal aRetener=activas==0?BigDecimal.ZERO:(pagado.compareTo(totalAnterior)>=0?totalNuevo:senaNueva);
        BigDecimal importe=pagado.subtract(aRetener).max(BigDecimal.ZERO);
        try(PreparedStatement s=conexion.prepareStatement("DELETE FROM reembolso WHERE idPedido=? AND estado='PENDIENTE'")){s.setInt(1,idPedido);s.executeUpdate();}
        if(importe.signum()>0){
            try(PreparedStatement s=conexion.prepareStatement("INSERT INTO reembolso(idPedido,monto,estado,motivo,fechaSolicitud) VALUES (?,?,'PENDIENTE','Cancelación de alojamiento',NOW())")){
                s.setInt(1,idPedido);s.setBigDecimal(2,importe);s.executeUpdate();
            }
        }
        sincronizarPedidoPorId(conexion,idPedido);
    }

    /** Libera alojamientos cuya seña venció sin acreditarse. */
    public static void vencerPedidosPendientes() throws SQLException {
        try(Connection conexion=Conexion.obtener()){
            conexion.setAutoCommit(false);
            try {
                Set<Integer> clientesAfectados = new LinkedHashSet<>();
                Timestamp momento = momentoOperativo();
                try (PreparedStatement s = conexion.prepareStatement(
                        "SELECT pa.idCliente FROM pedido_alojamiento pa WHERE "
                                + "((pa.fechaLimiteSena<? AND pa.estadoPedido IN ('ESPERANDO_SENA','PENDIENTE') "
                                + "AND pa.estadoPago='SENA_PENDIENTE' AND (SELECT COALESCE(SUM(p.montoTotal),0) "
                                + "FROM detalle_pedido_alojamiento d JOIN pago p ON p.idReserva=d.idReserva "
                                + "WHERE d.idPedido=pa.idPedido AND p.estado='CONFIRMADO' AND p.tipoPago IN ('SENA','SALDO'))<pa.montoSena) "
                                + "OR (pa.estadoPago='SENA_INFORMADA' AND pa.fechaLimiteRevisionSena IS NOT NULL "
                                + "AND pa.fechaLimiteRevisionSena<?)) FOR UPDATE")) {
                    s.setTimestamp(1, momento);
                    s.setTimestamp(2, momento);
                    try (ResultSet r = s.executeQuery()) {
                        while (r.next()) clientesAfectados.add(r.getInt(1));
                    }
                }
                try(PreparedStatement s=conexion.prepareStatement(
                        "UPDATE pedido_alojamiento pa LEFT JOIN (SELECT d.idPedido,SUM(CASE WHEN p.estado='CONFIRMADO' AND p.tipoPago IN ('SENA','SALDO') THEN p.montoTotal ELSE 0 END) pagado FROM detalle_pedido_alojamiento d LEFT JOIN pago p ON p.idReserva=d.idReserva GROUP BY d.idPedido) x ON x.idPedido=pa.idPedido "
                                + "SET pa.estadoPedido='VENCIDO',pa.estadoPago='VENCIDO',pa.canceladoEn=? WHERE pa.fechaLimiteSena<? AND pa.estadoPedido IN ('ESPERANDO_SENA','PENDIENTE') AND pa.estadoPago='SENA_PENDIENTE' AND COALESCE(x.pagado,0)<pa.montoSena")){
                    s.setTimestamp(1, momento);
                    s.setTimestamp(2, momento);
                    s.executeUpdate();
                }
                try(PreparedStatement s=conexion.prepareStatement("UPDATE reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido SET r.estadoReserva='CANCELADA' WHERE pa.estadoPedido='VENCIDO' AND UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA')")){s.executeUpdate();}
                try(PreparedStatement s=conexion.prepareStatement("UPDATE pedido_alojamiento SET estadoPedido='VENCIDO',estadoPago='PAGO_RECHAZADO',fechaLimiteRevisionSena=NULL,canceladoEn=? WHERE estadoPago='SENA_INFORMADA' AND fechaLimiteRevisionSena<?")){
                    s.setTimestamp(1, momento);
                    s.setTimestamp(2, momento);
                    s.executeUpdate();
                }
                try(PreparedStatement s=conexion.prepareStatement("UPDATE pago p JOIN detalle_pedido_alojamiento d ON d.idReserva=p.idReserva JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido SET p.estado='ANULADO',p.observacion='La recepción no pudo confirmar la transferencia dentro de las 24 horas.' WHERE pa.estadoPago='PAGO_RECHAZADO' AND p.estado='PENDIENTE'")){s.executeUpdate();}
                try(PreparedStatement s=conexion.prepareStatement("UPDATE reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido SET r.estadoReserva='CANCELADA' WHERE pa.estadoPago='PAGO_RECHAZADO' AND UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA')")){s.executeUpdate();}

                // Si venció la estadía que habilitaba actividades, también se
                // liberan sus cupos y se anulan los cargos aún pendientes.
                for (int idCliente : clientesAfectados) {
                    sincronizarActividadesCliente(conexion, idCliente);
                }
                repararAlojamientosPendientesDeLimpieza(conexion, momento);
                conexion.commit();
            } catch (Exception error) {
                conexion.rollback();
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException("No se pudieron vencer los pedidos pendientes", error);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /**
     * Respaldo para reservas que ya fueron cerradas: si la aplicación se abrió
     * luego del check-out, deja el alojamiento en limpieza sin liberarlo por
     * accidente. Limpieza conserva la tarea de cambiarlo a Disponible.
     */
    private static void repararAlojamientosPendientesDeLimpieza(Connection conexion, Timestamp momento) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "UPDATE alojamientos a JOIN reservas r ON r.idAlojamiento=a.idAlojamiento "
                        + "SET a.estado='En limpieza' WHERE a.estado='Ocupado' "
                        + "AND UPPER(r.estadoReserva)='FINALIZADA' "
                        + "AND (r.checkOutReal IS NOT NULL OR r.fechaEgreso<=?)")) {
            sentencia.setDate(1, Date.valueOf(momento.toLocalDateTime().toLocalDate()));
            sentencia.executeUpdate();
        }
    }

    /** Revisa las actividades del titular después de cambiar una reserva. */
    public static void sincronizarActividadesDeReserva(Connection conexion, int idReserva) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT idCliente FROM reservas WHERE idReserva=?")) {
            sentencia.setInt(1, idReserva);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next()) sincronizarActividadesCliente(conexion, resultados.getInt(1));
            }
        }
    }

    /** Revisa las actividades después de activar o desactivar un acompañante. */
    static void sincronizarActividadesDeAcompanante(Connection conexion, int idAcompanante) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT idCliente FROM `acompañante` WHERE idAcompañante=?")) {
            sentencia.setInt(1, idAcompanante);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next()) sincronizarActividadesCliente(conexion, resultados.getInt(1));
            }
        }
    }

    /** Aplica en Java la misma regla de coherencia usada por la página. */
    private static void sincronizarActividadesCliente(Connection conexion, int idCliente) throws SQLException {
        List<Integer> inscripciones = new ArrayList<>();
        Map<Integer, Date> fechas = new LinkedHashMap<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT idInscripcion, fechaActividad FROM inscripcionActividad "
                        + "WHERE idCliente=? AND UPPER(estado)='ACTIVA' FOR UPDATE")) {
            sentencia.setInt(1, idCliente);
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    int idInscripcion = resultados.getInt(1);
                    inscripciones.add(idInscripcion);
                    fechas.put(idInscripcion, resultados.getDate(2));
                }
            }
        }

        for (int idInscripcion : inscripciones) {
            Date fecha = fechas.get(idInscripcion);
            boolean titularValido;
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT 1 FROM reservas r "
                            + "JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                            + "JOIN pedido_alojamiento p ON p.idPedido=d.idPedido "
                            + "WHERE r.idCliente=? AND UPPER(r.estadoReserva)='CONFIRMADA' "
                            + "AND UPPER(p.estadoPago) IN ('SENA_PAGADA','PAGADO') "
                            + "AND ? >= r.fechaIngreso AND ? < r.fechaEgreso LIMIT 1")) {
                sentencia.setInt(1, idCliente);
                sentencia.setDate(2, fecha);
                sentencia.setDate(3, fecha);
                try (ResultSet resultados = sentencia.executeQuery()) {
                    titularValido = resultados.next();
                }
            }
            if (!titularValido) {
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "UPDATE inscripcionActividad SET participaTitular=0 WHERE idInscripcion=?")) {
                    sentencia.setInt(1, idInscripcion);
                    sentencia.executeUpdate();
                }
            }

            List<Integer> filasAcompanantes = new ArrayList<>();
            List<Integer> idsAcompanantes = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT iaa.idInscripcionAcompanante, iaa.idAcompañante "
                            + "FROM inscripcionActividadAcompanante iaa "
                            + "JOIN inscripcionActividad ia ON ia.idCliente=iaa.idClienteTitular "
                            + "AND ia.idActividad=iaa.idActividad "
                            + "WHERE ia.idInscripcion=? AND iaa.activo=1 FOR UPDATE")) {
                sentencia.setInt(1, idInscripcion);
                try (ResultSet resultados = sentencia.executeQuery()) {
                    while (resultados.next()) {
                        filasAcompanantes.add(resultados.getInt(1));
                        idsAcompanantes.add(resultados.getInt(2));
                    }
                }
            }
            for (int indice = 0; indice < filasAcompanantes.size(); indice++) {
                boolean acompananteValido;
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "SELECT 1 FROM `reservaAcompañante` ra "
                                + "JOIN `acompañante` a ON a.idAcompañante=ra.idAcompañante "
                                + "JOIN reservas r ON r.idReserva=ra.idReserva "
                                + "JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                                + "JOIN pedido_alojamiento p ON p.idPedido=d.idPedido "
                                + "WHERE ra.idAcompañante=? AND a.activo=1 "
                                + "AND UPPER(r.estadoReserva)='CONFIRMADA' "
                                + "AND UPPER(p.estadoPago) IN ('SENA_PAGADA','PAGADO') "
                                + "AND ? >= r.fechaIngreso AND ? < r.fechaEgreso LIMIT 1")) {
                    sentencia.setInt(1, idsAcompanantes.get(indice));
                    sentencia.setDate(2, fecha);
                    sentencia.setDate(3, fecha);
                    try (ResultSet resultados = sentencia.executeQuery()) {
                        acompananteValido = resultados.next();
                    }
                }
                if (!acompananteValido) {
                    try (PreparedStatement sentencia = conexion.prepareStatement(
                            "UPDATE inscripcionActividadAcompanante SET activo=0, canceladaEn=NOW() "
                                    + "WHERE idInscripcionAcompanante=?")) {
                        sentencia.setInt(1, filasAcompanantes.get(indice));
                        sentencia.executeUpdate();
                    }
                }
            }

            int participantes;
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT (CASE WHEN participaTitular=1 THEN 1 ELSE 0 END) + "
                            + "(SELECT COUNT(*) FROM inscripcionActividadAcompanante iaa "
                            + "WHERE iaa.idClienteTitular=ia.idCliente AND iaa.idActividad=ia.idActividad AND iaa.fechaActividad=ia.fechaActividad AND iaa.activo=1) "
                            + "FROM inscripcionActividad ia WHERE idInscripcion=?")) {
                sentencia.setInt(1, idInscripcion);
                try (ResultSet resultados = sentencia.executeQuery()) {
                    resultados.next();
                    participantes = resultados.getInt(1);
                }
            }
            if (participantes == 0) {
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "UPDATE inscripcionActividad SET estado='CANCELADA', canceladaEn=NOW(), "
                                + "motivoCancelacion='ESTADIA_O_ACOMPAÑANTE_MODIFICADO' WHERE idInscripcion=?")) {
                    sentencia.setInt(1, idInscripcion);
                    sentencia.executeUpdate();
                }
            }
            sincronizarCargoActividad(conexion, idInscripcion);
        }
    }

    /** Cancela una inscripción conservando el registro y anulando su cargo pendiente. */
    private static void cancelarInscripcionActividad(int idInscripcion) throws SQLException {
        validarPermisoEscritura("inscripcionActividad", ACTIVIDAD_INSCRIPTOS.titulo(), Operacion.ELIMINAR);
        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                Map<String, Object> anterior = AuditoriaDAO.capturar(
                        conexion, "inscripcionActividad", "idInscripcion", idInscripcion);
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "UPDATE inscripcionActividad SET estado='CANCELADA', canceladaEn=NOW(), "
                                + "motivoCancelacion='CANCELADA_DESDE_JAVA' "
                                + "WHERE idInscripcion=? AND UPPER(estado)='ACTIVA'")) {
                    sentencia.setInt(1, idInscripcion);
                    if (sentencia.executeUpdate() == 0) {
                        throw new SQLException("La inscripción no existe o ya estaba cancelada.");
                    }
                }
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "UPDATE inscripcionActividadAcompanante iaa "
                                + "JOIN inscripcionActividad ia ON ia.idCliente=iaa.idClienteTitular "
                                + "AND ia.idActividad=iaa.idActividad "
                                + "SET iaa.activo=0, iaa.canceladaEn=NOW() WHERE ia.idInscripcion=?")) {
                    sentencia.setInt(1, idInscripcion);
                    sentencia.executeUpdate();
                }
                sincronizarCargoActividad(conexion, idInscripcion);
                Map<String, Object> nuevo = AuditoriaDAO.capturar(
                        conexion, "inscripcionActividad", "idInscripcion", idInscripcion);
                AuditoriaDAO.registrar(conexion, "CANCELAR", "Inscripciones",
                        Integer.toString(idInscripcion), anterior, nuevo);
                conexion.commit();
            } catch (Exception excepcion) {
                conexion.rollback();
                if (excepcion instanceof SQLException excepcionSql) throw excepcionSql;
                throw new SQLException("No se pudo cancelar la inscripción", excepcion);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /** Guarda el valor histórico que se cobrará por una actividad paga. */
    static void sincronizarCargoActividad(Connection conexion, int idInscripcion) throws SQLException {
        int idCliente;
        int idActividad;
        Integer idPedido;
        Date fechaActividad;
        String estadoInscripcion;
        String modalidad;
        BigDecimal precioActual;
        int participantes;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT ia.idCliente, ia.idActividad, ia.fechaActividad, ia.estado, a.modalidad, a.precio, "
                        + "(CASE WHEN ia.participaTitular=1 AND UPPER(ia.estado)='ACTIVA' THEN 1 ELSE 0 END) "
                        + "+ (SELECT COUNT(*) FROM inscripcionActividadAcompanante iaa "
                        + "WHERE iaa.idClienteTitular=ia.idCliente AND iaa.idActividad=ia.idActividad AND iaa.fechaActividad=ia.fechaActividad AND iaa.activo=1) "
                        + ", (SELECT p.idPedido FROM reservas r "
                        + "JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                        + "JOIN pedido_alojamiento p ON p.idPedido=d.idPedido "
                        + "WHERE r.idCliente=ia.idCliente AND UPPER(r.estadoReserva)='CONFIRMADA' "
                        + "AND UPPER(p.estadoPago) IN ('SENA_PAGADA','PAGADO') "
                        + "AND ia.fechaActividad BETWEEN r.fechaIngreso AND r.fechaEgreso "
                        + "ORDER BY r.fechaIngreso DESC, p.idPedido DESC LIMIT 1) "
                        + "FROM inscripcionActividad ia JOIN Actividad a ON a.idActividad=ia.idActividad "
                        + "WHERE ia.idInscripcion=? FOR UPDATE")) {
            sentencia.setInt(1, idInscripcion);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) return;
                idCliente = resultados.getInt(1);
                idActividad = resultados.getInt(2);
                fechaActividad = resultados.getDate(3);
                estadoInscripcion = resultados.getString(4);
                modalidad = resultados.getString(5);
                precioActual = resultados.getBigDecimal(6);
                participantes = resultados.getInt(7);
                Object pedidoValor = resultados.getObject(8);
                idPedido = pedidoValor == null ? null : ((Number) pedidoValor).intValue();
            }
        }

        Integer idCargo = null;
        BigDecimal precioAceptado = null;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT idCargoActividad, precioUnitario FROM cargo_actividad "
                        + "WHERE idInscripcion=? AND UPPER(estado)='PENDIENTE' "
                        + "ORDER BY idCargoActividad DESC LIMIT 1 FOR UPDATE")) {
            sentencia.setInt(1, idInscripcion);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next()) {
                    idCargo = resultados.getInt(1);
                    precioAceptado = resultados.getBigDecimal(2);
                }
            }
        }

        boolean debeCobrar = "ACTIVA".equalsIgnoreCase(estadoInscripcion)
                && "inscripcion".equalsIgnoreCase(modalidad)
                && precioActual != null && precioActual.signum() > 0 && participantes > 0;
        if (!debeCobrar) {
            if (idCargo != null) {
                try (PreparedStatement sentencia = conexion.prepareStatement(
                        "UPDATE cargo_actividad SET estado='ANULADO', anuladoEn=NOW(), actualizadoEn=NOW() "
                                + "WHERE idCargoActividad=?")) {
                    sentencia.setInt(1, idCargo);
                    sentencia.executeUpdate();
                }
            }
            return;
        }

        if (idCargo != null) {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE cargo_actividad SET idPedido=?, fechaActividad=?, cantidadParticipantes=?, montoTotal=?, actualizadoEn=NOW() "
                            + "WHERE idCargoActividad=?")) {
                sentencia.setObject(1, idPedido);
                sentencia.setDate(2, fechaActividad);
                sentencia.setInt(3, participantes);
                sentencia.setBigDecimal(4, precioAceptado.multiply(BigDecimal.valueOf(participantes)));
                sentencia.setInt(5, idCargo);
                sentencia.executeUpdate();
            }
        } else {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO cargo_actividad (idInscripcion,idPedido,idCliente,idActividad,fechaActividad," 
                            + "cantidadParticipantes,precioUnitario,montoTotal,estado) "
                            + "VALUES (?,?,?,?,?,?,?,?,'PENDIENTE')")) {
                sentencia.setInt(1, idInscripcion);
                sentencia.setObject(2, idPedido);
                sentencia.setInt(3, idCliente);
                sentencia.setInt(4, idActividad);
                sentencia.setDate(5, fechaActividad);
                sentencia.setInt(6, participantes);
                sentencia.setBigDecimal(7, precioActual);
                sentencia.setBigDecimal(8, precioActual.multiply(BigDecimal.valueOf(participantes)));
                sentencia.executeUpdate();
            }
        }
    }

    /**
     * Comprueba es limpieza y devuelve verdadero o falso según se cumpla la condición.
     */
    private static boolean esLimpieza() {
        return SesionActual.get() != null && "LIMPIEZA".equals(SesionActual.get().getRol());
    }

    /**
     * Comprueba es recepcion y devuelve verdadero o falso según se cumpla la condición.
     */
    private static boolean esRecepcion() {
        return SesionActual.get() != null && "RECEPCION".equals(SesionActual.get().getRol());
    }

    /** Indica si la sesión pertenece a quien puede registrar asistencia de actividades. */
    private static boolean esCoordinador() {
        return SesionActual.get() != null && "COORDINADOR".equals(SesionActual.get().getRol());
    }

    /**
     * Comprueba es super admin y devuelve verdadero o falso según se cumpla la condición.
     */
    private static boolean esSuperAdmin() {
        return SesionActual.get() != null && "SUPERADMIN".equals(SesionActual.get().getRol());
    }

    /**
     * Devuelve la fecha real, salvo que el archivo local de configuración fije una
     * fecha de demostración. Nunca modifica el reloj de la computadora ni la base.
     */
    private static LocalDate fechaOperativa() {
        // El reloj definido por Superadmin en la web tiene prioridad porque es
        // compartido por los dos sistemas durante una exposición.
        var demostracion = RelojDemostracion.obtenerActivo();
        if (demostracion.isPresent()) return demostracion.get().fecha();
        String configurada = Config.getOrDefault("app.fecha_prueba", "");
        if (configurada.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(configurada);
        } catch (java.time.format.DateTimeParseException error) {
            throw new IllegalStateException("app.fecha_prueba debe tener el formato AAAA-MM-DD.", error);
        }
    }

    /** Devuelve la hora real o la hora de demostración indicada en la configuración local. */
    private static LocalTime horaOperativa() {
        var demostracion = RelojDemostracion.obtenerActivo();
        if (demostracion.isPresent()) return demostracion.get().hora();
        String configurada = Config.getOrDefault("app.hora_prueba", "");
        if (configurada.isBlank()) return LocalTime.now().withSecond(0).withNano(0);
        try {
            return LocalTime.parse(configurada);
        } catch (java.time.format.DateTimeParseException error) {
            throw new IllegalStateException("app.hora_prueba debe tener el formato HH:MM.", error);
        }
    }

    /** Forma una marca de tiempo coherente con el reloj de demostración activo. */
    private static Timestamp momentoOperativo() {
        return Timestamp.valueOf(LocalDateTime.of(fechaOperativa(), horaOperativa()));
    }

    /**
     * Comprueba cambio estado alojamiento y evita continuar cuando hay información inválida.
     */
    private static void validarCambioEstadoAlojamiento(Integer id, String nuevoEstado,
                                                        boolean rolLimpieza) throws SQLException {
        if (id == null) throw new SQLException("No se encontró el alojamiento seleccionado.");
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT estado FROM alojamientos WHERE idAlojamiento=?")) {
            sentencia.setInt(1, id);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next() && nuevoEstado.equals(resultados.getString(1))) return;
            }
        }
        validarEstadoOperativo(nuevoEstado, rolLimpieza, !rolLimpieza);
    }

    /** Centraliza los estados que cada equipo puede establecer manualmente. */
    private static void validarEstadoOperativo(String nuevoEstado, boolean rolLimpieza, boolean rolRecepcion) {
        if (rolLimpieza && !"Disponible".equals(nuevoEstado)) {
            throw new IllegalArgumentException("Limpieza solo puede finalizar una unidad y marcarla como Disponible.");
        }
        if (rolRecepcion && !List.of("Disponible", "En mantenimiento", "Fuera de servicio").contains(nuevoEstado)) {
            throw new IllegalArgumentException("Recepción solo puede habilitar una unidad o ponerla en mantenimiento/fuera de servicio.");
        }
    }

    /**
     * Registra el resultado de una fecha concreta de una actividad recurrente.
     * No desactiva la actividad completa: por ejemplo, una clase de los lunes
     * puede suspenderse hoy y continuar disponible el lunes siguiente.
     *
     * Al suspender, las inscripciones quedan conservadas como historial, se
     * liberan los acompañantes y se anulan solamente los cargos pendientes.
     */
    public static void registrarCierreActividad(
            int idActividad, String fechaTexto, String estadoCierre, String motivo
    ) throws SQLException {
        if (!esCoordinador() && !esSuperAdmin()) {
            throw new SQLException("Solo Coordinación o SuperAdmin pueden cerrar una actividad.");
        }
        final LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaTexto == null ? "" : fechaTexto.trim());
        } catch (java.time.format.DateTimeParseException error) {
            throw new IllegalArgumentException("Ingresá una fecha válida con el formato AAAA-MM-DD.", error);
        }
        String cierre = estadoCierre == null ? "" : estadoCierre.trim().toUpperCase(Locale.ROOT);
        if (!List.of("REALIZADA", "SUSPENDIDA").contains(cierre)) {
            throw new IllegalArgumentException("Elegí si la actividad fue realizada o suspendida.");
        }
        if ("REALIZADA".equals(cierre) && fecha.isAfter(fechaOperativa())) {
            throw new IllegalArgumentException("Una actividad solo puede marcarse como realizada el día en que ocurre o después.");
        }
        motivo = motivo == null ? "" : motivo.trim();
        if ("SUSPENDIDA".equals(cierre) && motivo.isBlank()) {
            throw new IllegalArgumentException("Indicá el motivo de la suspensión.");
        }
        if (motivo.length() > 500) {
            throw new IllegalArgumentException("El motivo no puede superar los 500 caracteres.");
        }

        try (Connection conexion = Conexion.obtener()) {
            conexion.setAutoCommit(false);
            try {
                String nombre;
                try (PreparedStatement actividad = conexion.prepareStatement(
                        "SELECT nombre,activa FROM Actividad WHERE idActividad=? FOR UPDATE")) {
                    actividad.setInt(1, idActividad);
                    try (ResultSet resultado = actividad.executeQuery()) {
                        if (!resultado.next() || !resultado.getBoolean("activa")) {
                            throw new IllegalArgumentException("La actividad seleccionada no existe o fue dada de baja.");
                        }
                        nombre = resultado.getString("nombre");
                    }
                }
                try (PreparedStatement existente = conexion.prepareStatement(
                        "SELECT estado FROM cierreActividad WHERE idActividad=? AND fechaActividad=? FOR UPDATE")) {
                    existente.setInt(1, idActividad);
                    existente.setDate(2, Date.valueOf(fecha));
                    try (ResultSet resultado = existente.executeQuery()) {
                        if (resultado.next()) {
                            throw new IllegalArgumentException("Ya se registró esa jornada como "
                                    + resultado.getString("estado").toLowerCase(Locale.ROOT) + ".");
                        }
                    }
                }

                if ("SUSPENDIDA".equals(cierre)) {
                    try (PreparedStatement cargos = conexion.prepareStatement(
                            "UPDATE cargo_actividad ca JOIN inscripcionActividad ia ON ia.idInscripcion=ca.idInscripcion "
                                    + "SET ca.estado='ANULADO',ca.anuladoEn=?,ca.actualizadoEn=? "
                                    + "WHERE ia.idActividad=? AND ia.fechaActividad=? "
                                    + "AND UPPER(ia.estado)='ACTIVA' AND UPPER(ca.estado)='PENDIENTE'")) {
                        Timestamp momento = momentoOperativo();
                        cargos.setTimestamp(1, momento);
                        cargos.setTimestamp(2, momento);
                        cargos.setInt(3, idActividad);
                        cargos.setDate(4, Date.valueOf(fecha));
                        cargos.executeUpdate();
                    }
                    try (PreparedStatement acompanantes = conexion.prepareStatement(
                            "UPDATE inscripcionActividadAcompanante iaa JOIN inscripcionActividad ia "
                                    + "ON ia.idCliente=iaa.idClienteTitular AND ia.idActividad=iaa.idActividad "
                                    + "AND ia.fechaActividad=iaa.fechaActividad "
                                    + "SET iaa.activo=0,iaa.canceladaEn=? WHERE ia.idActividad=? "
                                    + "AND ia.fechaActividad=? AND UPPER(ia.estado)='ACTIVA' AND iaa.activo=1")) {
                        acompanantes.setTimestamp(1, momentoOperativo());
                        acompanantes.setInt(2, idActividad);
                        acompanantes.setDate(3, Date.valueOf(fecha));
                        acompanantes.executeUpdate();
                    }
                    try (PreparedStatement inscripciones = conexion.prepareStatement(
                            "UPDATE inscripcionActividad SET estado='CANCELADA',canceladaEn=?,"
                                    + "motivoCancelacion='ACTIVIDAD_SUSPENDIDA_POR_COORDINACION' "
                                    + "WHERE idActividad=? AND fechaActividad=? AND UPPER(estado)='ACTIVA'")) {
                        inscripciones.setTimestamp(1, momentoOperativo());
                        inscripciones.setInt(2, idActividad);
                        inscripciones.setDate(3, Date.valueOf(fecha));
                        inscripciones.executeUpdate();
                    }
                    try (Statement tablas = conexion.createStatement();
                         ResultSet existe = tablas.executeQuery("SHOW TABLES LIKE 'listaEsperaActividad'")) {
                        if (existe.next()) {
                            try (PreparedStatement espera = conexion.prepareStatement(
                                    "UPDATE listaEsperaActividad SET estado='CANCELADA',canceladaEn=? "
                                            + "WHERE idActividad=? AND fechaActividad=? AND estado='PENDIENTE'")) {
                                espera.setTimestamp(1, momentoOperativo());
                                espera.setInt(2, idActividad);
                                espera.setDate(3, Date.valueOf(fecha));
                                espera.executeUpdate();
                            }
                        }
                    }
                }

                try (PreparedStatement cierreNuevo = conexion.prepareStatement(
                        "INSERT INTO cierreActividad (idActividad,fechaActividad,estado,motivo,cerradoEn,cerradoPor) "
                                + "VALUES (?,?,?,?,?,?)")) {
                    cierreNuevo.setInt(1, idActividad);
                    cierreNuevo.setDate(2, Date.valueOf(fecha));
                    cierreNuevo.setString(3, cierre);
                    cierreNuevo.setString(4, motivo.isBlank() ? null : motivo);
                    cierreNuevo.setTimestamp(5, momentoOperativo());
                    cierreNuevo.setInt(6, SesionActual.get().getIdUsuario());
                    cierreNuevo.executeUpdate();
                }
                AuditoriaDAO.registrar(conexion, "CERRAR_JORNADA", "Actividades",
                        idActividad + ":" + fecha, Map.of(), Map.of(
                                "actividad", nombre, "fecha", fecha.toString(), "estado", cierre,
                                "motivo", motivo
                        ));
                conexion.commit();
            } catch (Exception error) {
                conexion.rollback();
                if (error instanceof SQLException sql) throw sql;
                if (error instanceof IllegalArgumentException argumento) throw argumento;
                throw new SQLException("No se pudo registrar el cierre de la actividad", error);
            } finally {
                conexion.setAutoCommit(true);
            }
        }
    }

    /** Evita marcar asistencia de una actividad que aún no ocurrió o fue cancelada. */
    private static void validarRegistroAsistencia(Connection conexion, int idInscripcion) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT ia.idActividad,ia.fechaActividad,ia.estado,a.activa FROM inscripcionActividad ia "
                        + "JOIN Actividad a ON a.idActividad=ia.idActividad WHERE ia.idInscripcion=? FOR UPDATE")) {
            sentencia.setInt(1, idInscripcion);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("La inscripción seleccionada no existe.");
                int idActividad = resultados.getInt("idActividad");
                Date fecha = resultados.getDate("fechaActividad");
                if (!"ACTIVA".equalsIgnoreCase(resultados.getString("estado")) || !resultados.getBoolean("activa")) {
                    throw new IllegalArgumentException("No se puede registrar asistencia de una actividad cancelada.");
                }
                if (fecha != null && fecha.toLocalDate().isAfter(fechaOperativa())) {
                    throw new IllegalArgumentException("La asistencia se registra el día de la actividad o después.");
                }
                if (fecha != null && jornadaYaCerrada(conexion, idActividad, fecha)) {
                    throw new IllegalArgumentException("La jornada ya fue cerrada. Registrá la asistencia antes de marcarla realizada.");
                }
            }
        }
    }

    /** Comprueba si Coordinación ya registró el resultado de esa fecha. */
    private static boolean jornadaYaCerrada(Connection conexion, int idActividad, Date fecha) throws SQLException {
        try (Statement tablas = conexion.createStatement();
             ResultSet existe = tablas.executeQuery("SHOW TABLES LIKE 'cierreActividad'")) {
            if (!existe.next()) return false;
        }
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT 1 FROM cierreActividad WHERE idActividad=? AND fechaActividad=? LIMIT 1")) {
            consulta.setInt(1, idActividad);
            consulta.setDate(2, fecha);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }

    /**
     * Sincroniza la seña registrada desde la app con el pedido y la reserva web.
     * La confirmación solo ocurre cuando la suma de pagos de seña confirmados
     * alcanza el importe solicitado para el pedido.
     */
    private static void sincronizarPagoPedido(Connection conexion, Map<String, Object> datos) throws SQLException {
        Object reservaValor = datos.get("idReserva");
        if (!(reservaValor instanceof Number numeroReserva)) return;
        int idReserva = numeroReserva.intValue();
        int idPedido;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT pa.idPedido "
                        + "FROM detalle_pedido_alojamiento origen "
                        + "JOIN pedido_alojamiento pa ON pa.idPedido=origen.idPedido "
                        + "WHERE origen.idReserva=? LIMIT 1 FOR UPDATE")) {
            sentencia.setInt(1, idReserva);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) return; // El pago puede pertenecer a una reserva antigua sin pedido web.
                idPedido = resultados.getInt(1);
            }
        }
        sincronizarPedidoPorId(conexion, idPedido);
    }

    /** Solo permite que recepción confirme o anule una seña que informó el cliente. */
    private static void validarRevisionDeSena(Map<String, Object> anterior, Map<String, Object> nuevo) {
        String tipo = String.valueOf(anterior.get("tipoPago"));
        String estadoAnterior = String.valueOf(anterior.get("estado"));
        String estadoNuevo = String.valueOf(nuevo.get("estado"));
        if (!"SENA".equalsIgnoreCase(tipo) || !"PENDIENTE".equalsIgnoreCase(estadoAnterior)
                || !List.of("CONFIRMADO", "ANULADO").contains(estadoNuevo)) {
            throw new IllegalArgumentException("Solo se puede confirmar o anular una seña pendiente informada por el cliente.");
        }
        for (String campo : List.of("idReserva", "montoTotal", "metodoPago", "tipoPago", "fechaPago", "comprobante", "archivoComprobante")) {
            Object antes = anterior.get(campo);
            Object despues = nuevo.get(campo);
            if (antes instanceof BigDecimal montoAntes && despues instanceof BigDecimal montoDespues) {
                if (montoAntes.compareTo(montoDespues) != 0) throw new IllegalArgumentException("No se puede modificar el importe ni los datos de una seña informada.");
            } else if (antes == null ? despues != null : !antes.toString().equals(despues == null ? null : despues.toString())) {
                throw new IllegalArgumentException("No se puede modificar el importe ni los datos de una seña informada.");
            }
        }
    }

    /** Busca el pedido de una reserva y bloquea su lectura durante el cobro. */
    private static int pedidoDeReserva(Connection conexion, int idReserva) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT idPedido FROM detalle_pedido_alojamiento WHERE idReserva=? LIMIT 1 FOR UPDATE")) {
            sentencia.setInt(1, idReserva);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("La reserva no pertenece a un pedido web.");
                return resultados.getInt(1);
            }
        }
    }

    /** Obtiene los estados físicos afectados por un pedido antes de modificarlo. */
    private static List<Map<String, Object>> estadosAlojamientoPedido(Connection conexion, int idPedido) throws SQLException {
        List<Map<String, Object>> alojamientos = new ArrayList<>();
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT DISTINCT a.idAlojamiento,a.numero,a.estado FROM alojamientos a "
                        + "JOIN reservas r ON r.idAlojamiento=a.idAlojamiento "
                        + "JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva WHERE d.idPedido=? FOR UPDATE")) {
            consulta.setInt(1, idPedido);
            try (ResultSet resultados = consulta.executeQuery()) {
                while (resultados.next()) {
                    Map<String, Object> alojamiento = new LinkedHashMap<>();
                    alojamiento.put("idAlojamiento", resultados.getInt("idAlojamiento"));
                    alojamiento.put("numero", resultados.getString("numero"));
                    alojamiento.put("estado", resultados.getString("estado"));
                    alojamientos.add(alojamiento);
                }
            }
        }
        return alojamientos;
    }

    /** Lee el importe pendiente sin permitir que se cobre una estadía que aún no terminó. */
    private static ResumenCobroFinal leerResumenCobroFinal(Connection conexion, int idReserva, boolean bloquear) throws SQLException {
        String bloqueo = bloquear ? " FOR UPDATE" : "";
        int idPedido;
        BigDecimal total;
        BigDecimal pagado;
        Date egreso;
        String estado;
        String cliente;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT pa.idPedido,pa.total,r.fechaEgreso,r.estadoReserva,CONCAT(c.apellido,', ',c.nombre),"
                        + "(SELECT COALESCE(SUM(pg.montoTotal),0) FROM detalle_pedido_alojamiento dx JOIN pago pg ON pg.idReserva=dx.idReserva "
                        + "WHERE dx.idPedido=pa.idPedido AND UPPER(pg.estado)='CONFIRMADO' AND UPPER(pg.tipoPago) IN ('SENA','SALDO')) "
                        + "FROM reservas r JOIN cliente c ON c.idCliente=r.idCliente JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                        + "JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido WHERE r.idReserva=?" + bloqueo)) {
            sentencia.setInt(1, idReserva);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("No encontramos esa reserva.");
                idPedido = resultados.getInt(1); total = resultados.getBigDecimal(2); egreso = resultados.getDate(3);
                estado = resultados.getString(4); cliente = resultados.getString(5); pagado = resultados.getBigDecimal(6);
            }
        }
        LocalDate fechaOperativa = fechaOperativa();
        LocalTime horaOperativa = horaOperativa();
        if (egreso == null || egreso.toLocalDate().isAfter(fechaOperativa)
                || (egreso.toLocalDate().equals(fechaOperativa) && horaOperativa.isBefore(HORA_EGRESO))
                || !List.of("CONFIRMADA", "FINALIZADA").contains(estado.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("El saldo solo se puede cobrar cuando la estadía terminó.");
        }
        try (PreparedStatement pendientes = conexion.prepareStatement(
                "SELECT COUNT(*) FROM reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                + "WHERE d.idPedido=? AND UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA') "
                + "AND (r.fechaEgreso>? OR (r.fechaEgreso=? AND ?<?))")) {
            pendientes.setInt(1, idPedido);
            pendientes.setDate(2, Date.valueOf(fechaOperativa));
            pendientes.setDate(3, Date.valueOf(fechaOperativa));
            pendientes.setTime(4, Time.valueOf(horaOperativa));
            pendientes.setTime(5, Time.valueOf(HORA_EGRESO));
            try (ResultSet resultados = pendientes.executeQuery()) {
                resultados.next();
                if (resultados.getInt(1) > 0) throw new IllegalArgumentException(
                        "Este pedido incluye otra estadía que todavía no terminó; se cobra cuando finalicen todas sus reservas.");
            }
        }
        BigDecimal actividades;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COALESCE(SUM(montoTotal),0) FROM cargo_actividad WHERE idPedido=? AND fechaActividad<=? AND UPPER(estado)='PENDIENTE'")) {
            sentencia.setInt(1, idPedido);
            sentencia.setDate(2, Date.valueOf(fechaOperativa));
            try (ResultSet resultados = sentencia.executeQuery()) { resultados.next(); actividades = resultados.getBigDecimal(1); }
        }
        BigDecimal alojamiento = total.subtract(pagado).max(BigDecimal.ZERO);
        return new ResumenCobroFinal(cliente, alojamiento, actividades, alojamiento.add(actividades));
    }

    /** Recalcula el pedido después de confirmar una seña, saldo o reintegro. */
    private static void sincronizarPedidoPorId(Connection conexion, int idPedido) throws SQLException {
        BigDecimal total;
        BigDecimal porcentaje;
        int activas;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COALESCE(SUM(CASE WHEN UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO') THEN d.subtotal ELSE 0 END),0), "
                        + "SUM(CASE WHEN UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO') THEN 1 ELSE 0 END), pa.porcentajeSena "
                        + "FROM pedido_alojamiento pa JOIN detalle_pedido_alojamiento d ON d.idPedido=pa.idPedido "
                        + "JOIN reservas r ON r.idReserva=d.idReserva WHERE pa.idPedido=? GROUP BY pa.porcentajeSena")) {
            sentencia.setInt(1, idPedido);
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) return;
                total = resultados.getBigDecimal(1);
                activas = resultados.getInt(2);
                porcentaje = resultados.getBigDecimal(3);
            }
        }
        BigDecimal montoSena = total.multiply(porcentaje).divide(BigDecimal.valueOf(100));
        BigDecimal pagado = BigDecimal.ZERO;
        BigDecimal devuelto = BigDecimal.ZERO;
        BigDecimal reintegroPendiente = BigDecimal.ZERO;
        int pagosPendientes = 0;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COALESCE(SUM(CASE WHEN p.estado='CONFIRMADO' AND p.tipoPago IN ('SENA','SALDO') THEN p.montoTotal ELSE 0 END),0), "
                        + "COALESCE((SELECT SUM(monto) FROM reembolso WHERE idPedido=? AND estado='CONFIRMADO'),0), "
                        + "COALESCE((SELECT SUM(monto) FROM reembolso WHERE idPedido=? AND estado='PENDIENTE'),0), "
                        + "COALESCE(SUM(CASE WHEN p.estado='PENDIENTE' AND p.tipoPago='SENA' THEN 1 ELSE 0 END),0) "
                        + "FROM detalle_pedido_alojamiento d LEFT JOIN pago p ON p.idReserva=d.idReserva WHERE d.idPedido=?")) {
            sentencia.setInt(1,idPedido); sentencia.setInt(2,idPedido); sentencia.setInt(3,idPedido);
            try (ResultSet resultados=sentencia.executeQuery()) {
                resultados.next();
                pagado=resultados.getBigDecimal(1);
                devuelto=resultados.getBigDecimal(2);
                reintegroPendiente=resultados.getBigDecimal(3);
                pagosPendientes=resultados.getInt(4);
            }
        }
        BigDecimal neto = pagado.subtract(devuelto).max(BigDecimal.ZERO);
        boolean senaCompleta = activas > 0 && neto.compareTo(montoSena) >= 0;
        String nuevoEstadoPedido = activas == 0 ? "CANCELADO" : (senaCompleta ? "CONFIRMADO" : "ESPERANDO_SENA");
        String nuevoEstadoPago = activas == 0 ? (reintegroPendiente.signum()>0 || neto.signum()>0 ? "REEMBOLSO_PENDIENTE" : "CANCELADO")
                : (reintegroPendiente.signum()>0 ? "REEMBOLSO_PENDIENTE" : (neto.compareTo(total)>=0 ? "PAGADO" : (senaCompleta ? "SENA_PAGADA" : (pagosPendientes > 0 ? "SENA_INFORMADA" : "SENA_PENDIENTE"))));

        try (PreparedStatement sentencia = conexion.prepareStatement(
                "UPDATE pedido_alojamiento SET total=?, montoSena=?, estadoPedido=?, estadoPago=?, "
                        + "senaPagadaEn=CASE WHEN ? THEN COALESCE(senaPagadaEn, ?) ELSE NULL END, "
                        + "fechaLimiteRevisionSena=CASE WHEN ?='SENA_INFORMADA' THEN fechaLimiteRevisionSena ELSE NULL END "
                        + "WHERE idPedido=?")) {
            sentencia.setBigDecimal(1,total); sentencia.setBigDecimal(2,montoSena);
            sentencia.setString(3, nuevoEstadoPedido); sentencia.setString(4, nuevoEstadoPago);
            sentencia.setBoolean(5, senaCompleta); sentencia.setTimestamp(6, momentoOperativo());
            sentencia.setString(7, nuevoEstadoPago); sentencia.setInt(8, idPedido);
            sentencia.executeUpdate();
        }
        if (activas == 0) return;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "UPDATE reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                        + "SET r.estadoReserva=? "
                        + "WHERE d.idPedido=? AND UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA')")) {
            sentencia.setString(1, senaCompleta || "PAGADO".equalsIgnoreCase(nuevoEstadoPago)
                    ? "CONFIRMADA" : "PENDIENTE_SENA");
            sentencia.setInt(2, idPedido);
            sentencia.executeUpdate();
        }
    }

    /**
     * Comprueba rol usuario y evita continuar cuando hay información inválida.
     */
    private static void validarRolUsuario(Map<String, Object> datos, Integer id) throws SQLException {
        if (SesionActual.get() == null || !List.of("ADMINISTRADOR", "SUPERADMIN").contains(SesionActual.get().getRol())) {
            throw new SQLException("Solo un administrador puede gestionar usuarios.");
        }
        Object rolNuevo = datos.get("rol");
        if (id == null && !esSuperAdmin() && !"ADMINISTRADOR".equals(rolNuevo)) {
            throw new SQLException("Desde la aplicación solo se pueden crear usuarios con rol ADMINISTRADOR.");
        }
        if ("SUPERADMIN".equals(rolNuevo) && !esSuperAdmin()) {
            throw new SQLException("Solo un SuperAdmin puede asignar el rol SUPERADMIN.");
        }
        if (id != null && !esSuperAdmin()) {
            try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT rol FROM usuarios WHERE idUsuario = ?")) {
                sentencia.setInt(1, id);
                try (ResultSet resultados = sentencia.executeQuery()) {
                    if (resultados.next() && "SUPERADMIN".equals(resultados.getString(1))) {
                        throw new SQLException("Solo un SuperAdmin puede modificar una cuenta SuperAdmin.");
                    }
                }
            }
        }
    }

    /**
     * Convierte los datos al formato que necesita el resto del sistema.
     */
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

    /**
     * Comprueba email y evita continuar cuando hay información inválida.
     */
    private static void validarEmail(String correo) {
        if (correo == null || !correo.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Ingresá un email válido.");
        }
    }

    /**
     * Comprueba reserva y evita continuar cuando hay información inválida.
     */
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
        if (id != null) {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT COUNT(*) FROM `reservaAcompañante` WHERE idReserva = ?")) {
                sentencia.setInt(1, id);
                try (ResultSet resultados = sentencia.executeQuery()) {
                    if (resultados.next() && personas < resultados.getInt(1) + 1) {
                        throw new IllegalArgumentException("La cantidad de personas no puede ser menor que el titular y sus acompañantes ya asociados.");
                    }
                }
            }
        }
        BigDecimal total = (BigDecimal) datos.get("precioTotal");
        if (total.signum() < 0) throw new IllegalArgumentException("El precio total no puede ser negativo.");
        if (total.signum() == 0) {
            long noches = ChronoUnit.DAYS.between(ingreso, egreso);
            datos.put("precioTotal", precioNoche.multiply(BigDecimal.valueOf(noches)));
        }
    }

    /**
     * Registra la situación real de llegada sin alterar las fechas contratadas.
     * La seña confirma la reserva; la presencia se confirma solamente en recepción.
     */
    private static void validarEstadoLlegada(Connection conexion, Integer id, String nuevoEstado) throws SQLException {
        if (id == null) {
            throw new IllegalArgumentException("Recepción no crea reservas desde Llegadas y estadías.");
        }
        if (!List.of("PENDIENTE_LLEGADA", "EN_ESTADIA", "NO_SE_PRESENTO", "FINALIZADA").contains(nuevoEstado)) {
            throw new IllegalArgumentException("La situación de llegada no es válida.");
        }
        if ("FINALIZADA".equals(nuevoEstado)) {
            throw new IllegalArgumentException("La salida se cierra desde «Cobrar estadía final».");
        }

        LocalDate ingreso;
        LocalDate egreso;
        String estadoReserva;
        String estadoActual;
        Timestamp checkIn;
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT fechaIngreso,fechaEgreso,estadoReserva,estadoLlegada,checkInReal "
                        + "FROM reservas WHERE idReserva=? FOR UPDATE")) {
            consulta.setInt(1, id);
            try (ResultSet resultados = consulta.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("La reserva seleccionada no existe.");
                ingreso = resultados.getDate("fechaIngreso").toLocalDate();
                egreso = resultados.getDate("fechaEgreso").toLocalDate();
                estadoReserva = resultados.getString("estadoReserva");
                estadoActual = resultados.getString("estadoLlegada");
                checkIn = resultados.getTimestamp("checkInReal");
            }
        }
        if (!"CONFIRMADA".equalsIgnoreCase(estadoReserva)) {
            throw new IllegalArgumentException("Solo se puede actualizar la llegada de una estadía confirmada.");
        }
        if ("FINALIZADA".equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La estadía ya fue finalizada.");
        }
        LocalDate hoy = fechaOperativa();
        LocalTime hora = horaOperativa();
        if ("EN_ESTADIA".equals(nuevoEstado)) {
            if (hoy.isBefore(ingreso) || hoy.isAfter(egreso)) {
                throw new IllegalArgumentException("El check-in solo se registra entre la fecha de ingreso y la de egreso contratadas.");
            }
            if (hoy.equals(ingreso) && hora.isBefore(HORA_INGRESO)) {
                throw new IllegalArgumentException("El check-in se registra desde las 14:00 del día de ingreso.");
            }
            if (hoy.equals(egreso) && !hora.isBefore(HORA_EGRESO)) {
                throw new IllegalArgumentException("No se puede registrar una llegada después de las 10:00 del día de egreso.");
            }
            try (PreparedStatement marcarLlegada = conexion.prepareStatement(
                    "UPDATE reservas SET checkInReal=COALESCE(checkInReal,?) WHERE idReserva=?")) {
                marcarLlegada.setTimestamp(1, momentoOperativo());
                marcarLlegada.setInt(2, id);
                marcarLlegada.executeUpdate();
            }
            // El estado físico se actualiza al registrar la llegada. Al cobrar el
            // saldo final, registrarCobroFinal lo mueve a «En limpieza».
            try (PreparedStatement marcarOcupado = conexion.prepareStatement(
                    "UPDATE alojamientos a JOIN reservas r ON r.idAlojamiento=a.idAlojamiento "
                            + "SET a.estado='Ocupado' WHERE r.idReserva=? "
                            + "AND a.estado NOT IN ('En mantenimiento','Fuera de servicio')")) {
                marcarOcupado.setInt(1, id);
                marcarOcupado.executeUpdate();
            }
        }
        if ("NO_SE_PRESENTO".equals(nuevoEstado)) {
            if (hoy.isBefore(ingreso)) {
                throw new IllegalArgumentException("La no presentación se puede registrar desde la fecha de ingreso.");
            }
            if (hoy.equals(ingreso) && hora.isBefore(HORA_INGRESO)) {
                throw new IllegalArgumentException("La no presentación se puede registrar desde las 14:00 del día de ingreso.");
            }
            if (checkIn != null) {
                throw new IllegalArgumentException("No se puede marcar ausencia después de haber registrado el check-in.");
            }
        }
        if ("PENDIENTE_LLEGADA".equals(nuevoEstado) && checkIn != null) {
            throw new IllegalArgumentException("No se puede volver a pendiente una estadía que ya registró su llegada.");
        }
    }

    /**
     * Comprueba cupo y evita continuar cuando hay información inválida.
     */
    private static void validarCupo(Connection conexion, Map<String, Object> datos) throws SQLException {
        // Bloquea la inscripcion si la actividad esta inactiva o la fecha elegida ya no tiene lugares.
        LocalDate fechaActividad = ((Date) datos.get("fechaActividad")).toLocalDate();
        if (fechaActividad == null) {
            throw new IllegalArgumentException("Elegí el día de la actividad.");
        }
        int cupoMax;
        String diasHabilitados;
        String horario;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT cupoMax, activa, dias, horario FROM Actividad WHERE idActividad = ? FOR UPDATE")) {
            sentencia.setInt(1, (Integer) datos.get("idActividad"));
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("La actividad seleccionada no existe.");
                cupoMax = resultados.getInt(1);
                if (!resultados.getBoolean(2)) throw new IllegalArgumentException("La actividad no está habilitada para inscripciones.");
                diasHabilitados = resultados.getString(3);
                horario = resultados.getString(4);
            }
        }
        validarFechaActividad(fechaActividad, diasHabilitados);
        validarCruceHorarioActividad(conexion, (Integer) datos.get("idCliente"),
                (Integer) datos.get("idActividad"), fechaActividad, horario);
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT 1 FROM inscripcionActividad WHERE idCliente=? AND idActividad=? "
                        + "AND fechaActividad=? AND UPPER(estado)='ACTIVA' LIMIT 1")) {
            sentencia.setInt(1, (Integer) datos.get("idCliente"));
            sentencia.setInt(2, (Integer) datos.get("idActividad"));
            sentencia.setDate(3, Date.valueOf(fechaActividad));
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next()) throw new IllegalArgumentException("El cliente ya tiene una inscripción activa para esta actividad y fecha.");
            }
        }
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT 1 FROM reservas r "
                        + "JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                        + "JOIN pedido_alojamiento p ON p.idPedido=d.idPedido "
                        + "WHERE r.idCliente=? AND UPPER(r.estadoReserva)='CONFIRMADA' "
                        + "AND UPPER(p.estadoPago) IN ('SENA_PAGADA','PAGADO') "
                        + "AND ? >= r.fechaIngreso AND ? < r.fechaEgreso LIMIT 1")) {
            sentencia.setInt(1, (Integer) datos.get("idCliente"));
            sentencia.setDate(2, Date.valueOf(fechaActividad));
            sentencia.setDate(3, Date.valueOf(fechaActividad));
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) {
                    throw new IllegalArgumentException(
                            "El cliente necesita una estadía confirmada que incluya el día de la actividad.");
                }
            }
        }
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COUNT(DISTINCT CASE WHEN ia.participaTitular = 1 AND UPPER(ia.estado)='ACTIVA' THEN ia.idInscripcion END) "
                        + "+ COUNT(iaa.idInscripcionAcompanante) "
                        + "FROM inscripcionActividad ia "
                        + "LEFT JOIN inscripcionActividadAcompanante iaa "
                        + "ON iaa.idClienteTitular = ia.idCliente AND iaa.idActividad = ia.idActividad AND iaa.fechaActividad = ia.fechaActividad "
                        + "AND iaa.activo = 1 "
                        + "WHERE ia.idActividad = ? AND ia.fechaActividad = ? AND UPPER(ia.estado)='ACTIVA'")) {
            sentencia.setInt(1, (Integer) datos.get("idActividad"));
            sentencia.setDate(2, Date.valueOf(fechaActividad));
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (resultados.next() && resultados.getInt(1) >= cupoMax) {
                    throw new IllegalArgumentException("La actividad alcanzó su cupo máximo para ese día.");
                }
            }
        }
    }

    /**
     * Verifica que la fecha elegida sea próxima y coincida con los días
     * configurados por coordinación para esa actividad.
     */
    private static void validarFechaActividad(LocalDate fecha, String diasConfigurados) {
        // En una demostración la fecha puede configurarse localmente; en uso
        // normal fechaOperativa() devuelve la fecha real del equipo.
        LocalDate hoy = fechaOperativa();
        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(90))) {
            throw new IllegalArgumentException("La fecha debe estar entre hoy y los próximos 90 días.");
        }
        String configuracion = normalizarTexto(diasConfigurados == null ? "" : diasConfigurados);
        if (configuracion.isBlank() || configuracion.contains("todos los dias")) return;

        String dia = switch (fecha.getDayOfWeek()) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sabado";
            case SUNDAY -> "domingo";
        };
        if (!configuracion.contains(dia)) {
            throw new IllegalArgumentException("La actividad no se realiza el día de la fecha elegida.");
        }
    }

    /** Evita dos actividades con inicio y fin explícitos que se superpongan para el mismo cliente. */
    private static void validarCruceHorarioActividad(Connection conexion, int idCliente, int idActividad,
                                                     LocalDate fechaActividad, String horarioSolicitado) throws SQLException {
        LocalTime[] solicitado = intervaloHorario(horarioSolicitado);
        if (solicitado == null) return;
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT a.nombre,a.horario FROM inscripcionActividad ia JOIN Actividad a ON a.idActividad=ia.idActividad "
                        + "WHERE ia.idCliente=? AND ia.idActividad<>? AND ia.fechaActividad=? "
                        + "AND UPPER(ia.estado)='ACTIVA'")) {
            consulta.setInt(1, idCliente);
            consulta.setInt(2, idActividad);
            consulta.setDate(3, Date.valueOf(fechaActividad));
            try (ResultSet resultados = consulta.executeQuery()) {
                while (resultados.next()) {
                    LocalTime[] existente = intervaloHorario(resultados.getString("horario"));
                    if (existente != null && solicitado[0].isBefore(existente[1]) && existente[0].isBefore(solicitado[1])) {
                        throw new IllegalArgumentException("El horario se superpone con la actividad “"
                                + resultados.getString("nombre") + "” (" + resultados.getString("horario") + ").");
                    }
                }
            }
        }
    }

    /** Convierte un horario “HH:MM a HH:MM” en un intervalo; los demás formatos no se inventan. */
    private static LocalTime[] intervaloHorario(String horario) {
        if (horario == null) return null;
        Matcher coincidencia = HORARIO_CON_FIN.matcher(horario);
        if (!coincidencia.find()) return null;
        LocalTime inicio = LocalTime.of(Integer.parseInt(coincidencia.group(1)), Integer.parseInt(coincidencia.group(2)));
        LocalTime fin = LocalTime.of(Integer.parseInt(coincidencia.group(3)), Integer.parseInt(coincidencia.group(4)));
        return fin.isAfter(inicio) ? new LocalTime[]{inicio, fin} : null;
    }

    /** Quita acentos y pasa un texto a minúsculas para compararlo con seguridad. */
    private static String normalizarTexto(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }

    /**
     * Comprueba actividad y evita continuar cuando hay información inválida.
     */
    private static void validarActividad(Connection conexion, Map<String, Object> datos, Integer idActividad)
            throws SQLException {
        // Aplica límites básicos antes de guardar los datos de la actividad.
        int cupo = (Integer) datos.get("cupoMax");
        if (cupo < 1) {
            throw new IllegalArgumentException("El cupo máximo debe ser al menos 1.");
        }
        BigDecimal precio = (BigDecimal) datos.get("precio");
        if (precio.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        if (intervaloHorario(String.valueOf(datos.get("horario"))) == null) {
            throw new IllegalArgumentException("El horario debe indicar inicio y fin, por ejemplo: 15:00 a 16:00.");
        }
        if (String.valueOf(datos.get("dias")).isBlank()) {
            throw new IllegalArgumentException("Indicá los días en que se realiza la actividad.");
        }
        String modalidad = String.valueOf(datos.get("modalidad"));
        if ("incluida".equals(modalidad)) {
            datos.put("precio", BigDecimal.ZERO);
            datos.put("pago", "Incluida");
        } else if (precio.signum() <= 0) {
            throw new IllegalArgumentException("Una actividad con inscripción debe tener un precio mayor que cero.");
        } else {
            datos.put("pago", "Con inscripción");
        }

        // No se permite bajar el cupo por debajo de las personas ya anotadas
        // en una misma fecha. Así no quedan sobrecupos al editar desde Java.
        if (idActividad != null) {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT COALESCE(MAX(porFecha.total),0) FROM ("
                            + "SELECT ia.fechaActividad, "
                            + "COUNT(DISTINCT CASE WHEN ia.participaTitular=1 AND UPPER(ia.estado)='ACTIVA' THEN ia.idInscripcion END) "
                            + "+ COUNT(iaa.idInscripcionAcompanante) AS total "
                            + "FROM inscripcionActividad ia LEFT JOIN inscripcionActividadAcompanante iaa "
                            + "ON iaa.idClienteTitular=ia.idCliente AND iaa.idActividad=ia.idActividad "
                            + "AND iaa.fechaActividad=ia.fechaActividad AND iaa.activo=1 "
                            + "WHERE ia.idActividad=? AND UPPER(ia.estado)='ACTIVA' "
                            + "GROUP BY ia.fechaActividad) porFecha")) {
                sentencia.setInt(1, idActividad);
                try (ResultSet resultados = sentencia.executeQuery()) {
                    resultados.next();
                    int mayorCantidadInscripta = resultados.getInt(1);
                    if (cupo < mayorCantidadInscripta) {
                        throw new IllegalArgumentException("El cupo no puede ser menor que las "
                                + mayorCantidadInscripta + " personas ya anotadas en una fecha.");
                    }
                }
            }
        }
    }

    /**
     * Ejecuta la lógica de “q” en GestionDAO.java.
     */
    private static String q(String identificador) {
        // Acepta solo nombres SQL simples antes de agregarlos a una consulta.
        if (!identificador.matches("[\\p{L}_][\\p{L}\\p{N}_]*")) {
            throw new IllegalArgumentException("Identificador SQL no válido.");
        }
        return "`" + identificador + "`";
    }
}
