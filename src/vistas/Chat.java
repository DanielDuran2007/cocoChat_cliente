package vistas;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.*;
import protocolo.Protocolo.Fila;

/** Panel central. Usa el mismo layout para conversaciones privadas y grupales. */
final class Chat extends JPanel {
    private final JLabel titulo = new JLabel("Selecciona un usuario o un grupo.");
    private final JTextArea historial = new JTextArea(18, 40);
    private final JTextField mensaje = new JTextField();
    private final JButton enviar = new JButton("Enviar");

    Chat(Consumer<String> enviarTexto) {
        super(new BorderLayout(6, 6));
        historial.setEditable(false);
        historial.setLineWrap(true);
        historial.setWrapStyleWord(true);
        add(titulo, BorderLayout.NORTH);
        add(new JScrollPane(historial), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(6, 6));
        pie.add(mensaje, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.add(enviar);
        pie.add(botones, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        enviar.addActionListener(evento -> enviarTexto.accept(mensaje.getText()));
        mensaje.addActionListener(evento -> enviarTexto.accept(mensaje.getText()));
        activarEnvio(false);
    }

    void abrir(String etiqueta) {
        titulo.setText(etiqueta);
        historial.setText("");
        mensaje.setText("");
    }

    void mostrar(List<Fila> filas) {
        // Conservamos la posición de lectura cuando el sondeo devuelve los mismos mensajes.
        String texto = filas.stream()
                .map(fila -> fila.titulo() + " | " + fila.detalle())
                .collect(Collectors.joining("\n"));
        if (!texto.equals(historial.getText())) {
            historial.setText(texto);
            historial.setCaretPosition(historial.getDocument().getLength());
        }
    }

    void limpiarSiCoincide(String enviado) {
        if (mensaje.getText().equals(enviado)) mensaje.setText("");
    }

    void activarEnvio(boolean activo) {
        mensaje.setEnabled(activo);
        enviar.setEnabled(activo);
    }
}
