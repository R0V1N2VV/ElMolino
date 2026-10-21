package com.elmolino.ui;

import com.elmolino.dao.GestionDAO;
import com.elmolino.dao.GestionDAO.Campo;
import com.elmolino.dao.GestionDAO.Modulo;
import com.elmolino.dao.GestionDAO.Opcion;
import com.elmolino.dao.GestionDAO.Tipo;
import com.elmolino.util.SesionActual;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Ventana reutilizable para listar y mantener los datos de un modulo. */
public class VentanaGestion extends JFrame {

    private record OpcionEnum(String valor, String etiqueta) {
        @Override public String toString() { return etiqueta; }
    }

    private final VentanaPrincipal principal;
    private final Modulo modulo;
    private final List<Campo> visibles;
    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final JLabel estado = new JLabel(" ");
    private final JButton botonNuevo = new JButton("Nuevo");
    private final JButton botonEditar = new JButton("Editar");
    private final JButton botonEliminar = new JButton("Eliminar");

    public VentanaGestion(VentanaPrincipal principal, Modulo modulo) {
        super("El Molino - " + modulo.titulo());
        this.principal = principal;
        this.modulo = modulo;
        // Ajusta los botones visibles segun los permisos y las reglas de cada modulo.
        boolean pagosSoloLectura = "pago".equals(modulo.tabla());
        boolean soloAltaInscripcion = "inscripcionActividad".equals(modulo.tabla());
        boolean altaUsuarioAdmin = "usuarios".equals(modulo.tabla()) && esAdministrador();
        // Los alojamientos se crean en la web; aqui solo se crean inscripciones y usuarios administradores.
        botonNuevo.setVisible(soloAltaInscripcion || altaUsuarioAdmin);
        if (pagosSoloLectura) {
            botonNuevo.setVisible(false);
            botonEditar.setVisible(false);
            botonEliminar.setVisible(false);
        }
        if (soloAltaInscripcion) {
            botonEditar.setVisible(false);
            botonEliminar.setVisible(false);
        }
        if ("alojamientos".equals(modulo.tabla()) && esLimpieza()) {
            botonNuevo.setVisible(false);
            botonEliminar.setVisible(false);
        }
        if ("usuarios".equals(modulo.tabla()) && !esAdministrador()) {
            botonNuevo.setVisible(false);
            botonEditar.setVisible(false);
            botonEliminar.setVisible(false);
        }
        if ("reservas".equals(modulo.tabla())) botonEliminar.setText("Cancelar reserva");
        this.visibles = GestionDAO.camposVisibles(modulo);
        // Define las columnas de la tabla; actividades usa datos calculados adicionales.
        String[] columnas;
        if (esModuloActividades()) {
            columnas = new String[]{"ID", "Actividad", "Descripción breve", "Horario", "Días", "Momento",
                    "Duración", "Sector", "Responsable", "Edad", "Modalidad", "Activa", "Espacio",
                    "Inscriptos / cupo", "Disponibles", "Estado del cupo", "Precio", "Pago"};
        } else {
            columnas = new String[visibles.size() + 1];
            columnas[0] = "ID";
            for (int indice = 0; indice < visibles.size(); indice++) columnas[indice + 1] = visibles.get(indice).etiqueta();
        }
        this.modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        this.tabla = new JTable(modelo);
        if (esModuloActividades()) {
            tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            int[] anchos = {55, 180, 200, 105, 115, 95, 95, 110, 135, 120, 110, 65, 150, 110, 90, 125, 95, 105};
            for (int indice = 0; indice < anchos.length; indice++) tabla.getColumnModel().getColumn(indice).setPreferredWidth(anchos[indice]);
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1050, 580);
        setMinimumSize(new java.awt.Dimension(760, 420));
        setLocationRelativeTo(principal);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton botonVolver = new JButton("← Salir");
        botonVolver.setToolTipText("Salir de esta sección y volver al menú principal");
        botonVolver.setForeground(java.awt.Color.WHITE);
        botonVolver.setBackground(new java.awt.Color(0x4F7358));
        botonVolver.setOpaque(true);
        botonVolver.setContentAreaFilled(true);
        JButton botonActualizar = new JButton("Actualizar");
        botonVolver.addActionListener(evento -> dispose());
        botonActualizar.addActionListener(evento -> cargar());
        botonNuevo.addActionListener(evento -> mostrarFormulario(null));
        botonEditar.addActionListener(evento -> editarSeleccionado());
        botonEliminar.addActionListener(evento -> eliminarSeleccionado());

        JPanel herramientas = new JPanel();
        herramientas.add(botonVolver);
        herramientas.add(botonNuevo);
        herramientas.add(botonEditar);
        herramientas.add(botonEliminar);
        herramientas.add(botonActualizar);
        herramientas.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(new JLabel("  " + modulo.titulo()), BorderLayout.NORTH);
        arriba.add(herramientas, BorderLayout.CENTER);

        JPanel abajo = new JPanel(new BorderLayout());
        abajo.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        abajo.add(estado, BorderLayout.WEST);
        add(arriba, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent evento) {
                principal.setVisible(true);
                principal.toFront();
            }
        });
        cargar();
    }

    private void cargar() {
        // Consulta los datos fuera del hilo grafico y luego actualiza la tabla.
        estado.setText("Cargando...");
        botonNuevo.setEnabled(false);
        botonEditar.setEnabled(false);
        botonEliminar.setEnabled(false);
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                return GestionDAO.listar(modulo);
            }
            @Override protected void done() {
                botonNuevo.setEnabled(botonNuevo.isVisible());
                botonEditar.setEnabled(botonEditar.isVisible());
                botonEliminar.setEnabled(botonEliminar.isVisible());
                try {
                    List<Object[]> filas = get();
                    modelo.setRowCount(0);
                    for (Object[] fila : filas) modelo.addRow(fila);
                    estado.setText(filas.size() + " registros");
                } catch (Exception excepcion) {
                    mostrarError("No se pudo cargar " + modulo.titulo().toLowerCase(), excepcion);
                }
            }
        }.execute();
    }

    private void editarSeleccionado() {
        // Comprueba la fila elegida y prepara sus datos para abrir el formulario.
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un registro de la tabla.",
                    "Editar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if ("usuarios".equals(modulo.tabla()) && !esAdministrador()) {
            int columnaRol = 1 + indiceVisible("rol");
            if (columnaRol > 0 && "ADMINISTRADOR".equals(modelo.getValueAt(fila, columnaRol))) {
                JOptionPane.showMessageDialog(this, "Solo un administrador puede modificar esa cuenta.",
                        "Acción no permitida", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        int identificadorSeleccionado = (int) modelo.getValueAt(fila, 0);
        if (esModuloActividades()) {
            try {
                Map<String, Object> valores = GestionDAO.buscarPorId(modulo, identificadorSeleccionado);
                if (valores.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "La actividad ya no existe. Actualizá la lista.",
                            "Editar actividad", JOptionPane.INFORMATION_MESSAGE);
                    cargar();
                    return;
                }
                mostrarFormulario(new Registro(identificadorSeleccionado, valores));
            } catch (Exception excepcion) {
                mostrarError("No se pudo abrir la actividad", excepcion);
            }
            return;
        }
        Map<String, Object> valores = new LinkedHashMap<>();
        for (int indice = 0; indice < visibles.size(); indice++) {
            valores.put(visibles.get(indice).columna(), modelo.getValueAt(fila, indice + 1));
        }
        mostrarFormulario(new Registro((int) modelo.getValueAt(fila, 0), valores));
    }

    private void mostrarFormulario(Registro original) {
        // Crea un formulario dinamico a partir de los campos definidos en el modulo.
        if ("usuarios".equals(modulo.tabla()) && !esAdministrador()) {
            JOptionPane.showMessageDialog(this, "Solo un administrador puede gestionar usuarios.",
                    "Acción no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (esLimpieza() && (!"alojamientos".equals(modulo.tabla()) || original == null)) return;
        if (original == null && !"inscripcionActividad".equals(modulo.tabla())
                && !("usuarios".equals(modulo.tabla()) && esAdministrador())) {
            return;
        }
        JDialog dialogo = new JDialog(this, "", true);
        dialogo.setTitle((original == null ? "Nuevo: " : "Editar: ") + modulo.titulo());
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        formulario.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        Map<Campo, java.awt.Component> componentes = new LinkedHashMap<>();
        List<Campo> camposFormulario = modulo.campos();
        if (esLimpieza() && "alojamientos".equals(modulo.tabla())) {
            camposFormulario = camposFormulario.stream().filter(campo -> "estado".equals(campo.columna())).toList();
        }

        try {
            for (Campo campo : camposFormulario) {
                JLabel etiqueta = new JLabel(campo.etiqueta() + (campo.obligatorio() ? " *" : ""));
                boolean estadoBloqueado = "alojamientos".equals(modulo.tabla())
                        && "estado".equals(campo.columna()) && !esLimpieza();
                etiqueta.setEnabled(!estadoBloqueado);
                formulario.add(etiqueta);
                java.awt.Component componente = crearEditor(campo);
                componente.setEnabled(!estadoBloqueado);
                componentes.put(campo, componente);
                formulario.add(componente);
                Object valor = original == null ? null : original.valores().get(campo.columna());
                configurarEditor(campo, componente, valor, original == null);
            }
        } catch (Exception excepcion) {
            mostrarError("No se pudieron cargar las opciones del formulario", excepcion);
            return;
        }

        JButton guardar = new JButton("Guardar");
        JButton cancelar = new JButton("Cancelar");
        JPanel botones = new JPanel();
        botones.add(guardar);
        botones.add(cancelar);
        formulario.add(new JLabel("* Campo obligatorio"));
        formulario.add(botones);
        dialogo.setContentPane(new JScrollPane(formulario));
        dialogo.pack();
        dialogo.setMinimumSize(new java.awt.Dimension(540, Math.min(650, dialogo.getHeight())));
        dialogo.setLocationRelativeTo(this);
        cancelar.addActionListener(evento -> dialogo.dispose());
        guardar.addActionListener(evento -> {
            Map<String, String> datos = new LinkedHashMap<>();
            try {
                for (Map.Entry<Campo, java.awt.Component> entrada : componentes.entrySet()) {
                    datos.put(entrada.getKey().columna(), leerEditor(entrada.getKey(), entrada.getValue()));
                }
                validarFormulario(datos, original != null);
            } catch (RuntimeException excepcion) {
                JOptionPane.showMessageDialog(dialogo, excepcion.getMessage(), "Revisá los datos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            guardar.setEnabled(false);
            cancelar.setEnabled(false);
            estado.setText("Guardando...");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    GestionDAO.guardar(modulo, original == null ? null : original.id(), datos);
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        dialogo.dispose();
                        cargar();
                    } catch (InterruptedException excepcion) {
                        Thread.currentThread().interrupt();
                        guardar.setEnabled(true);
                        cancelar.setEnabled(true);
                        JOptionPane.showMessageDialog(dialogo, "Se interrumpió el guardado.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (ExecutionException excepcion) {
                        guardar.setEnabled(true);
                        cancelar.setEnabled(true);
                        mostrarError("No se pudo guardar el registro", excepcion);
                    }
                }
            }.execute();
        });
        dialogo.getRootPane().setDefaultButton(guardar);
        dialogo.setVisible(true);
    }

    private java.awt.Component crearEditor(Campo campo) throws Exception {
        // Elige un control segun el tipo de campo: texto, clave, lista u opcion.
        if (campo.tipo() == Tipo.CLAVE) return new JPasswordField(20);
        if (esTextoLargo(campo)) {
            javax.swing.JTextArea areaTexto = new javax.swing.JTextArea(4, 20);
            areaTexto.setLineWrap(true);
            areaTexto.setWrapStyleWord(true);
            return new JScrollPane(areaTexto);
        }
        if (campo.tipo() == Tipo.OPCION) {
                JComboBox<Object> selector = new JComboBox<>();
                if (campo.consultaOpciones() != null) {
                    if (!campo.obligatorio()) selector.addItem(new Opcion(0, "— Sin asignar —"));
                    for (Opcion opcion : GestionDAO.opciones(campo.consultaOpciones())) selector.addItem(opcion);
                } else {
                    if (!campo.obligatorio() && esEnumActividad(campo.columna())) {
                        selector.addItem(new OpcionEnum("", "— Sin especificar —"));
                    }
                    for (String opcion : campo.opciones()) {
                        if ("rol".equals(campo.columna()) && "ADMINISTRADOR".equals(opcion) && !esAdministrador()) continue;
                        selector.addItem(esEnumActividad(campo.columna())
                                ? new OpcionEnum(opcion, etiquetaEnumActividad(campo.columna(), opcion)) : opcion);
                    }
                }
            return selector;
        }
        if (campo.tipo() == Tipo.BOOLEANO) {
            JComboBox<Boolean> selector = new JComboBox<>(new Boolean[]{false, true});
            selector.setRenderer((listaDesplegable, valor, indice, seleccionado, foco) ->
                    new JLabel(Boolean.TRUE.equals(valor) ? "Sí" : "No"));
            return selector;
        }
        return new JTextField(20);
    }

    private void configurarEditor(Campo campo, java.awt.Component componente, Object valor, boolean nuevo) {
        // Rellena cada control con el valor guardado o con un valor inicial util.
        if (componente instanceof JPasswordField clave) {
            clave.setToolTipText("Mínimo 8 caracteres. Al editar, dejar vacío conserva la clave.");
        } else if (componente instanceof JScrollPane desplazamiento && esTextoLargo(campo)) {
            javax.swing.JTextArea areaTexto = (javax.swing.JTextArea) desplazamiento.getViewport().getView();
            if (valor != null) areaTexto.setText(valor.toString());
        } else if (componente instanceof JComboBox<?> selector) {
            if (campo.tipo() == Tipo.BOOLEANO) {
                boolean marcado = valor instanceof Boolean valorBooleano
                        ? valorBooleano : valor != null && ((Number) valor).intValue() != 0;
                selector.setSelectedItem(marcado);
            } else if (campo.consultaOpciones() != null && valor != null) {
                for (int indice = 0; indice < selector.getItemCount(); indice++) {
                    Object elemento = selector.getItemAt(indice);
                    if (elemento instanceof Opcion opcion && opcion.id() == ((Number) valor).intValue()) {
                        selector.setSelectedIndex(indice);
                        break;
                    }
                }
            } else if (esEnumActividad(campo.columna())) {
                String codigo = valor == null ? "" : valor.toString();
                for (int indice = 0; indice < selector.getItemCount(); indice++) {
                    Object elemento = selector.getItemAt(indice);
                    if (elemento instanceof OpcionEnum opcion && opcion.valor().equals(codigo)) {
                        selector.setSelectedIndex(indice);
                        break;
                    }
                }
            } else if (valor != null) {
                selector.setSelectedItem(valor.toString());
            } else if (!campo.obligatorio() && selector.getItemCount() > 0
                    && selector.getItemAt(0) instanceof Opcion opcion && opcion.id() == 0) {
                selector.setSelectedIndex(0);
            }
        } else if (componente instanceof JTextField campoTexto) {
            if (valor != null) campoTexto.setText(valor.toString());
            else if (nuevo && campo.tipo() == Tipo.FECHA) campoTexto.setText(LocalDate.now().toString());
            else if (nuevo && campo.tipo() == Tipo.HORA) campoTexto.setText("09:00");
            else if (nuevo && "precioTotal".equals(campo.columna())) campoTexto.setText("0");
        }
    }

    private String leerEditor(Campo campo, java.awt.Component componente) {
        // Convierte el contenido de cualquier control a texto para validarlo y guardarlo.
        if (componente instanceof JPasswordField clave) return new String(clave.getPassword());
        if (componente instanceof JScrollPane desplazamiento && esTextoLargo(campo)) {
            return ((javax.swing.JTextArea) desplazamiento.getViewport().getView()).getText().trim();
        }
        if (componente instanceof JComboBox<?> selector) {
            Object valor = selector.getSelectedItem();
            if (valor == null) return "";
            if (valor instanceof Opcion opcion) {
                if (opcion.id() == 0 && !campo.obligatorio()) return "";
                return String.valueOf(opcion.id());
            }
            if (valor instanceof OpcionEnum opcion) return opcion.valor();
            return valor.toString();
        }
        return ((JTextField) componente).getText().trim();
    }

    private void validarFormulario(Map<String, String> datos, boolean edicion) {
        // Revisa campos obligatorios y formatos antes de llamar al DAO.
        for (Campo campo : modulo.campos()) {
            if (!datos.containsKey(campo.columna())) continue;
            String valor = datos.getOrDefault(campo.columna(), "").trim();
            if (campo.obligatorio() && valor.isEmpty()) throw new IllegalArgumentException("Completá " + campo.etiqueta() + ".");
            if (valor.isEmpty()) continue;
            switch (campo.tipo()) {
                case ENTERO -> {
                    int numero = Integer.parseInt(valor);
                    if (numero < 0) throw new IllegalArgumentException(campo.etiqueta() + " no puede ser negativo.");
                    if ("cupoMax".equals(campo.columna()) && numero < 1)
                        throw new IllegalArgumentException("El cupo máximo debe ser al menos 1.");
                }
                case DECIMAL -> {
                    if (new BigDecimal(valor.replace(',', '.')).signum() < 0) throw new IllegalArgumentException(campo.etiqueta() + " no puede ser negativo.");
                }
                case FECHA -> LocalDate.parse(valor);
                case HORA -> LocalTime.parse(valor.length() == 5 ? valor + ":00" : valor);
                case CLAVE -> {
                    if ((!edicion || !valor.isEmpty()) && valor.length() < 8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
                }
                default -> { }
            }
        }
    }

    private void eliminarSeleccionado() {
        // Una reserva cambia a estado cancelada; las demas filas siguen la regla de borrado.
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un registro de la tabla.", "Eliminar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (int) modelo.getValueAt(fila, 0);
        if ("usuarios".equals(modulo.tabla()) && SesionActual.get() != null
                && id == SesionActual.get().getIdUsuario()) {
            JOptionPane.showMessageDialog(this, "No podés eliminar la cuenta que está usando la aplicación.",
                    "Acción no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if ("usuarios".equals(modulo.tabla()) && !esAdministrador()) {
            int columnaRol = 1 + indiceVisible("rol");
            if (columnaRol > 0 && "ADMINISTRADOR".equals(modelo.getValueAt(fila, columnaRol))) {
                JOptionPane.showMessageDialog(this, "Solo un administrador puede eliminar esa cuenta.",
                        "Acción no permitida", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        boolean esReserva = "reservas".equals(modulo.tabla());
        String pregunta = esReserva
                ? "¿Cancelar la reserva seleccionada? Se conservará el registro y sus pagos."
                : "¿Eliminar el registro seleccionado?";
        if (JOptionPane.showConfirmDialog(this, pregunta, "Confirmar acción",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (esReserva) GestionDAO.cancelarReserva(id);
                else GestionDAO.eliminar(modulo, id);
                return null;
            }
            @Override protected void done() {
                try { get(); cargar(); }
                catch (Exception excepcion) { mostrarError("No se pudo eliminar. Puede tener datos relacionados", excepcion); }
            }
        }.execute();
    }

    private void mostrarError(String mensaje, Exception excepcion) {
        // Busca la causa mas especifica para mostrar un error entendible.
        Throwable causa = excepcion;
        while (causa.getCause() != null) causa = causa.getCause();
        String detalle = causa.getMessage() == null ? "" : ":\n" + causa.getMessage();
        estado.setText("Ocurrió un error");
        JOptionPane.showMessageDialog(this, mensaje + detalle, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private int indiceVisible(String columna) {
        // Busca la posicion de una columna en la tabla que muestra solo campos visibles.
        for (int indice = 0; indice < visibles.size(); indice++) {
            if (columna.equals(visibles.get(indice).columna())) return indice;
        }
        return -1;
    }

    private boolean esAdministrador() {
        // Centraliza la comprobacion del rol de administrador.
        return SesionActual.get() != null && "ADMINISTRADOR".equals(SesionActual.get().getRol());
    }

    private boolean esLimpieza() {
        // Centraliza la comprobacion del rol de limpieza.
        return SesionActual.get() != null && "LIMPIEZA".equals(SesionActual.get().getRol());
    }

    private boolean esModuloActividades() {
        return "Actividad".equals(modulo.tabla());
    }

    private boolean esEnumActividad(String columna) {
        return esModuloActividades() && ("momentoDia".equals(columna)
                || "edadRecomendada".equals(columna) || "modalidad".equals(columna));
    }

    private boolean esTextoLargo(Campo campo) {
        return esModuloActividades() && List.of("descripcion", "requisitos", "informacionImportante")
                .contains(campo.columna());
    }

    private String etiquetaEnumActividad(String columna, String valor) {
        // Muestra etiquetas en espanol sin cambiar los valores guardados en la base.
        if ("momentoDia".equals(columna)) {
            return switch (valor) {
                case "manana" -> "Mañana";
                case "tarde" -> "Tarde";
                case "noche" -> "Noche";
                default -> valor;
            };
        }
        if ("edadRecomendada".equals(columna)) {
            return switch (valor) {
                case "todas" -> "Todas las edades";
                case "ninos" -> "Niños";
                case "adolescentes" -> "Adolescentes";
                case "adultos" -> "Adultos";
                default -> valor;
            };
        }
        if ("modalidad".equals(columna)) {
            return switch (valor) {
                case "incluida" -> "Incluida";
                case "inscripcion" -> "Con inscripción";
                default -> valor;
            };
        }
        return valor;
    }

    private record Registro(int id, Map<String, Object> valores) { }
}
