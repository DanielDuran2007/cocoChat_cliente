package vistas;

import cocochat_cliente.ConexionChat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import protocolo.Protocolo.Fila;
import protocolo.Protocolo.Respuesta;

/**
 * Pantalla "Crear nuevo grupo".
 * Mínimo 3 personas: creador y dos invitados.
 * El servidor solo permite agregar a usuarios que ya son amigos.
 */

public class CrearGrupo extends JPanel {

    private static final int MIN_INVITADOS = 2;
    private static final int MAX_NOMBRE = 80; // mismo límite que el servidor

    private static final Color AMARILLO = new Color(255, 221, 89);
    private static final Color NARANJA = new Color(255, 146, 77);
    private static final Color VERDE = new Color(128, 219, 90);

    private final Runnable volver;
    private final Runnable alCrearGrupo;

    private final JPanel listaUsuarios = new JPanel(new GridBagLayout());
    private final JTextField campoNombre = new JTextField();
    private final JButton btnCrear = new JButton("CREAR");
    private final JLabel estado = new JLabel(" ", SwingConstants.CENTER);

    // IDs de los usuarios seleccionados.
    private final Set<Integer> invitados = new LinkedHashSet<>();
    private final List<JButton> botonesInvitar = new ArrayList<>();

    private ConexionChat conexion;
    private boolean ocupado;

    // Resultado de intentar crear el grupo en el servidor.
    private record Resultado(boolean creado, String aviso) {}

