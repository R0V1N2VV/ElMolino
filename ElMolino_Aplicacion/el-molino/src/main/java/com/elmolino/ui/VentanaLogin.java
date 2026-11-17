package com.elmolino.ui;

import com.elmolino.dao.UsuarioDAO;
import com.elmolino.model.Usuario;
import com.elmolino.util.SeguridadPassword;
import com.elmolino.util.LimiteIntentosLogin;
import com.elmolino.util.SesionActual;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/**
 * Pantalla de acceso y registro de cuentas del personal.
 */
public class VentanaLogin extends JFrame {

    private final UsuarioDAO accesoDatosUsuario = new UsuarioDAO();

    private final JTextField campoCorreo = new JTextField();
    private final JPasswordField campoContrasena = new JPasswordField();
    private final JButton botonIngresar = new JButton("Ingresar");
    private final JLabel estado = new JLabel(" ", SwingConstants.CENTER);

    /**
     * Construye el objeto, guarda sus datos iniciales y deja sus componentes preparados para usarse.
     */
    public VentanaLogin() {
        super("El Molino - Iniciar sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 500);
        setMinimumSize(new Dimension(720, 460));
        setLocationRelativeTo(null);

        // La paleta clara y los textos internos muestran que esta pantalla es para empleados.
        Color verdeBosque = new Color(0x2D4A3E);
        Color verdeSuave = new Color(0xE5F2EC);
        Color verdeAccion = new Color(0x4F7358);
        Color textoSecundario = new Color(0x52645E);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(Color.WHITE);

        // El bloque lateral presenta la identidad visual y el bloque derecho contiene el acceso.
        JPanel identidad = new JPanel();
        identidad.setLayout(new javax.swing.BoxLayout(identidad, javax.swing.BoxLayout.Y_AXIS));
        identidad.setBackground(verdeSuave);
        identidad.setBorder(BorderFactory.createEmptyBorder(54, 38, 42, 38));
        identidad.setPreferredSize(new Dimension(350, 500));
        JLabel marca = new JLabel("EL MOLINO");
        marca.setForeground(verdeBosque);
        marca.setFont(marca.getFont().deriveFont(Font.BOLD, 27f));
        JLabel categoria = new JLabel("SISTEMA DE GESTIÓN");
        categoria.setForeground(new Color(0x1B6B7A));
        categoria.setFont(categoria.getFont().deriveFont(Font.BOLD, 12f));
        categoria.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JPanel lineaAcento = new JPanel();
        lineaAcento.setBackground(new Color(0x7FA87A));
        lineaAcento.setMaximumSize(new Dimension(56, 4));
        lineaAcento.setPreferredSize(new Dimension(56, 4));
        lineaAcento.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lema = new JLabel("<html><div style='width:260px'>Herramientas para el equipo de El Molino.</div></html>");
        lema.setForeground(verdeBosque);
        lema.setFont(lema.getFont().deriveFont(Font.BOLD, 20f));
        lema.setBorder(BorderFactory.createEmptyBorder(28, 0, 12, 0));
        lema.setVerticalAlignment(SwingConstants.TOP);
        lema.setPreferredSize(new Dimension(280, 105));
        JLabel descripcion = new JLabel("<html><div style='width:260px'>Acceso al sistema interno para el personal.</div></html>");
        descripcion.setForeground(textoSecundario);
        descripcion.setFont(descripcion.getFont().deriveFont(14f));
        descripcion.setVerticalAlignment(SwingConstants.TOP);
        descripcion.setPreferredSize(new Dimension(280, 80));
        identidad.add(marca);
        identidad.add(categoria);
        identidad.add(javax.swing.Box.createVerticalStrut(22));
        identidad.add(lineaAcento);
        identidad.add(lema);
        identidad.add(descripcion);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(BorderFactory.createEmptyBorder(42, 44, 38, 44));

        Dimension tamañoCampo = new Dimension(220, 32);
        campoCorreo.setPreferredSize(tamañoCampo);
        campoContrasena.setPreferredSize(tamañoCampo);
        campoCorreo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCFE4DC)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        campoContrasena.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCFE4DC)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        // GridBag permite alinear etiquetas y campos en una grilla compacta.
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(9, 8, 9, 8);
        restricciones.fill = GridBagConstraints.HORIZONTAL;

        restricciones.gridx = 0; restricciones.gridy = 0; restricciones.gridwidth = 2;
        JLabel titulo = new JLabel("Acceso al sistema", SwingConstants.LEFT);
        titulo.setForeground(verdeBosque);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        formulario.add(titulo, restricciones);

        restricciones.gridy++;
        JLabel subtitulo = new JLabel("Ingresá con tu cuenta de empleado");
        subtitulo.setForeground(textoSecundario);
        subtitulo.setFont(subtitulo.getFont().deriveFont(14f));
        formulario.add(subtitulo, restricciones);

