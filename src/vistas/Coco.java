package vistas;

import cocochat_cliente.ConexionChat;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import protocolo.Protocolo;
import protocolo.Protocolo.Fila;
import protocolo.Protocolo.Respuesta;

/**
 * Ventana principal. Izquierda: contactos y grupos. Centro: conversación.
 * Derecha: personas en línea. Todos los componentes usan los estilos de Swing.
 */
public final class Coco extends JFrame {
    private final ConexionChat conexion;
    private final String ip;
    private final int puerto;
    private final Chat chat = new Chat(this::enviar);
    private final JLabel aviso = new JLabel(" ");
    private final JTextField busqueda = new JTextField(15);
    private final JTabbedPane listas = new JTabbedPane();
    private final JList<Fila> amigos = nuevaLista();
    private final JList<Fila> solicitudes = nuevaLista();
    private final JList<Fila> grupos = nuevaLista();
    private final JList<Fila> encontrados = nuevaLista();
    private final JList<Fila> enLinea = nuevaLista();
    private final Timer reloj;
    private Conversacion actual;
    private boolean ocupada;
    private boolean cerrada;
    private int ciclos;

    // El ID puede existir en ambas tablas, por eso guardamos también el tipo.
    private record Conversacion(boolean grupo, Fila destino) {
        String id() { return String.valueOf(destino.id()); }
        String sufijo() { return grupo ? "_GRUPO" : "_PRIVADO"; }
    }

    private record Actualizacion(Respuesta historial, Respuesta lateral, Respuesta presencia) {}

