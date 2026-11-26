package vistas;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class LogIn extends JPanel {

    private static final Color COLOR_AMARILLO = new Color(255, 221, 87);
    private static final Color COLOR_VERDE = new Color(126, 217, 87);

    private JButton botonRegresar;
    private JLabel etiquetaTitulo;
    private JLabel etiquetaCorreo;
    private JTextField campoCorreo;
    private JLabel etiquetaContrasena;
    private JPasswordField campoContrasena;
    private JButton botonIniciarSesion;

    private final Runnable Panel, volverInicio;
    public LogIn(Runnable volverInicio, Runnable Panel) {
        this.Panel= Panel;
        this.volverInicio= volverInicio;
        crearComponentes();
        acomodarComponentes();
    }

    private void crearComponentes() {
        botonRegresar = new JButton("REGRESAR");
        darEstiloBoton(botonRegresar, COLOR_AMARILLO, new Font("Arial", Font.BOLD, 16));
        botonRegresar.setPreferredSize(new Dimension(150, 50));

        etiquetaTitulo = new JLabel("Inicio de sesión", SwingConstants.CENTER);
        etiquetaTitulo.setFont(new Font("Arial", Font.BOLD, 40));

        etiquetaCorreo = new JLabel("Correo");
        etiquetaCorreo.setFont(new Font("Arial", Font.PLAIN, 22));

        campoCorreo = new JTextField();
        darEstiloCampo(campoCorreo);

        etiquetaContrasena = new JLabel("Contraseña");
        etiquetaContrasena.setFont(new Font("Arial", Font.PLAIN, 22));

        campoContrasena = new JPasswordField();
        darEstiloCampo(campoContrasena);

        botonIniciarSesion = new JButton("Iniciar sesión");
        darEstiloBoton(botonIniciarSesion, COLOR_VERDE, new Font("Arial", Font.PLAIN, 20));
        botonIniciarSesion.setPreferredSize(new Dimension(240, 50));
        
        //FUNCIONAMIENTO DE BOTÓN:
        botonIniciarSesion.addActionListener(e -> Panel.run());
        botonRegresar.addActionListener(e -> volverInicio.run());
    }

    private void acomodarComponentes() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Parte de arriba: botón regresar a la izquierda y título al centro
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(Color.WHITE);
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelSuperior.add(botonRegresar, BorderLayout.WEST);
        panelSuperior.add(etiquetaTitulo, BorderLayout.CENTER);
        add(panelSuperior, BorderLayout.NORTH);

        // Parte del centro: formulario
        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBackground(Color.WHITE);

        GridBagConstraints posicion = new GridBagConstraints();
        posicion.gridx = 0;
        posicion.weightx = 1;
        posicion.fill = GridBagConstraints.HORIZONTAL;
        posicion.insets = new Insets(8, 80, 8, 80);

        posicion.gridy = 0;
        panelFormulario.add(etiquetaCorreo, posicion);

        posicion.gridy = 1;
        panelFormulario.add(campoCorreo, posicion);

        posicion.gridy = 2;
        panelFormulario.add(etiquetaContrasena, posicion);

        posicion.gridy = 3;
        panelFormulario.add(campoContrasena, posicion);

        // El botón de iniciar sesión no se estira, va centrado
        posicion.gridy = 4;
        posicion.fill = GridBagConstraints.NONE;
        posicion.anchor = GridBagConstraints.CENTER;
        posicion.insets = new Insets(25, 80, 8, 80);
        panelFormulario.add(botonIniciarSesion, posicion);

        add(panelFormulario, BorderLayout.CENTER);
    }

    private void darEstiloBoton(JButton boton, Color colorFondo, Font letra) {
        boton.setBackground(colorFondo);
        boton.setForeground(Color.BLACK);
        boton.setFont(letra);
        boton.setOpaque(true);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    }

    private void darEstiloCampo(JTextField campo) {
        campo.setFont(new Font("Arial", Font.PLAIN, 18));
        campo.setPreferredSize(new Dimension(400, 45));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));
    }
}