        restricciones.gridwidth = 1;
        restricciones.gridy++;
        restricciones.gridx = 0; restricciones.weightx = 0;
        JLabel etiquetaCorreo = new JLabel("Email");
        etiquetaCorreo.setForeground(verdeBosque);
        etiquetaCorreo.setFont(etiquetaCorreo.getFont().deriveFont(Font.BOLD, 14f));
        formulario.add(etiquetaCorreo, restricciones);
        restricciones.gridx = 1; restricciones.weightx = 1;
        formulario.add(campoCorreo, restricciones);

        restricciones.gridy++;
        restricciones.gridx = 0; restricciones.weightx = 0;
        JLabel etiquetaContrasena = new JLabel("Contraseña");
        etiquetaContrasena.setForeground(verdeBosque);
        etiquetaContrasena.setFont(etiquetaContrasena.getFont().deriveFont(Font.BOLD, 14f));
        formulario.add(etiquetaContrasena, restricciones);
        restricciones.gridx = 1; restricciones.weightx = 1;
        formulario.add(campoContrasena, restricciones);

        restricciones.gridy++;
        restricciones.gridx = 0; restricciones.gridwidth = 2; restricciones.weightx = 0;
        restricciones.insets = new Insets(16, 8, 8, 8);
        botonIngresar.setText("Ingresar");
        botonIngresar.setForeground(Color.WHITE);
        botonIngresar.setBackground(verdeAccion);
        botonIngresar.setFont(botonIngresar.getFont().deriveFont(Font.BOLD, 15f));
        botonIngresar.setPreferredSize(new Dimension(220, 44));
        botonIngresar.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        botonIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botonIngresar.setOpaque(true);
        botonIngresar.setContentAreaFilled(true);
        formulario.add(botonIngresar, restricciones);

        restricciones.gridy++;
        restricciones.insets = new Insets(4, 8, 8, 8);
        estado.setForeground(textoSecundario);
        formulario.add(estado, restricciones);

        contenedor.add(identidad, BorderLayout.WEST);
        contenedor.add(formulario, BorderLayout.CENTER);
        add(contenedor);

        botonIngresar.addActionListener(evento -> intentarLogin());
        campoContrasena.addActionListener(evento -> intentarLogin());

        getRootPane().setDefaultButton(botonIngresar);
    }

    /**
     * Ejecuta la lógica de “intentar login” en VentanaLogin.java.
     */
    private void intentarLogin() {
        // Valida los campos antes de consultar la cuenta y comprobar su clave.
        String correo = campoCorreo.getText().trim();
        String contrasena = new String(campoContrasena.getPassword());

        if (correo.isEmpty() || contrasena.isEmpty()) {
            estado.setText("Completá email y contraseña");
            return;
        }

        java.time.Duration espera = LimiteIntentosLogin.tiempoRestante(correo);
        if (!espera.isZero()) {
            estado.setText("Demasiados intentos. Probá de nuevo en "
                    + Math.max(1, espera.toMinutes()) + " min.");
            return;
        }

        botonIngresar.setEnabled(false);
        estado.setText("Verificando...");

        // La consulta se ejecuta fuera del hilo grafico para mantener la ventana activa.
        new SwingWorker<Usuario, Void>() {
            /**
             * Ejecuta la tarea pesada en segundo plano para que la interfaz no se congele.
             */
            @Override
            protected Usuario doInBackground() throws Exception {
                if (!LimiteIntentosLogin.tiempoRestante(correo).isZero()) return null;
                Usuario usuario = accesoDatosUsuario.buscarPorEmail(correo);
                if (usuario != null && usuario.getRol() != null
                        && UsuarioDAO.ROLES_EMPLEADO.contains(usuario.getRol().toUpperCase(java.util.Locale.ROOT))
                        && SeguridadPassword.verificar(
                        contrasena.toCharArray(), usuario.getPasswordHash())) {
                    if (SeguridadPassword.necesitaMigracion(usuario.getPasswordHash())) {
                        accesoDatosUsuario.actualizarPasswordHash(usuario.getIdUsuario(),
                                SeguridadPassword.crearHash(contrasena.toCharArray()));
                    }
                    LimiteIntentosLogin.registrarExito(correo);
                    return usuario;
                }
                LimiteIntentosLogin.registrarFallo(correo);
                return null;
            }

            /**
             * Recibe en la interfaz el resultado de la tarea en segundo plano y actualiza la pantalla.
             */
            @Override
            protected void done() {
                botonIngresar.setEnabled(true);
                try {
                    Usuario usuario = get();
                    if (usuario == null) {
                        java.time.Duration restante = LimiteIntentosLogin.tiempoRestante(correo);
                        estado.setText(!restante.isZero()
                                ? "Demasiados intentos. Probá de nuevo en "
                                        + Math.max(1, restante.toMinutes()) + " min."
                                : "Email o contraseña incorrectos");
                        campoContrasena.setText("");
                        return;
                    }
                    SesionActual.iniciar(usuario);
                    dispose();
                    SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
                } catch (InterruptedException | ExecutionException excepcion) {
                    Throwable causa = excepcion.getCause() != null ? excepcion.getCause() : excepcion;
                    estado.setText("Error de conexión");
                    JOptionPane.showMessageDialog(VentanaLogin.this,
                            "No se pudo verificar el usuario:\n" + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Prepara y muestra registro.
     */
}