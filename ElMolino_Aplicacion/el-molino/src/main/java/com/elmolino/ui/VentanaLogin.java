package com.elmolino.ui;

import com.elmolino.dao.UsuarioDAO;
import com.elmolino.model.Usuario;
import com.elmolino.util.SeguridadPassword;
import com.elmolino.util.SesionActual;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
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

    private void intentarLogin() {
        // Valida los campos antes de consultar la cuenta y comprobar su clave.
        String correo = campoCorreo.getText().trim();
        String contrasena = new String(campoContrasena.getPassword());

        if (correo.isEmpty() || contrasena.isEmpty()) {
            estado.setText("Completá email y contraseña");
            return;
        }

        botonIngresar.setEnabled(false);
        estado.setText("Verificando...");

        // La consulta se ejecuta fuera del hilo grafico para mantener la ventana activa.
        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() throws Exception {
                Usuario usuario = accesoDatosUsuario.buscarPorEmail(correo);
                if (usuario != null && SeguridadPassword.verificar(
                        contrasena.toCharArray(), usuario.getPasswordHash())) {
                    if (SeguridadPassword.necesitaMigracion(usuario.getPasswordHash())) {
                        accesoDatosUsuario.actualizarPasswordHash(usuario.getIdUsuario(),
                                SeguridadPassword.crearHash(contrasena.toCharArray()));
                    }
                    return usuario;
                }
                return null;
            }

            @Override
            protected void done() {
                botonIngresar.setEnabled(true);
                try {
                    Usuario usuario = get();
                    if (usuario == null) {
                        estado.setText("Email o contraseña incorrectos");
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

    private void mostrarRegistro() {
        // El formulario registra una cuenta nueva con el rol inicial definido por el DAO.
        JDialog dialogo = new JDialog(this, "Crear cuenta", true);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.setSize(430, 380);
        dialogo.setLocationRelativeTo(this);

        JTextField nombre = new JTextField(18);
        JTextField apellido = new JTextField(18);
        JTextField correo = new JTextField(18);
        JPasswordField contrasena = new JPasswordField(18);
        JPasswordField confirmar = new JPasswordField(18);
        JLabel mensaje = new JLabel("Las cuentas nuevas comienzan con rol Recepción.");
        JButton crear = new JButton("Registrarse");
        JButton cancelar = new JButton("Cancelar");

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(6, 6, 6, 6);
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        restricciones.gridx = 0;
        restricciones.gridy = 0;
        formulario.add(new JLabel("Nombre:"), restricciones);
        restricciones.gridx = 1;
        formulario.add(nombre, restricciones);
        restricciones.gridx = 0;
        restricciones.gridy++;
        formulario.add(new JLabel("Apellido:"), restricciones);
        restricciones.gridx = 1;
        formulario.add(apellido, restricciones);
        restricciones.gridx = 0;
        restricciones.gridy++;
        formulario.add(new JLabel("Email:"), restricciones);
        restricciones.gridx = 1;
        formulario.add(correo, restricciones);
        restricciones.gridx = 0;
        restricciones.gridy++;
        formulario.add(new JLabel("Contraseña:"), restricciones);
        restricciones.gridx = 1;
        formulario.add(contrasena, restricciones);
        restricciones.gridx = 0;
        restricciones.gridy++;
        formulario.add(new JLabel("Repetir contraseña:"), restricciones);
        restricciones.gridx = 1;
        formulario.add(confirmar, restricciones);
        restricciones.gridx = 0;
        restricciones.gridy++;
        restricciones.gridwidth = 2;
        formulario.add(mensaje, restricciones);

        JPanel acciones = new JPanel();
        acciones.add(crear);
        acciones.add(cancelar);
        restricciones.gridy++;
        formulario.add(acciones, restricciones);
        dialogo.setContentPane(formulario);

        cancelar.addActionListener(evento -> dialogo.dispose());
        crear.addActionListener(evento -> {
            String nombreValor = nombre.getText().trim();
            String apellidoValor = apellido.getText().trim();
            String correoValor = correo.getText().trim().toLowerCase(java.util.Locale.ROOT);
            char[] clave = contrasena.getPassword();
            char[] claveConfirmada = confirmar.getPassword();

            if (nombreValor.isEmpty() || apellidoValor.isEmpty()) {
                mensaje.setText("Completá tu nombre y apellido.");
                return;
            }
            if (!correoValor.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                mensaje.setText("Ingresá un email válido.");
                return;
            }
            if (clave.length < 8) {
                mensaje.setText("La contraseña debe tener al menos 8 caracteres.");
                return;
            }
            if (!java.util.Arrays.equals(clave, claveConfirmada)) {
                mensaje.setText("Las contraseñas no coinciden.");
                return;
            }

            crear.setEnabled(false);
            cancelar.setEnabled(false);
            mensaje.setText("Creando cuenta...");
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    String huella = SeguridadPassword.crearHash(clave);
                    return accesoDatosUsuario.crearCuenta(nombreValor, apellidoValor, correoValor, huella);
                }

                @Override
                protected void done() {
                    crear.setEnabled(true);
                    cancelar.setEnabled(true);
                    try {
                        if (!get()) {
                            mensaje.setText("Ese email ya está registrado.");
                            return;
                        }
                        campoCorreo.setText(correoValor);
                        dialogo.dispose();
                        JOptionPane.showMessageDialog(VentanaLogin.this,
                                "Cuenta creada. Ya podés iniciar sesión.",
                                "Registro completado", JOptionPane.INFORMATION_MESSAGE);
                    } catch (InterruptedException excepcion) {
                        Thread.currentThread().interrupt();
                        mensaje.setText("Se interrumpió el registro. Intentá de nuevo.");
                    } catch (ExecutionException excepcion) {
                        Throwable causa = excepcion.getCause() != null ? excepcion.getCause() : excepcion;
                        mensaje.setText("No se pudo crear la cuenta.");
                        JOptionPane.showMessageDialog(dialogo,
                                "No se pudo completar el registro:\n" + causa.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        dialogo.getRootPane().setDefaultButton(crear);
        dialogo.setVisible(true);
    }
}
