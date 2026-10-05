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

/** Ventana reutilizable de alta, consulta, edición y baja de un módulo. */
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
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnEliminar = new JButton("Eliminar");

    public VentanaGestion(VentanaPrincipal principal, Modulo modulo) {
        super("El Molino - " + modulo.titulo());
        this.principal = principal;
        this.modulo = modulo;
        boolean pagosSoloLectura = "pago".equals(modulo.tabla());
        boolean soloAltaInscripcion = "inscripcionActividad".equals(modulo.tabla());
        boolean altaUsuarioAdmin = "usuarios".equals(modulo.tabla()) && esAdministrador();
        // Las altas se realizan desde la web. La app solo permite crear inscripciones
        // (para evitar inconsistencias) y usuarios administradores.
        btnNuevo.setVisible(soloAltaInscripcion || altaUsuarioAdmin);
        if (pagosSoloLectura) {
            btnNuevo.setVisible(false);
            btnEditar.setVisible(false);
            btnEliminar.setVisible(false);
        }
        if (soloAltaInscripcion) {
            btnEditar.setVisible(false);
            btnEliminar.setVisible(false);
        }
        if ("usuarios".equals(modulo.tabla()) && !esAdministrador()) {
            btnNuevo.setVisible(false);
            btnEditar.setVisible(false);
            btnEliminar.setVisible(false);
        }
        if ("reservas".equals(modulo.tabla())) btnEliminar.setText("Cancelar reserva");
        this.visibles = GestionDAO.camposVisibles(modulo);
        String[] columnas;
        if (esModuloActividades()) {
            columnas = new String[]{"ID", "Actividad", "Descripción breve", "Horario", "Días", "Momento",
                    "Duración", "Sector", "Responsable", "Edad", "Modalidad", "Activa", "Espacio",
                    "Inscriptos / cupo", "Disponibles", "Estado del cupo", "Precio", "Pago"};
        } else {
            columnas = new String[visibles.size() + 1];
            columnas[0] = "ID";
            for (int i = 0; i < visibles.size(); i++) columnas[i + 1] = visibles.get(i).etiqueta();
        }
        this.modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        this.tabla = new JTable(modelo);
        if (esModuloActividades()) {
            tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            int[] anchos = {55, 180, 200, 105, 115, 95, 95, 110, 135, 120, 110, 65, 150, 110, 90, 125, 95, 105};
            for (int i = 0; i < anchos.length; i++) tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1050, 580);
        setMinimumSize(new java.awt.Dimension(760, 420));
        setLocationRelativeTo(principal);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton btnVolver = new JButton("← Volver al menú");
        JButton btnActualizar = new JButton("Actualizar");
        btnVolver.addActionListener(e -> dispose());
        btnActualizar.addActionListener(e -> cargar());
        btnNuevo.addActionListener(e -> mostrarFormulario(null));
        btnEditar.addActionListener(e -> editarSeleccionado());
        btnEliminar.addActionListener(e -> eliminarSeleccionado());

        JPanel herramientas = new JPanel();
        herramientas.add(btnVolver);
        herramientas.add(btnNuevo);
        herramientas.add(btnEditar);
        herramientas.add(btnEliminar);
        herramientas.add(btnActualizar);
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
            @Override public void windowClosed(WindowEvent e) {
                principal.setVisible(true);
                principal.toFront();
            }
        });
        cargar();
    }

    private void cargar() {
        estado.setText("Cargando...");
        btnNuevo.setEnabled(false);
        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception {
                return GestionDAO.listar(modulo);
            }
            @Override protected void done() {
                btnNuevo.setEnabled(btnNuevo.isVisible());
                btnEditar.setEnabled(btnEditar.isVisible());
                btnEliminar.setEnabled(btnEliminar.isVisible());
                try {
                    List<Object[]> filas = get();
                    modelo.setRowCount(0);
                    for (Object[] fila : filas) modelo.addRow(fila);
                    estado.setText(filas.size() + " registros");
                } catch (Exception ex) {
                    mostrarError("No se pudo cargar " + modulo.titulo().toLowerCase(), ex);
                }
            }
        }.execute();
    }

    private void editarSeleccionado() {
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
        int idSeleccionado = (int) modelo.getValueAt(fila, 0);
        if (esModuloActividades()) {
            try {
                Map<String, Object> valores = GestionDAO.buscarPorId(modulo, idSeleccionado);
                if (valores.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "La actividad ya no existe. Actualizá la lista.",
                            "Editar actividad", JOptionPane.INFORMATION_MESSAGE);
                    cargar();
                    return;
                }
                mostrarFormulario(new Registro(idSeleccionado, valores));
            } catch (Exception ex) {
                mostrarError("No se pudo abrir la actividad", ex);
            }
            return;
        }
        Map<String, Object> valores = new LinkedHashMap<>();
        for (int i = 0; i < visibles.size(); i++) {
            valores.put(visibles.get(i).columna(), modelo.getValueAt(fila, i + 1));
        }
        mostrarFormulario(new Registro((int) modelo.getValueAt(fila, 0), valores));
    }

    private void mostrarFormulario(Registro original) {
        if ("usuarios".equals(modulo.tabla()) && !esAdministrador()) {
            JOptionPane.showMessageDialog(this, "Solo un administrador puede gestionar usuarios.",
                    "Acción no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }
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

        try {
            for (Campo campo : modulo.campos()) {
                formulario.add(new JLabel(campo.etiqueta() + (campo.obligatorio() ? " *" : "")));
                java.awt.Component componente = crearEditor(campo);
                componentes.put(campo, componente);
                formulario.add(componente);
                Object valor = original == null ? null : original.valores().get(campo.columna());
                configurarEditor(campo, componente, valor, original == null);
            }
        } catch (Exception ex) {
            mostrarError("No se pudieron cargar las opciones del formulario", ex);
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
        cancelar.addActionListener(e -> dialogo.dispose());
        guardar.addActionListener(e -> {
            Map<String, String> datos = new LinkedHashMap<>();
            try {
                for (Map.Entry<Campo, java.awt.Component> entrada : componentes.entrySet()) {
                    datos.put(entrada.getKey().columna(), leerEditor(entrada.getKey(), entrada.getValue()));
                }
                validarFormulario(datos, original != null);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(dialogo, ex.getMessage(), "Revisá los datos", JOptionPane.WARNING_MESSAGE);
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
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        guardar.setEnabled(true);
                        cancelar.setEnabled(true);
                        JOptionPane.showMessageDialog(dialogo, "Se interrumpió el guardado.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (ExecutionException ex) {
                        guardar.setEnabled(true);
                        cancelar.setEnabled(true);
                        mostrarError("No se pudo guardar el registro", ex);
                    }
                }
            }.execute();
        });
        dialogo.getRootPane().setDefaultButton(guardar);
        dialogo.setVisible(true);
    }

    private java.awt.Component crearEditor(Campo campo) throws Exception {
        if (campo.tipo() == Tipo.CLAVE) return new JPasswordField(20);
        if (esTextoLargo(campo)) {
            javax.swing.JTextArea area = new javax.swing.JTextArea(4, 20);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            return new JScrollPane(area);
        }
        if (campo.tipo() == Tipo.OPCION) {
                JComboBox<Object> combo = new JComboBox<>();
                if (campo.consultaOpciones() != null) {
                    if (!campo.obligatorio()) combo.addItem(new Opcion(0, "— Sin asignar —"));
                    for (Opcion opcion : GestionDAO.opciones(campo.consultaOpciones())) combo.addItem(opcion);
                } else {
                    if (!campo.obligatorio() && esEnumActividad(campo.columna())) {
                        combo.addItem(new OpcionEnum("", "— Sin especificar —"));
                    }
                    for (String opcion : campo.opciones()) {
                        if ("rol".equals(campo.columna()) && "ADMINISTRADOR".equals(opcion) && !esAdministrador()) continue;
                        combo.addItem(esEnumActividad(campo.columna())
                                ? new OpcionEnum(opcion, etiquetaEnumActividad(campo.columna(), opcion)) : opcion);
                    }
                }
            return combo;
        }
        if (campo.tipo() == Tipo.BOOLEANO) {
            JComboBox<Boolean> combo = new JComboBox<>(new Boolean[]{false, true});
            combo.setRenderer((list, valor, index, seleccionado, foco) ->
                    new JLabel(Boolean.TRUE.equals(valor) ? "Sí" : "No"));
            return combo;
        }
        return new JTextField(20);
    }

    private void configurarEditor(Campo campo, java.awt.Component componente, Object valor, boolean nuevo) {
        if (componente instanceof JPasswordField clave) {
            clave.setToolTipText("Mínimo 8 caracteres. Al editar, dejar vacío conserva la clave.");
        } else if (componente instanceof JScrollPane desplazamiento && esTextoLargo(campo)) {
            javax.swing.JTextArea area = (javax.swing.JTextArea) desplazamiento.getViewport().getView();
            if (valor != null) area.setText(valor.toString());
        } else if (componente instanceof JComboBox<?> combo) {
            if (campo.tipo() == Tipo.BOOLEANO) {
                boolean marcado = valor instanceof Boolean valorBooleano
                        ? valorBooleano : valor != null && ((Number) valor).intValue() != 0;
                combo.setSelectedItem(marcado);
            } else if (campo.consultaOpciones() != null && valor != null) {
                for (int i = 0; i < combo.getItemCount(); i++) {
                    Object item = combo.getItemAt(i);
                    if (item instanceof Opcion opcion && opcion.id() == ((Number) valor).intValue()) {
                        combo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (esEnumActividad(campo.columna())) {
                String codigo = valor == null ? "" : valor.toString();
                for (int i = 0; i < combo.getItemCount(); i++) {
                    Object item = combo.getItemAt(i);
                    if (item instanceof OpcionEnum opcion && opcion.valor().equals(codigo)) {
                        combo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (valor != null) {
                combo.setSelectedItem(valor.toString());
            } else if (!campo.obligatorio() && combo.getItemCount() > 0
                    && combo.getItemAt(0) instanceof Opcion opcion && opcion.id() == 0) {
                combo.setSelectedIndex(0);
            }
        } else if (componente instanceof JTextField campoTexto) {
            if (valor != null) campoTexto.setText(valor.toString());
            else if (nuevo && campo.tipo() == Tipo.FECHA) campoTexto.setText(LocalDate.now().toString());
            else if (nuevo && campo.tipo() == Tipo.HORA) campoTexto.setText("09:00");
            else if (nuevo && "precioTotal".equals(campo.columna())) campoTexto.setText("0");
        }
    }

    private String leerEditor(Campo campo, java.awt.Component componente) {
        if (componente instanceof JPasswordField clave) return new String(clave.getPassword());
        if (componente instanceof JScrollPane desplazamiento && esTextoLargo(campo)) {
            return ((javax.swing.JTextArea) desplazamiento.getViewport().getView()).getText().trim();
        }
        if (componente instanceof JComboBox<?> combo) {
            Object valor = combo.getSelectedItem();
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
        for (Campo campo : modulo.campos()) {
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
                catch (Exception ex) { mostrarError("No se pudo eliminar. Puede tener datos relacionados", ex); }
            }
        }.execute();
    }

    private void mostrarError(String mensaje, Exception ex) {
        Throwable causa = ex;
        while (causa.getCause() != null) causa = causa.getCause();
        String detalle = causa.getMessage() == null ? "" : ":\n" + causa.getMessage();
        estado.setText("Ocurrió un error");
        JOptionPane.showMessageDialog(this, mensaje + detalle, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private int indiceVisible(String columna) {
        for (int i = 0; i < visibles.size(); i++) {
            if (columna.equals(visibles.get(i).columna())) return i;
        }
        return -1;
    }

    private boolean esAdministrador() {
        return SesionActual.get() != null && "ADMINISTRADOR".equals(SesionActual.get().getRol());
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
