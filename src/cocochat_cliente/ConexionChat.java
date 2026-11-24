package cocochat_cliente;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import protocolo.Protocolo;
import protocolo.Protocolo.Respuesta;

/** El cliente conoce la dirección del servidor Java y conversa con él por TCP. */
public final class ConexionChat implements AutoCloseable {
    private final Socket socket;
    private final DataInputStream entrada;
    private final DataOutputStream salida;

    public ConexionChat(String ip, int puerto) throws IOException {
        socket = new Socket();
        try {
            // El primer tiempo es para conectar. El segundo es para recibir respuesta.
            socket.connect(new InetSocketAddress(ip, puerto), 5000);
            socket.setSoTimeout(15000);
            socket.setTcpNoDelay(true);
            entrada = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            salida = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException error) {
            close();
            throw error;
        }
    }

    // Se envía una petición y se lee su respuesta antes de iniciar la siguiente.
    // Llama a este método en segundo plano para que Swing siga respondiendo.
    public synchronized Respuesta pedir(String accion, String... campos) throws IOException {
        try {
            Protocolo.escribirPeticion(salida, accion, campos);
            return Protocolo.leerRespuesta(entrada);
        } catch (IOException error) {
            close();
            throw error;
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignorado) {
            // El programa también puede llegar aquí cuando el servidor ya se desconectó.
        }
    }
}
