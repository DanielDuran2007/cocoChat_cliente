package vistas;

import cocochat_cliente.ConexionChat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import protocolo.Protocolo.Fila;
import protocolo.Protocolo.Respuesta;

/**
 * Copia este archivo como src/vistas/PanelPrincipal.java.
 *
 * EN CocoChat.java, dentro de mostrarVentana():
 *
 * 1. Crea una variable y registra el panel en el CardLayout:
 *
 *      PanelPrincipal panelPrincipal = new PanelPrincipal();
 *      contenido.add(panelPrincipal, "PANEL_PRINCIPAL");
 *
 *    Estas líneas sustituyen contenido.add(new PanelPrincipal(), "PANEL_PRINCIPAL").
 *
 * 2. Configura la navegación después de crear los paneles. El primer argumento
 *    muestra una tarjeta. El segundo recibe los datos necesarios para abrir Chat.
 *
 *      panelPrincipal.configurarNavegacion(
 *          pantalla -> paginas.show(contenido, pantalla),
 *          (destino, esGrupo) -> {
 *              // Llama aquí al método que implemente tu compañero en el panel Chat.
 *              // Pásale destino.id(), destino.titulo() y esGrupo.
 *              paginas.show(contenido, "CHAT");
 *          }
 *      );
 *
 * 3. Tras un LOGIN exitoso, en el hilo de Swing:
 *
 *      panelPrincipal.iniciarSesion(conexionAutenticada);
 *      paginas.show(contenido, "PANEL_PRINCIPAL");
 *
 *    Usa el mismo socket que recibió la respuesta correcta a LOGIN.
 *    Si Registrarse abre el panel, primero debe registrarse e iniciar sesión.
 *
 * 4. Al cerrar sesión, llama a panelPrincipal.finalizarSesion().
 *    La clase que administra la sesión debe cerrar su propio ConexionChat.
 */
public class PanelPrincipal extends JPanel {

    private static final Color NARANJA = new Color(255, 146, 77);
    private static final Color VERDE = new Color(128, 219, 90);
    private static final Color AMARILLO = new Color(255, 221, 89);
    private static final Color CIAN = new Color(92, 225, 230);

    // La fila lleva el nombre visible y el ID que necesita el servidor.
    private final DefaultListModel<Fila> modeloActivos = new DefaultListModel<>();
    private final DefaultListModel<Fila> modeloAmigos = new DefaultListModel<>();
    private final DefaultListModel<Fila> modeloGrupos = new DefaultListModel<>();

    private final JList<Fila> listaActivos = new JList<>(modeloActivos);
    private final JList<Fila> listaAmigos = new JList<>(modeloAmigos);
    private final JList<Fila> listaGrupos = new JList<>(modeloGrupos);

    private final JLabel estado = new JLabel("Inicia sesión para cargar las listas.");
    private final JButton btnActualizar = new JButton("Actualizar");
    private ConexionChat conexion;
    private SwingWorker<Listas, Void> carga;
    private Consumer<String> mostrarPantalla;
    private BiConsumer<Fila, Boolean> mostrarChat;

    // Agrupa las respuestas para entregarlas juntas al hilo de Swing.
    private record Listas(Respuesta activos, Respuesta amigos, Respuesta grupos) {}