    public CrearGrupo(Runnable volver, Runnable alCrearGrupo) {
        super(new BorderLayout(10, 10));
        this.volver = volver;
        this.alCrearGrupo = alCrearGrupo;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 506));
        setBorder(BorderFactory.createEmptyBorder(15, 40, 10, 40));

        add(crearCabecera(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 0.5;
        c.insets = new Insets(0, 25, 0, 25);
        centro.add(crearColumnaUsuarios(), c);
        centro.add(crearColumnaDatos(), c);
        add(centro, BorderLayout.CENTER);

        add(estado, BorderLayout.SOUTH);

        // CardLayout avisa cada vez que esta pantalla se muestra.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                cargarAmigos();
            }
        });
        actualizarControles();
    }

    // Llámalo después de un LOGIN exitoso, con la misma conexión.
    public void iniciarSesion(ConexionChat conexionAutenticada) {
        conexion = conexionAutenticada;
        limpiar();
        actualizarControles();
    }

    private JPanel crearCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);

        JButton btnRegresar = new JButton("REGRESAR");
        darEstilo(btnRegresar, AMARILLO, 14f);
        btnRegresar.setPreferredSize(new Dimension(130, 50));
        btnRegresar.addActionListener(e -> volver.run());

        JPanel izquierda = new JPanel(new GridBagLayout());
        izquierda.setOpaque(false);
        izquierda.add(btnRegresar);

        JLabel titulo = new JLabel("Crear nuevo grupo", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 40));

        cabecera.add(izquierda, BorderLayout.WEST);
        cabecera.add(titulo, BorderLayout.CENTER);
        cabecera.add(Box.createHorizontalStrut(130), BorderLayout.EAST);
        return cabecera;
    }

    private JPanel crearColumnaUsuarios() {
        JPanel columna = new JPanel(new BorderLayout(0, 10));
        columna.setOpaque(false);
        columna.setPreferredSize(new Dimension(360, 0));
        columna.add(crearSubtitulo("Invitar usuarios"), BorderLayout.NORTH);

        listaUsuarios.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(listaUsuarios);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        columna.add(scroll, BorderLayout.CENTER);
        return columna;
    }

    private JPanel crearColumnaDatos() {
        JPanel columna = new JPanel(new GridBagLayout());
        columna.setOpaque(false);
        columna.setPreferredSize(new Dimension(360, 0));

        campoNombre.setFont(new Font("Arial", Font.PLAIN, 20));
        campoNombre.setPreferredSize(new Dimension(300, 50));
        campoNombre.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));

        JLabel ayuda = new JLabel(
                "<html><center>Invita al menos dos usuarios para<br>crear el grupo</center></html>",
                SwingConstants.CENTER);
        ayuda.setFont(new Font("Arial", Font.PLAIN, 14));

        darEstilo(btnCrear, VERDE, 22f);
        btnCrear.setPreferredSize(new Dimension(300, 80));
        btnCrear.addActionListener(e -> crearGrupo());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        columna.add(crearSubtitulo("Nombre del grupo"), c);
        c.insets = new Insets(10, 0, 0, 0);
        columna.add(campoNombre, c);
        columna.add(ayuda, c);
        c.insets = new Insets(50, 0, 0, 0);
        columna.add(btnCrear, c);

        c.weighty = 1;
        columna.add(Box.createGlue(), c);
        return columna;
    }

    private JLabel crearSubtitulo(String texto) {
        JLabel etiqueta = new JLabel(texto, SwingConstants.CENTER);
        etiqueta.setFont(new Font("Arial", Font.BOLD, 22));
        return etiqueta;
    }

    private void darEstilo(JButton boton, Color fondo, float tamano) {
        boton.setBackground(fondo);
        boton.setForeground(Color.BLACK);
        boton.setFont(new Font("Arial", Font.BOLD, 14).deriveFont(tamano));
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
    }

    private void limpiar() {
        invitados.clear();
        botonesInvitar.clear();
        campoNombre.setText("");
        listaUsuarios.removeAll();
        listaUsuarios.revalidate();
        listaUsuarios.repaint();
        estado.setText(" ");
    }

    private void actualizarControles() {
        boolean disponible = conexion != null && !ocupado;
        campoNombre.setEnabled(!ocupado);
        btnCrear.setEnabled(disponible);
        botonesInvitar.forEach(boton -> boton.setEnabled(disponible));
        if (conexion == null) {
            estado.setText("Inicia sesión para invitar usuarios.");
        }
    }

    // Pide la lista de amigos en segundo plano para no congelar la ventana.
    private void cargarAmigos() {
        ConexionChat sesion = conexion;
        if (sesion == null || ocupado) {
            return;
        }
        limpiar();
        ocupado = true;
        actualizarControles();
        estado.setText("Cargando usuarios…");

        new SwingWorker<Respuesta, Void>() {
            @Override
            protected Respuesta doInBackground() throws IOException {
                return sesion.pedir("AMIGOS");
            }

            @Override
            protected void done() {
                ocupado = false;
                // Si la sesión cambió mientras esperábamos, se ignora la respuesta.
                if (sesion == conexion) {
                    try {
                        Respuesta respuesta = get();
                        if (respuesta.correcta()) {
                            mostrarUsuarios(respuesta.filas());
                        } else {
                            estado.setText(respuesta.aviso());
                        }
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException error) {
                        estado.setText("Se perdió la conexión. Vuelve a iniciar sesión.");
                        conexion = null;
                    }
                }
                actualizarControles();
            }
        }.execute();
    }

    private void mostrarUsuarios(List<Fila> usuarios) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        for (Fila usuario : usuarios) {
            JLabel nombre = new JLabel(usuario.titulo());
            // Evita que un nombre con etiquetas se interprete como HTML.
            nombre.putClientProperty("html.disable", Boolean.TRUE);
            nombre.setFont(new Font("Arial", Font.PLAIN, 18));
            nombre.setToolTipText(usuario.detalle());
            nombre.setPreferredSize(new Dimension(200, 50));
            nombre.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK, 2),
                    BorderFactory.createEmptyBorder(0, 6, 0, 6)));

            JButton invitar = new JButton("INVITAR");
            darEstilo(invitar, NARANJA, 14f);
            invitar.setPreferredSize(new Dimension(100, 50));
            invitar.addActionListener(e -> alternarInvitacion(usuario.id(), invitar));
            botonesInvitar.add(invitar);

            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            listaUsuarios.add(nombre, c);
            c.gridx = 1;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            listaUsuarios.add(invitar, c);
            c.gridy++;
        }

        // Fila vacía que mantiene la lista pegada arriba.
        c.gridx = 0;
        c.weighty = 1;
        listaUsuarios.add(Box.createGlue(), c);

        listaUsuarios.revalidate();
        listaUsuarios.repaint();
        estado.setText(usuarios.size() < MIN_INVITADOS
                ? "Necesitas al menos 2 amigos para crear un grupo."
                : "Invitados seleccionados: 0");
    }

    // Un clic invita y otro clic quita la invitación.
    private void alternarInvitacion(int id, JButton boton) {
        if (invitados.remove(id)) {
            boton.setText("INVITAR");
            boton.setBackground(NARANJA);
        } else {
            invitados.add(id);
            boton.setText("QUITAR");
            boton.setBackground(Color.LIGHT_GRAY);
        }
        estado.setText("Invitados seleccionados: " + invitados.size());
    }

    private void crearGrupo() {
        ConexionChat sesion = conexion;
        if (sesion == null || ocupado) {
            return;
        }

        String nombre = campoNombre.getText().strip();
        if (nombre.isEmpty() || nombre.length() > MAX_NOMBRE) {
            JOptionPane.showMessageDialog(this,
                    "Escribe un nombre de 1 a " + MAX_NOMBRE + " caracteres.");
            return;
        }
        if (invitados.size() < MIN_INVITADOS) {
            JOptionPane.showMessageDialog(this,
                    "Invita al menos dos usuarios. El grupo debe tener mínimo tres personas.");
            return;
        }

        List<Integer> seleccion = List.copyOf(invitados);
        ocupado = true;
        actualizarControles();
        estado.setText("Creando grupo…");

        new SwingWorker<Resultado, Void>() {
            @Override
            protected Resultado doInBackground() throws IOException {
                return enviarGrupo(sesion, nombre, seleccion);
            }

            @Override
            protected void done() {
                ocupado = false;
                actualizarControles();
                try {
                    Resultado resultado = get();
                    estado.setText(resultado.aviso());
                    JOptionPane.showMessageDialog(CrearGrupo.this, resultado.aviso());
                    if (resultado.creado()) {
                        limpiar();
                        alCrearGrupo.run();
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    conexion = null;
                    actualizarControles();
                    JOptionPane.showMessageDialog(CrearGrupo.this,
                            "Se perdió la conexión. Al volver a iniciar sesión, revisa si el "
                            + "grupo quedó incompleto.");
                }
            }
        }.execute();
    }

    /**
     * El servidor crea el grupo y agrega miembros en peticiones separadas.
     * Como CREAR_GRUPO no devuelve el ID, se compara la lista antes y después.
     */
    private static Resultado enviarGrupo(ConexionChat sesion, String nombre,
            List<Integer> seleccion) throws IOException {
        Respuesta antes = sesion.pedir("GRUPOS");
        if (!antes.correcta()) {
            return new Resultado(false, antes.aviso());
        }
        Set<Integer> anteriores = new HashSet<>();
        antes.filas().forEach(fila -> anteriores.add(fila.id()));

        Respuesta creacion = sesion.pedir("CREAR_GRUPO", nombre, "");
        if (!creacion.correcta()) {
            return new Resultado(false, creacion.aviso());
        }

        Respuesta despues = sesion.pedir("GRUPOS");
        int grupo = despues.filas().stream()
                .filter(f -> !anteriores.contains(f.id()) && f.titulo().equals(nombre))
                .mapToInt(Fila::id)
                .max()
                .orElse(-1);
        if (grupo == -1) {
            return new Resultado(false,
                    "El grupo se creó, pero no se pudo identificar para agregar a los miembros.");
        }

        String idGrupo = String.valueOf(grupo);
        int agregados = 0;
        String fallo = "";
        for (int usuario : seleccion) {
            Respuesta r = sesion.pedir("AGREGAR_MIEMBRO", idGrupo, String.valueOf(usuario));
            if (r.correcta()) {
                agregados++;
            } else {
                fallo = r.aviso();
            }
        }

        // Si no se llegó a tres personas, se deshace el grupo.
        if (agregados < MIN_INVITADOS) {
            Respuesta borrado = sesion.pedir("ELIMINAR_GRUPO", idGrupo);
            return new Resultado(false, "No se pudo reunir a tres personas. " + fallo
                    + (borrado.correcta() ? "" : " Además, no se pudo eliminar el grupo incompleto."));
        }
        if (agregados < seleccion.size()) {
            return new Resultado(true, "Grupo creado, pero no se pudo agregar a "
                    + (seleccion.size() - agregados) + " usuario(s).");
        }
        return new Resultado(true, "Grupo creado correctamente.");
    }
}