    public Coco(ConexionChat conexion, Fila usuario, String ip, int puerto) {
        super("CocoChat");
        this.conexion = conexion;
        this.ip = ip;
        this.puerto = puerto;

        setLayout(new BorderLayout(8, 8));
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.add(new JLabel("Sesión de " + usuario.titulo()), BorderLayout.WEST);
        JPanel cuenta = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cuenta.add(boton("Cambiar contraseña", this::cambiarContrasena));
        cuenta.add(boton("Cerrar sesión", () -> cerrar(true)));
        cabecera.add(cuenta, BorderLayout.EAST);

        listas.addTab("Amigos", panelLista(amigos,
                boton("Abrir chat", () -> abrir(amigos, false)),
                boton("Eliminar", () -> eliminarAmigo())));
        listas.addTab("Solicitudes", panelLista(solicitudes,
                boton("Aceptar", () -> responderSolicitud("ACEPTAR")),
                boton("Rechazar", () -> responderSolicitud("RECHAZAR"))));
        listas.addTab("Grupos", panelLista(grupos,
                boton("Abrir grupo", () -> abrir(grupos, true)),
                boton("Crear grupo", this::crearGrupo),
                boton("Administrar", this::administrarGrupo)));
        listas.addTab("Buscar", panelBusqueda());

        add(cabecera, BorderLayout.NORTH);
        add(listas, BorderLayout.WEST);
        add(chat, BorderLayout.CENTER);
        JPanel derecha = panelLista(enLinea, boton("Abrir chat", () -> abrir(enLinea, false)));
        derecha.add(new JLabel("Usuarios en línea"), BorderLayout.NORTH);
        add(derecha, BorderLayout.EAST);

        JPanel pie = new JPanel(new BorderLayout());
        pie.add(aviso, BorderLayout.CENTER);
        pie.add(boton("Actualizar", () -> actualizar(true)), BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) { cerrar(false); }
        });

        pack();
        setLocationRelativeTo(null);

        // Cada tres segundos el cliente pide el historial al servidor por el socket.
        // Consultas de contactos, grupos y presencia se hacen cada tercer ciclo.
        reloj = new Timer(3000, e -> actualizar(false));
        reloj.start();
        listas.addChangeListener(e -> actualizar(true));
        actualizar(true);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(evento -> accion.run());
        return boton;
    }

    private static JList<Fila> nuevaLista() {
        JList<Fila> lista = new JList<>();
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setVisibleRowCount(9);
        lista.setPrototypeCellValue(new Fila(0, "Un nombre de referencia", ""));
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> origen, Object valor,
                    int fila, boolean seleccionada, boolean foco) {
                // Los nombres son texto que escribieron otros usuarios.
                putClientProperty("html.disable", Boolean.TRUE);
                return super.getListCellRendererComponent(origen, valor, fila, seleccionada, foco);
            }
        });
        return lista;
    }

    private JPanel panelLista(JList<Fila> lista, JButton... acciones) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JScrollPane(lista), BorderLayout.CENTER);
        JPanel pie = new JPanel(new GridLayout(0, 2, 4, 4));
        for (JButton accion : acciones) pie.add(accion);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelBusqueda() {
        JPanel panel = panelLista(encontrados,
                boton("Enviar solicitud", () -> {
                    Fila destino = seleccion(encontrados);
                    if (destino != null) solicitar("SOLICITAR", respuesta -> {}, id(destino));
                }),
                boton("Abrir chat", () -> abrir(encontrados, false)));
        JPanel formulario = new JPanel(new BorderLayout());
        formulario.add(busqueda, BorderLayout.CENTER);
        formulario.add(boton("Buscar", this::buscar), BorderLayout.EAST);
        busqueda.addActionListener(e -> buscar());
        panel.add(formulario, BorderLayout.NORTH);
        return panel;
    }

    private void buscar() {
        if (busqueda.getText().isBlank()) {
            aviso.setText("Escribe el nombre o correo de la persona.");
            return;
        }
        solicitar("BUSCAR", respuesta -> {
            reemplazar(encontrados, respuesta.filas());
            aviso.setText(respuesta.filas().size() + " resultado(s).");
        }, busqueda.getText());
    }

    private void abrir(JList<Fila> lista, boolean grupo) {
        Fila seleccionada = seleccion(lista);
        if (seleccionada == null) return;
        actual = new Conversacion(grupo, seleccionada);
        chat.abrir((grupo ? "Grupo: " : "Usuario: ") + seleccionada.titulo());
        chat.activarEnvio(true);
        actualizar(false);
    }

    private void enviar(String texto) {
        Conversacion destino = actual;
        if (destino == null || texto.isBlank()) return;
        if (texto.length() > Protocolo.MAX_MENSAJE) {
            aviso.setText("El mensaje supera los " + Protocolo.MAX_MENSAJE + " caracteres.");
            return;
        }
        solicitar("ENVIAR" + destino.sufijo(), respuesta -> {
            if (destino.equals(actual)) chat.limpiarSiCoincide(texto);
            actualizar(false);
        }, destino.id(), texto);
    }

    private void responderSolicitud(String accion) {
        Fila persona = seleccion(solicitudes);
        if (persona != null) solicitar(accion, respuesta -> actualizar(true), id(persona));
    }

    private void eliminarAmigo() {
        Fila persona = seleccion(amigos);
        if (persona != null && confirmar("¿Eliminar esta amistad?")) {
            solicitar("ELIMINAR_AMIGO", respuesta -> actualizar(true), id(persona));
        }
    }

    private void crearGrupo() {
        JTextField nombre = new JTextField(20);
        JTextField descripcion = new JTextField(20);
        JPanel datos = new JPanel(new GridLayout(0, 2, 8, 8));
        datos.add(new JLabel("Nombre"));
        datos.add(nombre);
        datos.add(new JLabel("Descripción"));
        datos.add(descripcion);
        if (JOptionPane.showConfirmDialog(this, datos, "Crear grupo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            solicitar("CREAR_GRUPO", respuesta -> actualizar(true), nombre.getText(), descripcion.getText());
        }
    }

    private void administrarGrupo() {
        Fila grupo = seleccion(grupos);
        if (grupo == null) return;
        String[] opciones = {"Ver miembros", "Agregar amigo", "Salir del grupo", "Eliminar grupo"};
        int opcion = JOptionPane.showOptionDialog(this, "Elige una acción.", "Administrar grupo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        switch (opcion) {
            case 0 -> solicitar("MIEMBROS", respuesta -> mostrarMiembros(respuesta.filas()), id(grupo));
            case 1 -> solicitar("AMIGOS", respuesta -> agregarMiembro(grupo, respuesta.filas()));
            case 2 -> cambiarMembresia("SALIR_GRUPO", grupo, "¿Salir del grupo?");
            case 3 -> cambiarMembresia("ELIMINAR_GRUPO", grupo, "¿Eliminar el grupo y sus mensajes?");
            default -> { /* Se cerró el diálogo. */ }
        }
    }

    private void mostrarMiembros(List<Fila> personas) {
        JTextArea texto = new JTextArea(12, 28);
        texto.setEditable(false);
        personas.forEach(p -> texto.append(p.titulo() + " | " + p.detalle() + "\n"));
        JOptionPane.showMessageDialog(this, new JScrollPane(texto), "Miembros", JOptionPane.PLAIN_MESSAGE);
    }

    private void agregarMiembro(Fila grupo, List<Fila> personas) {
        if (personas.isEmpty()) {
            aviso.setText("Primero agrega a una persona a tu lista de amigos.");
            return;
        }
        JComboBox<Fila> opciones = new JComboBox<>(personas.toArray(Fila[]::new));
        opciones.setRenderer(amigos.getCellRenderer());
        if (JOptionPane.showConfirmDialog(this, opciones, "Selecciona un amigo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION
                && opciones.getSelectedItem() instanceof Fila persona) {
            solicitar("AGREGAR_MIEMBRO", respuesta -> {}, id(grupo), id(persona));
        }
    }

    private void cambiarMembresia(String accion, Fila grupo, String pregunta) {
        if (confirmar(pregunta)) {
            solicitar(accion, respuesta -> {
                if (actual != null && actual.grupo() && actual.destino().id() == grupo.id()) {
                    actual = null;
                    chat.abrir("Selecciona un usuario o un grupo.");
                    chat.activarEnvio(false);
                }
                actualizar(true);
            }, id(grupo));
        }
    }

    private void cambiarContrasena() {
        JPasswordField anterior = new JPasswordField(20);
        JPasswordField nueva = new JPasswordField(20);
        JPanel campos = new JPanel(new GridLayout(0, 2, 8, 8));
        campos.add(new JLabel("Contraseña actual"));
        campos.add(anterior);
        campos.add(new JLabel("Nueva contraseña"));
        campos.add(nueva);
        if (JOptionPane.showConfirmDialog(this, campos, "Cambiar contraseña",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            solicitar("CAMBIAR_CLAVE", respuesta -> {},
                    new String(anterior.getPassword()), new String(nueva.getPassword()));
        }
    }

    private boolean confirmar(String pregunta) {
        return JOptionPane.showConfirmDialog(this, pregunta, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private Fila seleccion(JList<Fila> lista) {
        Fila seleccionada = lista.getSelectedValue();
        if (seleccionada == null) aviso.setText("Selecciona un elemento de la lista.");
        return seleccionada;
    }

    private String id(Fila fila) { return String.valueOf(fila.id()); }

    private void actualizar(boolean completo) {
        if (ocupada || cerrada) return;
        Conversacion destino = actual;
        int pestaña = listas.getSelectedIndex();
        boolean recargarListas = completo || ++ciclos % 3 == 0;
        String accion = switch (pestaña) {
            case 0 -> "AMIGOS";
            case 1 -> "SOLICITUDES";
            case 2 -> "GRUPOS";
            default -> null;
        };
        if (destino == null && !recargarListas) return;
        ejecutar(() -> {
            Respuesta historial = destino == null ? null
                    : conexion.pedir("HISTORIAL" + destino.sufijo(), destino.id());
            Respuesta lateral = recargarListas && accion != null ? conexion.pedir(accion) : null;
            Respuesta presencia = recargarListas ? conexion.pedir("EN_LINEA") : null;
            return new Actualizacion(historial, lateral, presencia);
        }, resultado -> {
            if (resultado.historial() != null && destino.equals(actual)
                    && mostrarErrorSiExiste(resultado.historial())) {
                chat.mostrar(resultado.historial().filas());
            }
            if (resultado.lateral() != null && mostrarErrorSiExiste(resultado.lateral())) {
                JList<Fila> lista = switch (pestaña) {
                    case 0 -> amigos;
                    case 1 -> solicitudes;
                    default -> grupos;
                };
                reemplazar(lista, resultado.lateral().filas());
            }
            if (resultado.presencia() != null && mostrarErrorSiExiste(resultado.presencia())) {
                reemplazar(enLinea, resultado.presencia().filas());
            }
        });
    }

    private void reemplazar(JList<Fila> lista, List<Fila> filas) {
        // Al refrescar se conserva la selección por ID, aunque cambie su posición.
        Fila seleccionada = lista.getSelectedValue();
        lista.setListData(filas.toArray(Fila[]::new));
        if (seleccionada == null) return;
        for (int i = 0; i < filas.size(); i++) {
            if (filas.get(i).id() == seleccionada.id()) {
                lista.setSelectedIndex(i);
                break;
            }
        }
    }

    private boolean mostrarErrorSiExiste(Respuesta respuesta) {
        if (!respuesta.correcta()) aviso.setText(respuesta.aviso());
        return respuesta.correcta();
    }

    private void solicitar(String accion, Consumer<Respuesta> despues, String... campos) {
        if (ocupada) {
            aviso.setText("Espera un momento y vuelve a intentar.");
            return;
        }
        ejecutar(() -> conexion.pedir(accion, campos), respuesta -> {
            aviso.setText(respuesta.aviso());
            if (respuesta.correcta()) despues.accept(respuesta);
        });
    }

    private <T> void ejecutar(Callable<T> tarea, Consumer<T> despues) {
        if (cerrada) return;
        ocupada = true;
        // Capturamos los datos de los campos antes de llamar a ejecutar. El
        // worker solo hace trabajo de red y done es quien modifica la ventana.
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception { return tarea.call(); }

            @Override
            protected void done() {
                ocupada = false;
                if (cerrada) return;
                try {
                    despues.accept(get());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    reloj.stop();
                    conexion.close();
                    chat.activarEnvio(false);
                    JOptionPane.showMessageDialog(Coco.this,
                            "Se interrumpió la conexión. Vuelve a iniciar sesión.",
                            "Conexión", JOptionPane.ERROR_MESSAGE);
                    cerrar(true);
                }
            }
        }.execute();
    }

    private void cerrar(boolean volverAlLogin) {
        if (cerrada) return;
        cerrada = true;
        reloj.stop();
        // Cerrar el socket también cierra la sesión del lado del servidor.
        conexion.close();
        dispose();
        if (volverAlLogin) new LogIn(ip, puerto).setVisible(true);
    }
}