    public PanelPrincipal() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titulo = new JLabel("Panel principal");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 26));
        add(titulo, BorderLayout.NORTH);

        JPanel columnas = new JPanel(new GridLayout(1, 3, 10, 0));
        columnas.add(crearColumna("Usuarios activos", listaActivos, NARANJA));
        columnas.add(crearColumna("Amigos", listaAmigos, VERDE));
        columnas.add(crearColumna("Grupos", listaGrupos, AMARILLO));
        add(columnas, BorderLayout.CENTER);

        JPanel botones = new JPanel(new GridLayout(4, 1, 0, 10));
        botones.setPreferredSize(new Dimension(200, 0));

        JButton btnSolicitudes = crearBoton("Ver solicitudes de amistad / grupo", AMARILLO);
        JButton btnEnviarSolicitud = crearBoton("Enviar solicitud de amistad", VERDE);
        JButton btnCrearGrupo = crearBoton("Crear grupo", NARANJA);
        JButton btnVisor = crearBoton("Visor del todo", CIAN);

        btnSolicitudes.addActionListener(e -> abrirPantalla("SOLICITUDES"));
        btnEnviarSolicitud.addActionListener(e -> abrirPantalla("ENVIAR_SOLICITUD"));
        btnCrearGrupo.addActionListener(e -> abrirPantalla("CREAR_GRUPO"));
        btnVisor.addActionListener(e -> abrirPantalla("VISOR_GENERAL"));

        botones.add(btnSolicitudes);
        botones.add(btnEnviarSolicitud);
        botones.add(btnCrearGrupo);
        botones.add(btnVisor);
        add(botones, BorderLayout.EAST);

        activarClic(listaActivos, false);
        activarClic(listaAmigos, false);
        activarClic(listaGrupos, true);

        JPanel pie = new JPanel(new BorderLayout(8, 0));
        pie.add(estado, BorderLayout.CENTER);
        pie.add(btnActualizar, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);
        btnActualizar.setEnabled(false);
        btnActualizar.addActionListener(e -> actualizarListas());
    }

    /** Conecta los botones con el CardLayout y entrega al Chat la fila elegida. */
    public void configurarNavegacion(Consumer<String> mostrarPantalla,
            BiConsumer<Fila, Boolean> mostrarChat) {
        this.mostrarPantalla = Objects.requireNonNull(mostrarPantalla);
        this.mostrarChat = Objects.requireNonNull(mostrarChat);
    }

    /** Se llama desde Swing después de recibir una respuesta exitosa a LOGIN. */
    public void iniciarSesion(ConexionChat conexionAutenticada) {
        Objects.requireNonNull(conexionAutenticada, "Hace falta una conexión autenticada.");
        if (conexion != conexionAutenticada) {
            finalizarSesion();
            conexion = conexionAutenticada;
        }
        actualizarListas();
    }

    /** Limpia las listas al salir. El dueño de la sesión cierra el socket. */
    public void finalizarSesion() {
        conexion = null;
        if (carga != null) {
            SwingWorker<Listas, Void> anterior = carga;
            carga = null;
            anterior.cancel(true);
        }
        modeloActivos.clear();
        modeloAmigos.clear();
        modeloGrupos.clear();
        btnActualizar.setEnabled(false);
        estado.setText("Inicia sesión para cargar las listas.");
    }

    /** Úsalo también al volver de una pantalla donde hayan cambiado las listas. */
    public void actualizarListas() {
        ConexionChat sesion = conexion;
        if (sesion == null || carga != null) {
            return;
        }

        btnActualizar.setEnabled(false);
        estado.setText("Cargando listas…");

        carga = new SwingWorker<Listas, Void>() {
            @Override
            protected Listas doInBackground() throws IOException {
                // Reutilizamos el socket autenticado y esperamos una respuesta
                // por cada petición. Esta espera ocurre fuera del hilo de Swing.
                return new Listas(sesion.pedir("EN_LINEA"), sesion.pedir("AMIGOS"),
                        sesion.pedir("GRUPOS"));
            }

            @Override
            protected void done() {
                // Una respuesta de la sesión anterior no debe pintar este panel.
                if (carga != this) {
                    return;
                }

                carga = null;
                btnActualizar.setEnabled(true);
                try {
                    Listas listas = get();
                    StringBuilder errores = new StringBuilder();
                    aplicar(modeloActivos, "Usuarios activos", listas.activos(), errores);
                    aplicar(modeloAmigos, "Amigos", listas.amigos(), errores);
                    aplicar(modeloGrupos, "Grupos", listas.grupos(), errores);
                    estado.setText(errores.length() == 0 ? "Listas actualizadas." : errores.toString());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    estado.setText("Se interrumpió la actualización.");
                } catch (ExecutionException error) {
                    finalizarSesion();
                    estado.setText("Se perdió la conexión. Vuelve a iniciar sesión.");
                }
            }
        };
        carga.execute();
    }

    private void aplicar(DefaultListModel<Fila> modelo, String nombre, Respuesta respuesta,
            StringBuilder errores) {
        if (respuesta.correcta()) {
            llenar(modelo, respuesta.filas());
        } else {
            if (errores.length() > 0) {
                errores.append(" | ");
            }
            errores.append(nombre).append(": ").append(respuesta.aviso());
        }
    }

    // Se llaman en el hilo de Swing si otra vista ya obtuvo las filas.
    public void setUsuariosActivos(List<Fila> usuarios) {
        llenar(modeloActivos, usuarios);
    }

    public void setAmigos(List<Fila> amigos) {
        llenar(modeloAmigos, amigos);
    }

    public void setGrupos(List<Fila> grupos) {
        llenar(modeloGrupos, grupos);
    }

    private void llenar(DefaultListModel<Fila> modelo, List<Fila> datos) {
        modelo.clear();
        modelo.addAll(datos);
    }

    private JPanel crearColumna(String texto, JList<Fila> lista, Color color) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));

        JLabel etiqueta = new JLabel(texto, SwingConstants.CENTER);
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(etiqueta, BorderLayout.NORTH);

        lista.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lista.setFixedCellHeight(36);
        lista.setVisibleRowCount(8);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.putClientProperty("html.disable", Boolean.TRUE);
        lista.setCellRenderer((origen, valor, indice, seleccionado, foco) -> {
            JLabel fila = (JLabel) renderer.getListCellRendererComponent(
                    origen, valor == null ? "" : valor.titulo(), indice, seleccionado, foco);
            fila.setForeground(Color.BLACK);
            fila.setBackground(seleccionado ? color.darker() : color);
            fila.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK),
                    BorderFactory.createEmptyBorder(0, 6, 0, 6)));
            return fila;
        });

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JButton crearBoton(String texto, Color color) {
        JButton boton = new JButton("<html><center>" + texto + "</center></html>");
        boton.setBackground(color);
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        boton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return boton;
    }

    private void activarClic(JList<Fila> lista, boolean esGrupo) {
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                if (!SwingUtilities.isLeftMouseButton(evento)) {
                    return;
                }

                int indice = lista.locationToIndex(evento.getPoint());
                if (indice < 0) {
                    return;
                }

                // locationToIndex puede dar la fila más cercana al espacio vacío.
                Rectangle celda = lista.getCellBounds(indice, indice);
                if (celda != null && celda.contains(evento.getPoint())) {
                    abrirChat(lista.getModel().getElementAt(indice), esGrupo);
                }
            }
        });
    }

    private void abrirPantalla(String pantalla) {
        if (mostrarPantalla == null) {
            estado.setText("Configura la navegación del panel en CocoChat.java.");
            return;
        }
        mostrarPantalla.accept(pantalla);
    }

    private void abrirChat(Fila destino, boolean esGrupo) {
        if (mostrarChat == null) {
            estado.setText("Configura la navegación hacia Chat en CocoChat.java.");
            return;
        }
        mostrarChat.accept(destino, esGrupo);
    }
}
