package vistas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Portada basada en la primera página del PDF. Los botones solo son visuales. */
public final class Inicio extends JPanel {

    public Inicio() {
        super(new GridBagLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 506));

        // Este panel dibuja el marco negro de la portada. Los márgenes
        // quedan fuera, para que el marco no toque los bordes de la ventana.
        JPanel marco = new JPanel(new GridBagLayout());
        marco.setBackground(Color.WHITE);
        marco.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

        GridBagConstraints exterior = new GridBagConstraints();
        exterior.weightx = 1;
        exterior.weighty = 1;
        exterior.fill = GridBagConstraints.BOTH;
        exterior.insets = new Insets(36, 70, 36, 70);
        add(marco, exterior);

        JLabel titulo = new JLabel("COCO-CHAT");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 52f));
        titulo.setForeground(Color.BLACK);

        JButton iniciarSesion = new BotonPortada("INICIAR SESIÓN", new Color(255, 148, 77));
        JButton registrarse = new BotonPortada("REGISTRARSE", new Color(130, 218, 76));

        // Una sola columna mantiene el título y los botones centrados.
        GridBagConstraints fila = new GridBagConstraints();
        fila.gridx = 0;
        fila.gridy = 0;
        fila.insets = new Insets(0, 0, 46, 0);
        marco.add(titulo, fila);

        fila.gridy = 1;
        fila.insets = new Insets(0, 0, 28, 0);
        marco.add(iniciarSesion, fila);

        fila.gridy = 2;
        fila.insets = new Insets(0, 0, 0, 0);
        marco.add(registrarse, fila);

        // Más adelante, aquí se conectarán los botones con el CardLayout.
    }

    /** JButton normal con un fondo redondeado dibujado por Java 2D. */
    private static final class BotonPortada extends JButton {
        private final Color fondo;

        BotonPortada(String texto, Color fondo) {
            super(texto);
            this.fondo = fondo;
            setPreferredSize(new Dimension(405, 82));
            setFont(getFont().deriveFont(Font.BOLD, 23f));
            setForeground(Color.BLACK);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graficos) {
            // Trabajamos sobre una copia para no cambiar el dibujo del texto.
            Graphics2D dibujo = (Graphics2D) graficos.create();
            try {
                dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                dibujo.setColor(fondo);
                dibujo.fillRoundRect(0, 0, getWidth(), getHeight(), 46, 46);
            } finally {
                dibujo.dispose();
            }
            super.paintComponent(graficos);
        }
    }
}
