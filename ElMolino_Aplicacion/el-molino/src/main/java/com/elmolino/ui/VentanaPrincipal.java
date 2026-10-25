package com.elmolino.ui;

import com.elmolino.dao.GestionDAO;
import com.elmolino.model.Usuario;
import com.elmolino.util.SesionActual;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Menu principal que muestra solo las secciones permitidas al rol activo. */
public class VentanaPrincipal extends JFrame {

    // Colores usados en el fondo, el texto y los acentos del menu.
    private static final Color PETROLEO = new Color(0x0F4C5C);
    private static final Color VERDE = new Color(0x2D4A3E);
    private static final Color FONDO = new Color(0xF4FAF8);
    private static final Color BORDE = new Color(0xCFE4DC);
    private static final Color TEXTO_SUAVE = new Color(0x52645E);
    private static final Color AZUL = new Color(0x1B6B7A);
    private static final Color VERDE_MEDIO = new Color(0x4F7358);
    private static final Color NARANJA = new Color(0xC97B3D);
    private final float escala;

    public VentanaPrincipal() {
        super("El Molino | Gestión");
        escala = calcularEscala();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 720);
        setMinimumSize(new Dimension(700, 560));
        setLocationRelativeTo(null);

        // Arma las tarjetas a partir de la cuenta conectada y abre la ventana maximizada.
        Usuario usuario = SesionActual.get();
        List<Acceso> accesos = armarAccesos(usuario.getRol());
        getContentPane().setBackground(FONDO);
        setLayout(new BorderLayout());

        add(crearEncabezado(usuario), BorderLayout.NORTH);
        add(crearContenido(accesos), BorderLayout.CENTER);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private float calcularEscala() {
        // Limita el escalado para que el contenido siga siendo legible en distintas pantallas.
        Dimension pantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        float escalaHorizontal = pantalla.width / 1280f;
        float escalaVertical = pantalla.height / 800f;
        return Math.max(0.95f, Math.min(1.45f, Math.min(escalaHorizontal, escalaVertical)));
    }

    private int escalar(int medida) {
        return Math.round(medida * escala);
    }

