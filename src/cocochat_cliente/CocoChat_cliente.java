
package cocochat_cliente;

//importación de herramientas de diseño:
import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
//importacion de vistas
import vistas.Chat;
import vistas.CrearGrupo;
import vistas.EnviarSolicitud;
import vistas.Inicio;
import vistas.LogIn;
import vistas.PanelPrincipal;
import vistas.Registro;
import vistas.Solicitudes;
import vistas.VisorGeneral;

public class CocoChat_cliente {
    
        public static void main(String[] args) {
        
        // Swing crea y modifica sus ventanas en el hilo de eventos.
        SwingUtilities.invokeLater(CocoChat_cliente::mostrarVentana);
    }

    private static void mostrarVentana() {
        //Crea la vetnaan principal titulada COCO-CHAT EQUIPO 2
        JFrame ventana = new JFrame("COCO-CHAT EQUIPO 2");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //cierra al terminar el programa

        // CardLayout apila los paneles y muestra uno a la vez por su nombre.
        CardLayout paginas = new CardLayout();
        JPanel contenido = new JPanel(paginas);
        
        //se crean los ocho paneles:
        contenido.add(new Inicio(), "INICIO");
        contenido.add(new LogIn(), "LOGIN");
        contenido.add(new Registro(), "REGISTRO");
        contenido.add(new PanelPrincipal(), "PANEL_PRINCIPAL");
        contenido.add(new Solicitudes(), "SOLICITUDES");
        contenido.add(new Chat(), "CHAT");
        contenido.add(new EnviarSolicitud(), "ENVIAR_SOLICITUD");
        contenido.add(new CrearGrupo(), "CREAR_GRUPO");
        contenido.add(new VisorGeneral(), "VISOR_GENERAL");

        // Para una acción futura, por ejemplo, usen paginas.show(contenido, "LOGIN").
        // Por ahora solo se muestra Inicio. Ningún botón cambia de pantalla.
        paginas.show(contenido, "INICIO");

        ventana.setContentPane(contenido);
        ventana.pack();
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
    }
    
}
