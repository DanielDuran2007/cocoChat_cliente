
package cocochat_cliente;
import javax.swing.SwingUtilities;
import protocolo.Protocolo;
import vistas.LogIn;

public class CocoChat_cliente {
    //esta es la parte de daniel
    //respeta mi parte bro
    public static void main(String[] args) {
        // localhost sirve para probar en la computadora del servidor.
        // En otra PC, escribe la IP de la computadora del servidor
        SwingUtilities.invokeLater(() -> new LogIn("127.0.0.1", Protocolo.PUERTO).setVisible(true));
    }
    
}
