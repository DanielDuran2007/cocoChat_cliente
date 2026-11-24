package cocochat_cliente;


import java.io.*;
import java.net.InetSocketAddress; //representa la ip y puerto de destino
import java.net.Socket;
import protocolo.Protocolo;
import protocolo.Protocolo.Respuesta;

//Aqui se envian peticiones y reciben respuestas
// El protocolo de comunicación es TCP. 
public final class ConexionChat implements AutoCloseable {
    
    private final Socket socket; //socket TCP
    private final DataInputStream entrada; 
    private final DataOutputStream salida;

    //CONSTRUCTOR
    public ConexionChat(String ip, int puerto) throws IOException {
        socket = new Socket();
        try {
            // El primer tiempo de 5 segundos es para conectar. El segundo  de 15 es para recibir respuesta.
            socket.connect(new InetSocketAddress(ip, puerto), 5000);
            socket.setSoTimeout(15000);
            socket.setTcpNoDelay(true);
            
            //escribe la petición al servidor
            entrada = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            //lee la respuesta del servidor
            salida = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        } catch (IOException error) {
            close(); //cierra el socket en caso de error
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

    //se cierra el socket
    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignorado) {
            // El programa también puede llegar aquí cuando el servidor ya se desconectó.
        }
    }
}