    private JPanel crearEncabezado(Usuario usuario) {
        // Muestra la marca, el saludo de empleado y la accion para cerrar sesion.
        JPanel encabezado = new JPanel(new BorderLayout(18, 0));
        encabezado.setBackground(FONDO);
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xB3CBA6)),
                BorderFactory.createEmptyBorder(escalar(15), escalar(32), escalar(15), escalar(30))));

        JPanel textos = new JPanel(new GridLayout(0, 1, 0, escalar(3)));
        textos.setOpaque(false);
        JLabel marca = new JLabel("EL MOLINO");
        marca.setForeground(VERDE);
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, 21f * escala));
        JLabel saludo = new JLabel("Hola, " + usuario.getNombre() + " · " + nombreRol(usuario.getRol()));
        saludo.setForeground(TEXTO_SUAVE);
        saludo.setFont(saludo.getFont().deriveFont(13f * escala));
        textos.add(marca);
        textos.add(saludo);

        JButton cerrarSesion = new JButton("Cerrar sesión");
        cerrarSesion.setToolTipText("Cerrar la sesión actual y volver al inicio de sesión");
        cerrarSesion.getAccessibleContext().setAccessibleName("Cerrar sesión");
        cerrarSesion.getAccessibleContext().setAccessibleDescription("Cierra la sesión actual");
        cerrarSesion.setForeground(Color.WHITE);
        cerrarSesion.setBackground(VERDE_MEDIO);
        cerrarSesion.setFont(cerrarSesion.getFont().deriveFont(Font.BOLD, 13f * escala));
        cerrarSesion.setFocusPainted(true);
        cerrarSesion.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x7FA87A), 1),
                BorderFactory.createEmptyBorder(escalar(9), escalar(15), escalar(9), escalar(15))));
        cerrarSesion.addActionListener(evento -> {
            SesionActual.cerrar();
            dispose();
            SwingUtilities.invokeLater(() -> new VentanaLogin().setVisible(true));
        });
        cerrarSesion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        encabezado.add(textos, BorderLayout.WEST);
        encabezado.add(cerrarSesion, BorderLayout.EAST);
        return encabezado;
    }

    private JScrollPane crearContenido(List<Acceso> accesos) {
        // Presenta el titulo y acomoda las secciones en una grilla que cambia con el ancho.
        JPanel contenido = new JPanel(new BorderLayout(0, escalar(24)));
        contenido.setBackground(FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(escalar(28), escalar(38), escalar(36), escalar(38)));

        JPanel introduccion = new JPanel(new GridLayout(0, 1, 0, 7));
        introduccion.setBackground(new Color(0xE5F2EC));
        introduccion.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, new Color(0x7FA87A)),
                BorderFactory.createEmptyBorder(escalar(18), escalar(22), escalar(18), escalar(22))));
        JLabel titulo = new JLabel("Panel de gestión");
        titulo.setForeground(PETROLEO);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 24f * escala));
        JLabel ayuda = new JLabel("Seleccioná una sección para comenzar.");
        ayuda.setForeground(TEXTO_SUAVE);
        ayuda.setFont(ayuda.getFont().deriveFont(15f * escala));
        introduccion.add(titulo);
        introduccion.add(ayuda);
        contenido.add(introduccion, BorderLayout.NORTH);

        if (accesos.isEmpty()) {
            JLabel vacio = new JLabel("No hay secciones disponibles para este perfil.", SwingConstants.CENTER);
            vacio.setForeground(TEXTO_SUAVE);
            vacio.setFont(vacio.getFont().deriveFont(17f * escala));
            contenido.add(vacio, BorderLayout.CENTER);
        } else {
            int anchoPantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
            int maxColumnas = anchoPantalla >= 1600 ? 3 : anchoPantalla < 900 ? 1 : 2;
            int columnas = Math.min(maxColumnas, accesos.size());
            JPanel tarjetas = new JPanel(new GridBagLayout());
            tarjetas.setOpaque(false);
            for (int indice = 0; indice < accesos.size(); indice++) {
                int fila = indice / columnas;
                int columna = indice % columnas;
                int restantes = accesos.size() - fila * columnas;
                int desplazamiento = restantes < columnas ? (columnas - restantes) / 2 : 0;
                GridBagConstraints restricciones = new GridBagConstraints();
                restricciones.gridx = columna + desplazamiento;
                restricciones.gridy = fila;
                restricciones.weightx = 1;
                restricciones.fill = GridBagConstraints.HORIZONTAL;
                restricciones.anchor = GridBagConstraints.CENTER;
                restricciones.insets = new Insets(escalar(8), escalar(8), escalar(8), escalar(8));
                tarjetas.add(crearTarjeta(accesos.get(indice), indice), restricciones);
            }
            contenido.add(tarjetas, BorderLayout.CENTER);
        }

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(escalar(20));
        return scroll;
    }

    private JButton crearTarjeta(Acceso acceso, int indice) {
        // Cada seccion recibe un tono suave y un acento de la paleta natural.
        Color[] fondos = {new Color(0xF1F7F2), new Color(0xEAF5F3), new Color(0xFAF8F3), new Color(0xEDF5E9)};
        Color[] acentos = {VERDE_MEDIO, AZUL, NARANJA, new Color(0x7FA87A)};
        Color fondoTarjeta = fondos[indice % fondos.length];
        Color acentoTarjeta = acentos[indice % acentos.length];
        JButton boton = new TarjetaAcceso("<html><div style='text-align:left'>"
                + "<b style='font-size:" + escalar(21) + "pt;color:#2D4A3E'>" + acceso.titulo() + "</b><br>"
                + "<span style='font-size:" + escalar(14) + "pt;color:#52645E'>" + acceso.descripcion() + "</span><br><br>"
                + "<span style='font-size:" + escalar(11) + "pt;color:#1B6B7A'><b>ABRIR SECCIÓN&nbsp; &#8594;</b></span>"
                + "</div></html>", fondoTarjeta, acentoTarjeta);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setVerticalAlignment(SwingConstants.CENTER);
        boton.setForeground(PETROLEO);
        boton.setFont(boton.getFont().deriveFont(15f * escala));
        boton.setPreferredSize(new Dimension(escalar(400), escalar(144)));
        boton.setFocusPainted(false);
        boton.setToolTipText(acceso.descripcion());
        boton.getAccessibleContext().setAccessibleName(acceso.titulo());
        boton.getAccessibleContext().setAccessibleDescription(acceso.descripcion());
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addActionListener(evento -> acceso.accion().run());
        return boton;
    }

    private List<Acceso> armarAccesos(String rol) {
        // Define las secciones visibles segun el rol que inicio sesion.
        List<Acceso> accesos = new ArrayList<>();
        switch (rol) {
            case "SUPERADMIN" -> {
                accesos.add(modulo("Clientes", "Consultar y editar datos de clientes", null));
                accesos.add(modulo("Alojamientos", "Gestionar alojamientos y estados", GestionDAO.ALOJAMIENTOS));
                accesos.add(modulo("Reservas", "Gestionar reservas", GestionDAO.RESERVAS));
                accesos.add(modulo("Pagos", "Consultar pagos registrados", GestionDAO.PAGOS));
                accesos.add(modulo("Usuarios", "Crear usuarios y asignar cualquier rol", GestionDAO.USUARIOS));
                accesos.add(modulo("Acompañantes", "Gestionar acompañantes", GestionDAO.ACOMPANANTES));
                accesos.add(new Acceso("Acompañantes por reserva", "Gestionar acompañantes asociados", () -> abrirAcompanantes()));
                accesos.add(modulo("Espacios recreativos", "Gestionar espacios recreativos", GestionDAO.ESPACIOS));
                accesos.add(modulo("Actividades", "Gestionar actividades", GestionDAO.ACTIVIDADES));
                accesos.add(modulo("Inscripciones", "Gestionar inscripciones", GestionDAO.ACTIVIDAD_INSCRIPTOS));
                accesos.add(new Acceso("Informes", "Ver resumenes de reservas, alojamientos, actividades y pagos", () -> abrirInformes()));
                accesos.add(new Acceso("Registro de auditoría", "Consultar quién realizó cambios en el sistema", () -> abrirAuditoria()));
            }
            case "RECEPCION" -> {
                accesos.add(modulo("Clientes", "Consultar y editar datos de clientes", null));
                accesos.add(modulo("Alojamientos", "Consultar y actualizar alojamientos", GestionDAO.ALOJAMIENTOS));
                accesos.add(modulo("Pagos", "Consultar pagos registrados", GestionDAO.PAGOS));
                accesos.add(modulo("Espacios recreativos", "Consultar y actualizar espacios", GestionDAO.ESPACIOS));
            }
            case "ADMINISTRACION" -> {
                accesos.add(modulo("Clientes", "Consultar y editar datos de clientes", null));
                accesos.add(modulo("Acompañantes", "Gestionar los datos de acompañantes", GestionDAO.ACOMPANANTES));
                accesos.add(new Acceso("Acompañantes por reserva", "Editar o quitar acompañantes de una reserva", () -> abrirAcompanantes()));
                accesos.add(modulo("Alojamientos", "Consultar y actualizar alojamientos", GestionDAO.ALOJAMIENTOS));
                accesos.add(modulo("Reservas", "Consultar y actualizar reservas", GestionDAO.RESERVAS));
                accesos.add(modulo("Pagos", "Consultar pagos registrados", GestionDAO.PAGOS));
                accesos.add(new Acceso("Registro de auditoría", "Consultar quién realizó cambios en el sistema", () -> abrirAuditoria()));
            }
            case "COORDINADOR" -> {
                accesos.add(modulo("Espacios recreativos", "Consultar y actualizar espacios", GestionDAO.ESPACIOS));
                accesos.add(modulo("Actividades", "Consultar y actualizar actividades", GestionDAO.ACTIVIDADES));
                accesos.add(modulo("Inscripciones", "Agregar y consultar inscripciones", GestionDAO.ACTIVIDAD_INSCRIPTOS));
            }
            case "GERENCIA" -> {
                accesos.add(modulo("Clientes", "Consultar y editar datos de clientes", null));
                accesos.add(modulo("Reservas", "Consultar y actualizar reservas", GestionDAO.RESERVAS));
                accesos.add(modulo("Pagos", "Consultar pagos registrados", GestionDAO.PAGOS));
                accesos.add(modulo("Alojamientos", "Consultar y actualizar alojamientos", GestionDAO.ALOJAMIENTOS));
                accesos.add(new Acceso("Registro de auditoría", "Consultar quién realizó cambios en el sistema", () -> abrirAuditoria()));
            }
            case "LIMPIEZA" -> accesos.add(modulo(
                    "Habitaciones", "Consultar habitaciones y actualizar su estado", GestionDAO.ALOJAMIENTOS));
            case "ADMINISTRADOR" -> {
                accesos.add(modulo("Clientes", "Consultar y editar datos de clientes", null));
                accesos.add(modulo("Reservas", "Consultar y actualizar reservas", GestionDAO.RESERVAS));
                accesos.add(modulo("Pagos", "Consultar pagos registrados", GestionDAO.PAGOS));
                accesos.add(modulo("Usuarios", "Crear cuentas, asignar roles y gestionar usuarios", GestionDAO.USUARIOS));
                accesos.add(new Acceso("Registro de auditoría", "Consultar quién realizó cambios en el sistema", () -> abrirAuditoria()));
            }
            default -> { }
        }
        return accesos;
    }

    private Acceso modulo(String titulo, String descripcion, GestionDAO.Modulo modulo) {
        // Oculta el menu y abre la pantalla indicada cuando se pulsa una tarjeta.
        return new Acceso(titulo, descripcion, () -> {
            setVisible(false);
            if (modulo == null) new VentanaClientes(this).setVisible(true);
            else new VentanaGestion(this, modulo).setVisible(true);
        });
    }

    private void abrirAcompanantes() {
        // La relacion de acompanantes usa una pantalla propia.
        setVisible(false);
        new VentanaAcompanantesReserva(this).setVisible(true);
    }

    private void abrirInformes() {
        // Abre los informes desde el menu general.
        setVisible(false);
        new VentanaInformes(this).setVisible(true);
    }

    private void abrirAuditoria() {
        // Solo estos roles ven esta seccion; el DAO valida el permiso al consultar.
        setVisible(false);
        new VentanaAuditoria(this).setVisible(true);
    }

    private String nombreRol(String rol) {
        return switch (rol) {
            case "SUPERADMIN" -> "SuperAdmin";
            case "ADMINISTRADOR" -> "Administrador";
            case "RECEPCION" -> "Recepción";
            case "ADMINISTRACION" -> "Administración";
            case "COORDINADOR" -> "Coordinación";
            case "GERENCIA" -> "Gerencia";
            case "LIMPIEZA" -> "Limpieza";
            default -> rol;
        };
    }

    private record Acceso(String titulo, String descripcion, Runnable accion) { }

    // Dibuja las tarjetas con esquinas suaves, borde de foco y una franja de color.
    private final class TarjetaAcceso extends JButton {
        private final Color fondoTarjeta;
        private final Color acentoTarjeta;

        private TarjetaAcceso(String texto, Color fondoTarjeta, Color acentoTarjeta) {
            super(texto);
            this.fondoTarjeta = fondoTarjeta;
            this.acentoTarjeta = acentoTarjeta;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(escalar(18), escalar(24), escalar(18), escalar(20)));
            setMaximumSize(new Dimension(escalar(560), escalar(190)));
        }

        @Override
        protected void paintComponent(Graphics graficos) {
            // Pinta el fondo y el acento antes de que Swing dibuje el texto del boton.
            Graphics2D lienzo = (Graphics2D) graficos.create();
            lienzo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean resaltada = getModel().isRollover() || getModel().isPressed() || hasFocus();
            lienzo.setColor(fondoTarjeta);
            lienzo.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, escalar(18), escalar(18));
            lienzo.setColor(resaltada ? acentoTarjeta : BORDE);
            lienzo.setStroke(new java.awt.BasicStroke(resaltada ? 2f : 1f));
            lienzo.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, escalar(18), escalar(18));
            lienzo.setColor(acentoTarjeta);
            lienzo.fillRoundRect(0, escalar(13), escalar(6), getHeight() - escalar(26), escalar(6), escalar(6));
            lienzo.dispose();
            super.paintComponent(graficos);
        }
    }
}
