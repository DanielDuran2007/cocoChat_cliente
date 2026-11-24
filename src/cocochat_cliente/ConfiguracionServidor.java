package cocochat_cliente;

import java.io.IOException;
import protocolo.Protocolo;

// Aqui se concentran los datos que necesita el cliente para abrir el socket
public final class ConfiguracionServidor {

    // Cambia esta IP si el servidor Java se ejecuta en otra computadora.
    // 127.0.0.1 apunta a la misma computadora donde corre el cliente.
    public static final String IP_SERVIDOR = "127.0.0.1";

    // Protocolo.PUERTO vale 5050 y debe coincidir con el puerto del servidor Java.
    public static final int PUERTO_SERVIDOR = Protocolo.PUERTO;

    private ConfiguracionServidor() {}

    /**
     * ConexionChat llama al servidor y devuelve la conexión abierta. También atrapa errores
     * Cuando implementen Login o Registro, llamen a este método en segundo
     * plano con SwingWorker. La conexión puede tardar varios segundos.
     * Tras LOGIN exitoso, conserven ese mismo objeto para toda la sesión.
     */
    public static ConexionChat conectar() throws IOException {
        return new ConexionChat(IP_SERVIDOR, PUERTO_SERVIDOR);
    }
}
