package vistas;

// Aqui va el formulario de registro
import javax.swing.*;
import java.awt.*;

public class Registro extends JPanel {

    private JTextField txtNombre;
    private JTextField txtApellidos;
    private JTextField txtCorreo;
    private JPasswordField txtContrasena;
    private JPasswordField txtConfirmarContrasena;
    private JButton btnCrearCuenta;
    private final Runnable Panel;

    public Registro(Runnable volverInicio, Runnable Panel) {
        this.Panel= Panel;
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(
                BorderFactory.createEmptyBorder(25, 55, 25, 55)
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Titulo
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Color.WHITE);
        cabecera.setBorder(BorderFactory.createEmptyBorder(15, 25, 0, 25));

        JButton btnRegresar = new JButton("REGRESAR");
        btnRegresar.setPreferredSize(new Dimension(150, 52));
        btnRegresar.setBackground(new Color(255, 222, 89));
        btnRegresar.setForeground(Color.BLACK);
        btnRegresar.setFont(new Font("Arial", Font.BOLD, 14));
        btnRegresar.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        btnRegresar.setOpaque(true);

        JLabel titulo = new JLabel("Registro", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 40));

        cabecera.add(btnRegresar, BorderLayout.WEST);
        cabecera.add(titulo, BorderLayout.CENTER);

        // Reserva a la derecha el mismo ancho que ocupa el botón.
        // Así el título queda centrado respecto a toda la ventana.
        cabecera.add(Box.createHorizontalStrut(150), BorderLayout.EAST);

        add(cabecera, BorderLayout.NORTH);
        formulario.add(titulo, gbc);

        // Nombre
        agregarCampo(formulario, gbc, "Nombre", 1);
        txtNombre = crearCampo();
        agregarEntrada(formulario, gbc, txtNombre, 2);

        // Apellidos
        agregarCampo(formulario, gbc, "Apellidos", 3);
        txtApellidos = crearCampo();
        agregarEntrada(formulario, gbc, txtApellidos, 4);

        // Correo
        agregarCampo(formulario, gbc, "Correo", 5);
        txtCorreo = crearCampo();
        agregarEntrada(formulario, gbc, txtCorreo, 6);

        // Contraseña
        agregarCampo(formulario, gbc, "Contraseña", 7);
        txtContrasena = new JPasswordField();
        configurarCampo(txtContrasena);
        agregarEntrada(formulario, gbc, txtContrasena, 8);

        // Confirmar contraseña
        agregarCampo(formulario, gbc, "Confirmar contraseña", 9);
        txtConfirmarContrasena = new JPasswordField();
        configurarCampo(txtConfirmarContrasena);
        agregarEntrada(formulario, gbc, txtConfirmarContrasena, 10);

        // Boton
        btnCrearCuenta = new JButton("Crear cuenta");
        btnCrearCuenta.setPreferredSize(new Dimension(300, 45));
        btnCrearCuenta.setBackground(Color.BLACK);
        btnCrearCuenta.setForeground(Color.WHITE);
        btnCrearCuenta.setFocusPainted(false);

        gbc.gridy = 11;
        gbc.insets = new Insets(25, 0, 10, 0);
        formulario.add(btnCrearCuenta, gbc);

        add(formulario, BorderLayout.CENTER);

        //ACCIONES
        btnCrearCuenta.addActionListener(e -> validarRegistro());
        btnRegresar.addActionListener(evento -> volverInicio.run());
    }

    private JTextField crearCampo() {
        JTextField campo = new JTextField();
        configurarCampo(campo);
        return campo;
    }

    private void configurarCampo(JTextField campo) {
        campo.setFont(new Font("Arial", Font.PLAIN, 20));
        campo.setPreferredSize(new Dimension(300, 45));

        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc,
            String texto, int fila) {

        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(new Font("Arial", Font.PLAIN, 25));

        gbc.gridy = fila;
        gbc.insets = new Insets(12, 0, 5, 0);

        panel.add(etiqueta, gbc);
    }

    private void agregarEntrada(JPanel panel, GridBagConstraints gbc,
            JTextField campo, int fila) {

        gbc.gridy = fila;
        gbc.insets = new Insets(5, 0, 15, 0);

        panel.add(campo, gbc);
    }

    //Parte lógica
    private void validarRegistro() {
        String nombre = txtNombre.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String correo = txtCorreo.getText().trim();
        String contrasena = new String(txtContrasena.getPassword());
        String confirmacion = new String(txtConfirmarContrasena.getPassword());

        if (nombre.isEmpty() || apellidos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe tu nombre y apellidos.");
            return;
        }

        if (!correo.matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+")) {
            JOptionPane.showMessageDialog(this, "Correo inválido");
            return;
        }

        if (contrasena.length() < 8) {
            JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 8 dígitos.");
            return;
        }

        if (!contrasena.equals(confirmacion)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.");
            return;
        }

        // Aquí irá la petición al servidor cuando implementes el registro.
        JOptionPane.showMessageDialog(this, "Los datos son válidos.");
        Panel.run();
    }
}
