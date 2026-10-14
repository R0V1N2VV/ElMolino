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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/**
 * Pantalla de inicio de sesión y registro de cuentas.
 */
public class VentanaLogin extends JFrame {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private final JTextField campoEmail = new JTextField();
    private final JPasswordField campoPassword = new JPasswordField();
    private final JButton btnIngresar = new JButton("Ingresar");
    private final JLabel estado = new JLabel(" ", SwingConstants.CENTER);

    public VentanaLogin() {
        super("El Molino - Iniciar sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(440, 370);
        setMinimumSize(new Dimension(440, 370));
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        Dimension tamañoCampo = new Dimension(220, 32);
        campoEmail.setPreferredSize(tamañoCampo);
        campoPassword.setPreferredSize(tamañoCampo);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel titulo = new JLabel("El Molino", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        panel.add(titulo, c);

        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0; c.weightx = 0;
        panel.add(new JLabel("Email:"), c);
        c.gridx = 1; c.weightx = 1;
        panel.add(campoEmail, c);

        c.gridy++;
        c.gridx = 0; c.weightx = 0;
        panel.add(new JLabel("Contraseña:"), c);
        c.gridx = 1; c.weightx = 1;
        panel.add(campoPassword, c);

        c.gridy++;
        c.gridx = 0; c.gridwidth = 2; c.weightx = 0;
        c.insets = new Insets(16, 8, 8, 8);
        btnIngresar.setPreferredSize(new Dimension(100, 34));
        panel.add(btnIngresar, c);

        c.gridy++;
        c.insets = new Insets(4, 8, 8, 8);
        panel.add(estado, c);

        add(panel);

        btnIngresar.addActionListener(e -> intentarLogin());
        campoPassword.addActionListener(e -> intentarLogin());

        getRootPane().setDefaultButton(btnIngresar);
    }

    private void intentarLogin() {
        String email = campoEmail.getText().trim();
        String password = new String(campoPassword.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            estado.setText("Completá email y contraseña");
            return;
        }

        btnIngresar.setEnabled(false);
        estado.setText("Verificando...");

        new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() throws Exception {
                Usuario u = usuarioDAO.buscarPorEmail(email);
                if (u != null && SeguridadPassword.verificar(
                        password.toCharArray(), u.getPasswordHash())) {
                    if (SeguridadPassword.necesitaMigracion(u.getPasswordHash())) {
                        usuarioDAO.actualizarPasswordHash(u.getIdUsuario(),
                                SeguridadPassword.crearHash(password.toCharArray()));
                    }
                    return u;
                }
                return null;
            }

            @Override
            protected void done() {
                btnIngresar.setEnabled(true);
                try {
                    Usuario u = get();
                    if (u == null) {
                        estado.setText("Email o contraseña incorrectos");
                        campoPassword.setText("");
                        return;
                    }
                    SesionActual.iniciar(u);
                    dispose();
                    SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    estado.setText("Error de conexión");
                    JOptionPane.showMessageDialog(VentanaLogin.this,
                            "No se pudo verificar el usuario:\n" + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void mostrarRegistro() {
        JDialog dialogo = new JDialog(this, "Crear cuenta", true);
        dialogo.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialogo.setSize(430, 380);
        dialogo.setLocationRelativeTo(this);

        JTextField nombre = new JTextField(18);
        JTextField apellido = new JTextField(18);
        JTextField email = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JPasswordField confirmar = new JPasswordField(18);
        JLabel mensaje = new JLabel("Las cuentas nuevas comienzan con rol Recepción.");
        JButton crear = new JButton("Registrarse");
        JButton cancelar = new JButton("Cancelar");

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        formulario.add(new JLabel("Nombre:"), c);
        c.gridx = 1;
        formulario.add(nombre, c);
        c.gridx = 0;
        c.gridy++;
        formulario.add(new JLabel("Apellido:"), c);
        c.gridx = 1;
        formulario.add(apellido, c);
        c.gridx = 0;
        c.gridy++;
        formulario.add(new JLabel("Email:"), c);
        c.gridx = 1;
        formulario.add(email, c);
        c.gridx = 0;
        c.gridy++;
        formulario.add(new JLabel("Contraseña:"), c);
        c.gridx = 1;
        formulario.add(password, c);
        c.gridx = 0;
        c.gridy++;
        formulario.add(new JLabel("Repetir contraseña:"), c);
        c.gridx = 1;
        formulario.add(confirmar, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        formulario.add(mensaje, c);

        JPanel acciones = new JPanel();
        acciones.add(crear);
        acciones.add(cancelar);
        c.gridy++;
        formulario.add(acciones, c);
        dialogo.setContentPane(formulario);

        cancelar.addActionListener(e -> dialogo.dispose());
        crear.addActionListener(e -> {
            String nombreValor = nombre.getText().trim();
            String apellidoValor = apellido.getText().trim();
            String emailValor = email.getText().trim().toLowerCase(java.util.Locale.ROOT);
            char[] clave = password.getPassword();
            char[] claveConfirmada = confirmar.getPassword();

            if (nombreValor.isEmpty() || apellidoValor.isEmpty()) {
                mensaje.setText("Completá tu nombre y apellido.");
                return;
            }
            if (!emailValor.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
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
                    String hash = SeguridadPassword.crearHash(clave);
                    return usuarioDAO.crearCuenta(nombreValor, apellidoValor, emailValor, hash);
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
                        campoEmail.setText(emailValor);
                        dialogo.dispose();
                        JOptionPane.showMessageDialog(VentanaLogin.this,
                                "Cuenta creada. Ya podés iniciar sesión.",
                                "Registro completado", JOptionPane.INFORMATION_MESSAGE);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        mensaje.setText("Se interrumpió el registro. Intentá de nuevo.");
                    } catch (ExecutionException ex) {
                        Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
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
