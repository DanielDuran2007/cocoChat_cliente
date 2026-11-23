package vistas;

import cocochat_cliente.ConexionChat;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import protocolo.Protocolo.Fila;
import protocolo.Protocolo.Respuesta;

/** Ventana Swing armada con layouts y componentes estándar. */
public final class LogIn extends JFrame {
    private final JTextField ip;
    private final JTextField puerto;
    private final JTextField correo = new JTextField(22);
    private final JPasswordField clave = new JPasswordField(22);
    private final JButton entrar = new JButton("Iniciar sesión");
    private final JButton registrar = new JButton("Crear cuenta");
    private final JLabel estado = new JLabel("Indica la dirección del servidor.");

    public LogIn(String direccionInicial, int puertoInicial) {
        super("CocoChat | Inicio de sesión");
        ip = new JTextField(direccionInicial, 22);
        puerto = new JTextField(String.valueOf(puertoInicial), 22);

        // GridLayout pone cada etiqueta a la izquierda y su campo a la derecha.
        JPanel datos = new JPanel(new GridLayout(0, 2, 8, 8));
        datos.add(new JLabel("IP del servidor"));
        datos.add(ip);
        datos.add(new JLabel("Puerto"));
        datos.add(puerto);
        datos.add(new JLabel("Correo"));
        datos.add(correo);
        datos.add(new JLabel("Contraseña"));
        datos.add(clave);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(registrar);
        botones.add(entrar);
        JPanel pie = new JPanel(new BorderLayout());
        pie.add(botones, BorderLayout.CENTER);
        pie.add(estado, BorderLayout.SOUTH);

        setLayout(new BorderLayout(8, 8));
        add(datos, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(entrar);
        entrar.addActionListener(evento -> enviarLogin());
        registrar.addActionListener(evento -> mostrarRegistro());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private record Direccion(String ip, int puerto) {}
    private record Resultado(ConexionChat conexion, Respuesta respuesta) {}

    private Direccion direccion() {
        try {
            int numero = Integer.parseInt(puerto.getText().strip());
            if (numero < 1 || numero > 65535 || ip.getText().isBlank()) {
                throw new NumberFormatException();
            }
            return new Direccion(ip.getText().strip(), numero);
        } catch (NumberFormatException error) {
            estado.setText("Escribe una IP y un puerto entre 1 y 65535.");
            return null;
        }
    }

    private void enviarLogin() {
        Direccion destino = direccion();
        if (destino != null) {
            solicitar(destino, "LOGIN", correo.getText(), new String(clave.getPassword()));
        }
    }

    private void mostrarRegistro() {
        Direccion destino = direccion();
        if (destino == null) return;
        JTextField nombre = new JTextField(24);
        JTextField apellido = new JTextField(24);
        JTextField email = new JTextField(correo.getText(), 24);
        JTextField telefono = new JTextField(24);
        JComboBox<String> genero = new JComboBox<>(new String[]{
            "Prefiero no decir", "Masculino", "Femenino", "Otro"
        });
        JPasswordField contrasena = new JPasswordField(24);
        JPasswordField confirmar = new JPasswordField(24);
        JPanel datos = new JPanel(new GridLayout(0, 2, 8, 8));
        datos.add(new JLabel("Nombre"));
        datos.add(nombre);
        datos.add(new JLabel("Apellido"));
        datos.add(apellido);
        datos.add(new JLabel("Correo"));
        datos.add(email);
        datos.add(new JLabel("Teléfono (opcional)"));
        datos.add(telefono);
        datos.add(new JLabel("Género (opcional)"));
        datos.add(genero);
        datos.add(new JLabel("Contraseña (8 caracteres o más)"));
        datos.add(contrasena);
        datos.add(new JLabel("Repite la contraseña"));
        datos.add(confirmar);
        if (JOptionPane.showConfirmDialog(this, datos, "Crear cuenta",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String nueva = new String(contrasena.getPassword());
        if (!nueva.equals(new String(confirmar.getPassword()))) {
            estado.setText("Las contraseñas deben coincidir.");
            return;
        }
        correo.setText(email.getText());
        String generoElegido = genero.getSelectedIndex() == 0 ? "" : (String) genero.getSelectedItem();
        solicitar(destino, "REGISTRAR", nombre.getText(), apellido.getText(), email.getText(),
                nueva, telefono.getText(), generoElegido);
    }

    private void solicitar(Direccion destino, String accion, String... campos) {
        entrar.setEnabled(false);
        registrar.setEnabled(false);
        estado.setText("Conectando con el servidor…");
        // doInBackground espera la red. done vuelve al hilo de Swing para mostrar el resultado.
        new SwingWorker<Resultado, Void>() {
            @Override
            protected Resultado doInBackground() throws IOException {
                ConexionChat conexion = new ConexionChat(destino.ip(), destino.puerto());
                Respuesta respuesta = conexion.pedir(accion, campos);
                if (accion.equals("LOGIN") && respuesta.correcta()) {
                    return new Resultado(conexion, respuesta);
                }
                // Registrar una cuenta no deja una sesión abierta.
                conexion.close();
                return new Resultado(null, respuesta);
            }

            @Override
            protected void done() {
                entrar.setEnabled(true);
                registrar.setEnabled(true);
                try {
                    Resultado resultado = get();
                    estado.setText(resultado.respuesta().aviso());
                    if (resultado.conexion() != null) {
                        Fila usuario = resultado.respuesta().filas().get(0);
                        new Coco(resultado.conexion(), usuario, destino.ip(), destino.puerto()).setVisible(true);
                        dispose();
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    estado.setText("No se pudo conectar. Revisa la IP, el puerto y el servidor.");
                }
            }
        }.execute();
    }
}
