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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

/** Menú principal accesible con accesos acordes al rol activo. */
public class VentanaPrincipal extends JFrame {

    private static final Color PETROLEO = new Color(0x0F4C5C);
    private static final Color VERDE = new Color(0x2D4A3E);
    private static final Color FONDO = new Color(0xFAF8F3);
    private static final Color BORDE = new Color(0xE6E1D3);
    private static final Color TEXTO_SUAVE = new Color(0x52645E);

    public VentanaPrincipal() {
        super("El Molino | Gestión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(940, 720);
        setMinimumSize(new Dimension(700, 560));
        setLocationRelativeTo(null);

        Usuario usuario = SesionActual.get();
        List<Acceso> accesos = armarAccesos(usuario.getRol());
        getContentPane().setBackground(FONDO);
        setLayout(new BorderLayout());

        add(crearEncabezado(usuario), BorderLayout.NORTH);
        add(crearContenido(accesos), BorderLayout.CENTER);
    }

    private JPanel crearEncabezado(Usuario usuario) {
        JPanel encabezado = new JPanel(new BorderLayout(18, 0));
        encabezado.setBackground(new Color(0xF0EDE4));
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                BorderFactory.createEmptyBorder(14, 28, 14, 24)));

        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 3));
        textos.setOpaque(false);
        JLabel marca = new JLabel("EL MOLINO");
        marca.setForeground(PETROLEO);
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, 19f));
        JLabel saludo = new JLabel("Hola, " + usuario.getNombre() + " · " + nombreRol(usuario.getRol()));
        saludo.setForeground(TEXTO_SUAVE);
        saludo.setFont(saludo.getFont().deriveFont(13f));
        textos.add(marca);
        textos.add(saludo);

        JButton cerrarSesion = new JButton("Cerrar sesión");
        cerrarSesion.setToolTipText("Cerrar la sesión actual y volver al inicio de sesión");
        cerrarSesion.getAccessibleContext().setAccessibleName("Cerrar sesión");
        cerrarSesion.getAccessibleContext().setAccessibleDescription("Cierra la sesión actual");
        cerrarSesion.setForeground(PETROLEO);
        cerrarSesion.setBackground(new Color(0xF0EDE4));
        cerrarSesion.setFont(cerrarSesion.getFont().deriveFont(Font.BOLD, 13f));
        cerrarSesion.setFocusPainted(true);
        cerrarSesion.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x8FC1CA), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        cerrarSesion.addActionListener(e -> {
            SesionActual.cerrar();
            dispose();
            SwingUtilities.invokeLater(() -> new VentanaLogin().setVisible(true));
        });

        encabezado.add(textos, BorderLayout.WEST);
        encabezado.add(cerrarSesion, BorderLayout.EAST);
        return encabezado;
    }

    private JScrollPane crearContenido(List<Acceso> accesos) {
        JPanel contenido = new JPanel(new BorderLayout(0, 18));
        contenido.setBackground(FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 30, 28, 30));

        JPanel introduccion = new JPanel(new GridLayout(0, 1, 0, 5));
        introduccion.setOpaque(false);
        JLabel titulo = new JLabel("¿Qué necesitás gestionar?");
        titulo.setForeground(PETROLEO);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        JLabel ayuda = new JLabel("Elegí una sección para consultar o actualizar la información.");
        ayuda.setForeground(TEXTO_SUAVE);
        ayuda.setFont(ayuda.getFont().deriveFont(14f));
        introduccion.add(titulo);
        introduccion.add(ayuda);
        contenido.add(introduccion, BorderLayout.NORTH);

        if (accesos.isEmpty()) {
            JLabel vacio = new JLabel("No hay secciones disponibles para este perfil.", SwingConstants.CENTER);
            vacio.setForeground(TEXTO_SUAVE);
            vacio.setFont(vacio.getFont().deriveFont(16f));
            contenido.add(vacio, BorderLayout.CENTER);
        } else {
            JPanel tarjetas = new JPanel(new GridLayout(0, 2, 12, 12));
            tarjetas.setOpaque(false);
            for (Acceso acceso : accesos) tarjetas.add(crearTarjeta(acceso));
            if (accesos.size() % 2 != 0) {
                JPanel espacio = new JPanel();
                espacio.setOpaque(false);
                tarjetas.add(espacio);
            }
            contenido.add(tarjetas, BorderLayout.CENTER);
        }

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(FONDO);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JButton crearTarjeta(Acceso acceso) {
        JButton boton = new JButton("<html><div style='text-align:left'>"
                + "<b style='font-size:14pt'>" + acceso.titulo() + "</b><br>"
                + "<span style='font-size:10pt'>" + acceso.descripcion() + "</span></div></html>");
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setVerticalAlignment(SwingConstants.CENTER);
        boton.setForeground(PETROLEO);
        boton.setBackground(Color.WHITE);
        boton.setFont(boton.getFont().deriveFont(15f));
        boton.setPreferredSize(new Dimension(300, 82));
        boton.setFocusPainted(true);
        boton.setToolTipText(acceso.descripcion());
        boton.getAccessibleContext().setAccessibleName(acceso.titulo());
        boton.getAccessibleContext().setAccessibleDescription(acceso.descripcion());
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        boton.addChangeListener(e -> {
            boolean activo = boton.getModel().isRollover() || boton.getModel().isPressed() || boton.hasFocus();
            actualizarBorde(boton, activo);
        });
        boton.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { actualizarBorde(boton, true); }
            @Override public void focusLost(FocusEvent e) { actualizarBorde(boton, false); }
        });
        boton.addActionListener(e -> acceso.accion().run());
        return boton;
    }

    private List<Acceso> armarAccesos(String rol) {
        List<Acceso> accesos = new ArrayList<>();
        switch (normalizarRol(rol)) {
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
            }
            case "COORDINADOR" -> {
                accesos.add(modulo("Espacios recreativos", "Consultar y actualizar espacios", GestionDAO.ESPACIOS));
                accesos.add(modulo("Categorías", "Consultar las categorías publicadas en la página", GestionDAO.CATEGORIAS_ACTIVIDAD));
                accesos.add(modulo("Actividades", "Consultar y actualizar actividades", GestionDAO.ACTIVIDADES));
                accesos.add(modulo("Inscripciones", "Agregar y consultar inscripciones", GestionDAO.ACTIVIDAD_INSCRIPTOS));
            }
            case "ADMINISTRADOR" -> {
                accesos.add(modulo("Clientes", "Consultar y editar datos de clientes", null));
                accesos.add(modulo("Acompañantes", "Gestionar los datos de acompañantes", GestionDAO.ACOMPANANTES));
                accesos.add(new Acceso("Acompañantes por reserva", "Editar o quitar acompañantes de una reserva", () -> abrirAcompanantes()));
                accesos.add(modulo("Alojamientos", "Consultar y actualizar alojamientos", GestionDAO.ALOJAMIENTOS));
                accesos.add(modulo("Espacios recreativos", "Consultar y actualizar espacios", GestionDAO.ESPACIOS));
                accesos.add(modulo("Reservas", "Consultar y actualizar reservas", GestionDAO.RESERVAS));
                accesos.add(modulo("Pagos", "Consultar pagos registrados", GestionDAO.PAGOS));
                accesos.add(modulo("Categorías", "Consultar las categorías publicadas en la página", GestionDAO.CATEGORIAS_ACTIVIDAD));
                accesos.add(modulo("Actividades", "Consultar y actualizar actividades", GestionDAO.ACTIVIDADES));
                accesos.add(modulo("Inscripciones", "Agregar y consultar inscripciones", GestionDAO.ACTIVIDAD_INSCRIPTOS));
                accesos.add(modulo("Usuarios", "Crear cuentas, asignar roles y gestionar usuarios", GestionDAO.USUARIOS));
            }
            default -> { }
        }
        return accesos;
    }

    private Acceso modulo(String titulo, String descripcion, GestionDAO.Modulo modulo) {
        return new Acceso(titulo, descripcion, () -> {
            setVisible(false);
            if (modulo == null) new VentanaClientes(this).setVisible(true);
            else new VentanaGestion(this, modulo).setVisible(true);
        });
    }

    private void abrirAcompanantes() {
        setVisible(false);
        new VentanaAcompanantesReserva(this).setVisible(true);
    }

    private void actualizarBorde(JButton boton, boolean resaltado) {
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(resaltado ? VERDE : BORDE, resaltado ? 2 : 1),
                BorderFactory.createEmptyBorder(resaltado ? 11 : 12, resaltado ? 15 : 16,
                        resaltado ? 11 : 12, resaltado ? 15 : 16)));
    }

    private String nombreRol(String rol) {
        return switch (normalizarRol(rol)) {
            case "ADMINISTRADOR" -> "Administrador";
            case "RECEPCION" -> "Recepción";
            case "ADMINISTRACION" -> "Administración";
            case "COORDINADOR" -> "Coordinación";
            case "GERENCIA" -> "Gerencia";
            default -> rol;
        };
    }

    private String normalizarRol(String rol) {
        if (rol == null) return "";
        if ("coordinador_actividades".equalsIgnoreCase(rol)) return "COORDINADOR";
        return rol.toUpperCase(java.util.Locale.ROOT);
    }

    private record Acceso(String titulo, String descripcion, Runnable accion) { }
}
