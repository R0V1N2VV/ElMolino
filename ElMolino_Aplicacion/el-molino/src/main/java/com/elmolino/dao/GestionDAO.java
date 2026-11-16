package com.elmolino.dao;

import com.elmolino.util.Conexion;
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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reune las consultas y reglas para los modulos de gestion. */
public final class GestionDAO {

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
            opcion("estado", "Estado", List.of("Disponible", "En limpieza", "Fuera de servicio", "Reservado"))));

    public static final Modulo ACTIVIDADES = new Modulo("Actividades", "Actividad", "idActividad", List.of(
            texto("slug", "Identificador web (sin espacios)", true), texto("nombre", "Nombre", true),
            relacion("idCategoria", "Categoría", "SELECT idCategoria, nombre FROM `CategoriaActividad` WHERE activa = 1 ORDER BY nombre"),
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
            opcion("estadoReserva", "Estado", List.of("PENDIENTE_SENA", "CONFIRMADA", "CANCELADA", "FINALIZADA")),
            decimal("precioTotal", "Precio total (0 calcula base)")));

    public static final Modulo ACTIVIDAD_INSCRIPTOS = new Modulo("Inscripciones", "inscripcionActividad", "idInscripcion", List.of(
            relacion("idCliente", "Cliente", "SELECT idCliente, CONCAT(apellido, ', ', nombre, ' - DNI ', dni) FROM `cliente` ORDER BY apellido, nombre"),
            relacion("idActividad", "Actividad", "SELECT idActividad, nombre FROM `Actividad` ORDER BY nombre"),
            fecha("fechaActividad", "Día elegido (AAAA-MM-DD)"),
            fecha("fechaInscripcion", "Fecha de inscripción (AAAA-MM-DD)"),
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
                case "ADMINISTRADOR" -> "usuarios".equals(tabla);
                case "COORDINADOR" -> "inscripcionActividad".equals(tabla);
                case "ADMINISTRACION" -> "reservaAcompañante".equals(tabla);
                default -> false;
            };
            case EDITAR -> tieneAccesoTabla(tabla, rol)
                    && (!"pago".equals(tabla) || "SUPERADMIN".equals(rol))
                    && (!"usuarios".equals(tabla) || List.of("ADMINISTRADOR", "SUPERADMIN").contains(rol))
                    && (!"inscripcionActividad".equals(tabla) || "SUPERADMIN".equals(rol));
            case ELIMINAR -> tieneAccesoTabla(tabla, rol)
                    && (!List.of("pago", "inscripcionActividad").contains(tabla) || "SUPERADMIN".equals(rol))
                    && (!"usuarios".equals(tabla) || List.of("ADMINISTRADOR", "SUPERADMIN").contains(rol))
                    && (!"alojamientos".equals(tabla) || !List.of("RECEPCION", "LIMPIEZA").contains(rol));
        };
        if (!permitido) throw new SQLException("Tu rol no puede " + operacion.name().toLowerCase(Locale.ROOT)
                + " registros de " + seccion + ".");
    }

    /** Cuenta las transferencias informadas que todavía requieren revisión humana. */
    public static int contarSeniasPorConfirmar() throws SQLException {
        try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COUNT(DISTINCT d.idPedido) FROM pago p JOIN detalle_pedido_alojamiento d ON d.idReserva=p.idReserva "
                        + "JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido WHERE p.tipoPago='SENA' AND p.estado='PENDIENTE' "
                        + "AND pa.estadoPago='SENA_INFORMADA'" ); ResultSet resultados = sentencia.executeQuery()) {
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
            case "RECEPCION" -> List.of("cliente", "alojamientos", "pago", "reembolso").contains(tabla);
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

    /** Lista únicamente estadías ya terminadas que todavía tienen algo por cobrar. */
    public static List<Opcion> reservasParaCobroFinal() throws SQLException {
        validarAcceso(PAGOS);
        return opciones("SELECT r.idReserva, CONCAT('Reserva ',r.idReserva,' · ',c.apellido,', ',c.nombre) "
                + "FROM reservas r JOIN cliente c ON c.idCliente=r.idCliente "
                + "JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva "
                + "JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido "
                + "WHERE r.fechaEgreso<=CURRENT_DATE AND UPPER(r.estadoReserva) IN ('CONFIRMADA','FINALIZADA') "
                + "AND UPPER(pa.estadoPago) IN ('SENA_PAGADA','PAGADO') ORDER BY r.fechaEgreso DESC, r.idReserva DESC");
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
                if (resumen.total().signum() <= 0) throw new IllegalArgumentException("Esta estadía ya no tiene saldo pendiente.");
                int idPedido = pedidoDeReserva(conexion, idReserva);
                try (PreparedStatement insertar = conexion.prepareStatement(
                        "INSERT INTO pago(idReserva,montoTotal,metodoPago,tipoPago,estado,fechaPago,comprobante) VALUES (?,?,? ,?, 'CONFIRMADO',CURRENT_DATE,?)")) {
                    if (resumen.saldoAlojamiento().signum() > 0) {
                        insertar.setInt(1, idReserva); insertar.setBigDecimal(2, resumen.saldoAlojamiento());
                        insertar.setString(3, metodoPago); insertar.setString(4, "SALDO"); insertar.setString(5, comprobante);
                        insertar.executeUpdate();
                    }
                    if (resumen.actividadesPendientes().signum() > 0) {
                        insertar.setInt(1, idReserva); insertar.setBigDecimal(2, resumen.actividadesPendientes());
                        insertar.setString(3, metodoPago); insertar.setString(4, "ACTIVIDAD"); insertar.setString(5, comprobante);
                        insertar.executeUpdate();
                    }
                }
                try (PreparedStatement cargos = conexion.prepareStatement(
                        "UPDATE cargo_actividad SET estado='PAGADO', actualizadoEn=NOW() WHERE idPedido=? AND UPPER(estado)='PENDIENTE'")) {
                    cargos.setInt(1, idPedido); cargos.executeUpdate();
                }
                try (PreparedStatement finalizar = conexion.prepareStatement("UPDATE reservas SET estadoReserva='FINALIZADA' WHERE idReserva=?")) {
                    finalizar.setInt(1, idReserva); finalizar.executeUpdate();
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
                + "      ON iaa.idClienteTitular = ia.idCliente AND iaa.idActividad = ia.idActividad "
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
                    if (rolLimpieza && !"estado".equals(campo.columna())) continue;
                    if (rolRecepcion && gestionAlojamientos && !"estado".equals(campo.columna())) continue;
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
                if ("reservas".equals(modulo.tabla())) validarReserva(conexion, datos, id);
                if ("Actividad".equals(modulo.tabla())) validarActividad(datos);
                if ("inscripcionActividad".equals(modulo.tabla()) && id == null) validarCupo(conexion, datos);
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
            try(PreparedStatement s=conexion.prepareStatement(
                    "UPDATE pedido_alojamiento pa LEFT JOIN (SELECT d.idPedido,SUM(CASE WHEN p.estado='CONFIRMADO' AND p.tipoPago IN ('SENA','SALDO') THEN p.montoTotal ELSE 0 END) pagado FROM detalle_pedido_alojamiento d LEFT JOIN pago p ON p.idReserva=d.idReserva GROUP BY d.idPedido) x ON x.idPedido=pa.idPedido "
                            + "SET pa.estadoPedido='VENCIDO',pa.estadoPago='VENCIDO',pa.canceladoEn=NOW() WHERE pa.fechaLimiteSena<NOW() AND pa.estadoPedido IN ('ESPERANDO_SENA','PENDIENTE') AND pa.estadoPago='SENA_PENDIENTE' AND COALESCE(x.pagado,0)<pa.montoSena")){
                s.executeUpdate();
            }
            try(PreparedStatement s=conexion.prepareStatement("UPDATE reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido SET r.estadoReserva='CANCELADA' WHERE pa.estadoPedido='VENCIDO' AND UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA')")){s.executeUpdate();}
            try(PreparedStatement s=conexion.prepareStatement("UPDATE pedido_alojamiento SET estadoPedido='VENCIDO',estadoPago='PAGO_RECHAZADO',fechaLimiteRevisionSena=NULL,canceladoEn=NOW() WHERE estadoPago='SENA_INFORMADA' AND fechaLimiteRevisionSena<NOW()")){s.executeUpdate();}
            try(PreparedStatement s=conexion.prepareStatement("UPDATE pago p JOIN detalle_pedido_alojamiento d ON d.idReserva=p.idReserva JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido SET p.estado='ANULADO',p.observacion='La recepción no pudo confirmar la transferencia dentro de las 24 horas.' WHERE pa.estadoPago='PAGO_RECHAZADO' AND p.estado='PENDIENTE'")){s.executeUpdate();}
            try(PreparedStatement s=conexion.prepareStatement("UPDATE reservas r JOIN detalle_pedido_alojamiento d ON d.idReserva=r.idReserva JOIN pedido_alojamiento pa ON pa.idPedido=d.idPedido SET r.estadoReserva='CANCELADA' WHERE pa.estadoPago='PAGO_RECHAZADO' AND UPPER(r.estadoReserva) NOT IN ('CANCELADA','CANCELADO','FINALIZADA')")){s.executeUpdate();}
            conexion.commit();
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
                            + "AND ? BETWEEN r.fechaIngreso AND r.fechaEgreso LIMIT 1")) {
                sentencia.setInt(1, idCliente);
                sentencia.setDate(2, fecha);
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
                                + "AND ? BETWEEN r.fechaIngreso AND r.fechaEgreso LIMIT 1")) {
                    sentencia.setInt(1, idsAcompanantes.get(indice));
                    sentencia.setDate(2, fecha);
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
                            + "WHERE iaa.idClienteTitular=ia.idCliente AND iaa.idActividad=ia.idActividad AND iaa.activo=1) "
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
                        + "WHERE iaa.idClienteTitular=ia.idCliente AND iaa.idActividad=ia.idActividad AND iaa.activo=1) "
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

    /**
     * Comprueba es super admin y devuelve verdadero o falso según se cumpla la condición.
     */
    private static boolean esSuperAdmin() {
        return SesionActual.get() != null && "SUPERADMIN".equals(SesionActual.get().getRol());
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
        List<String> estadosPermitidos = rolLimpieza
                ? List.of("En limpieza", "Disponible")
                : List.of("Disponible", "Reservado", "Fuera de servicio");
        if (!estadosPermitidos.contains(nuevoEstado)) {
            throw new SQLException(rolLimpieza
                    ? "Limpieza solo puede marcar un alojamiento como En limpieza o Disponible."
                    : "Recepción solo puede elegir Disponible, Reservado o Fuera de servicio.");
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
        if (egreso == null || egreso.toLocalDate().isAfter(LocalDate.now())
                || !List.of("CONFIRMADA", "FINALIZADA").contains(estado.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("El saldo solo se puede cobrar cuando la estadía terminó.");
        }
        BigDecimal actividades;
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COALESCE(SUM(montoTotal),0) FROM cargo_actividad WHERE idPedido=? AND UPPER(estado)='PENDIENTE'")) {
            sentencia.setInt(1, idPedido);
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
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT COALESCE(SUM(CASE WHEN p.estado='CONFIRMADO' AND p.tipoPago IN ('SENA','SALDO') THEN p.montoTotal ELSE 0 END),0), "
                        + "COALESCE((SELECT SUM(monto) FROM reembolso WHERE idPedido=? AND estado='CONFIRMADO'),0), "
                        + "COALESCE((SELECT SUM(monto) FROM reembolso WHERE idPedido=? AND estado='PENDIENTE'),0) "
                        + "FROM detalle_pedido_alojamiento d LEFT JOIN pago p ON p.idReserva=d.idReserva WHERE d.idPedido=?")) {
            sentencia.setInt(1,idPedido); sentencia.setInt(2,idPedido); sentencia.setInt(3,idPedido);
            try (ResultSet resultados=sentencia.executeQuery()) { resultados.next(); pagado=resultados.getBigDecimal(1); devuelto=resultados.getBigDecimal(2); reintegroPendiente=resultados.getBigDecimal(3); }
        }
        BigDecimal neto = pagado.subtract(devuelto).max(BigDecimal.ZERO);
        boolean senaCompleta = activas > 0 && neto.compareTo(montoSena) >= 0;
        String nuevoEstadoPedido = activas == 0 ? "CANCELADO" : (senaCompleta ? "CONFIRMADO" : "ESPERANDO_SENA");
        String nuevoEstadoPago = activas == 0 ? (reintegroPendiente.signum()>0 || neto.signum()>0 ? "REEMBOLSO_PENDIENTE" : "CANCELADO")
                : (reintegroPendiente.signum()>0 ? "REEMBOLSO_PENDIENTE" : (neto.compareTo(total)>=0 ? "PAGADO" : (senaCompleta ? "SENA_PAGADA" : "SENA_PENDIENTE")));

        try (PreparedStatement sentencia = conexion.prepareStatement(
                "UPDATE pedido_alojamiento SET total=?, montoSena=?, estadoPedido=?, estadoPago=?, "
                        + "senaPagadaEn=CASE WHEN ? THEN COALESCE(senaPagadaEn, NOW()) ELSE NULL END, "
                        + "fechaLimiteRevisionSena=CASE WHEN ?='SENA_INFORMADA' THEN fechaLimiteRevisionSena ELSE NULL END "
                        + "WHERE idPedido=?")) {
            sentencia.setBigDecimal(1,total); sentencia.setBigDecimal(2,montoSena);
            sentencia.setString(3, nuevoEstadoPedido); sentencia.setString(4, nuevoEstadoPago);
            sentencia.setBoolean(5, senaCompleta); sentencia.setString(6, nuevoEstadoPago); sentencia.setInt(7, idPedido);
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
        if (id == null && !"ADMINISTRADOR".equals(rolNuevo)) {
            throw new SQLException("Desde la aplicación solo se pueden crear usuarios con rol ADMINISTRADOR.");
        }
        if ("SUPERADMIN".equals(rolNuevo)) {
            boolean yaEsSuperAdmin = false;
            if (id != null) {
                try (Connection conexion = Conexion.obtener(); PreparedStatement sentencia = conexion.prepareStatement(
                        "SELECT rol FROM usuarios WHERE idUsuario = ?")) {
                    sentencia.setInt(1, id);
                    try (ResultSet resultados = sentencia.executeQuery()) {
                        yaEsSuperAdmin = resultados.next() && "SUPERADMIN".equals(resultados.getString(1));
                    }
                }
            }
            if (!yaEsSuperAdmin) {
                throw new SQLException("El rol SUPERADMIN solo se asigna directamente en la base de datos.");
            }
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
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT cupoMax, activa, dias FROM Actividad WHERE idActividad = ? FOR UPDATE")) {
            sentencia.setInt(1, (Integer) datos.get("idActividad"));
            try (ResultSet resultados = sentencia.executeQuery()) {
                if (!resultados.next()) throw new IllegalArgumentException("La actividad seleccionada no existe.");
                cupoMax = resultados.getInt(1);
                if (!resultados.getBoolean(2)) throw new IllegalArgumentException("La actividad no está habilitada para inscripciones.");
                diasHabilitados = resultados.getString(3);
            }
        }
        validarFechaActividad(fechaActividad, diasHabilitados);
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
                        + "AND ? BETWEEN r.fechaIngreso AND r.fechaEgreso LIMIT 1")) {
            sentencia.setInt(1, (Integer) datos.get("idCliente"));
            sentencia.setDate(2, Date.valueOf(fechaActividad));
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
                        + "ON iaa.idClienteTitular = ia.idCliente AND iaa.idActividad = ia.idActividad "
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
        LocalDate hoy = LocalDate.now();
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

    /** Quita acentos y pasa un texto a minúsculas para compararlo con seguridad. */
    private static String normalizarTexto(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }

    /**
     * Comprueba actividad y evita continuar cuando hay información inválida.
     */
    private static void validarActividad(Map<String, Object> datos) {
        // Aplica limites basicos antes de guardar los datos de la actividad.
        if (((Integer) datos.get("cupoMax")) < 1) {
            throw new IllegalArgumentException("El cupo máximo debe ser al menos 1.");
        }
        if (((BigDecimal) datos.get("precio")).signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
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
