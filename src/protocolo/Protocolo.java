package protocolo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * El cliente y el servidor tienen la misma copia de esta clase.
 * Es el acuerdo sobre qué datos viajan y en qué orden se leen.
 * DataInputStream/DataOutputStream permiten enviar texto y números con Java estándar.
 */
public final class Protocolo {
    public static final int PUERTO = 5050;
    public static final String VERSION = "COCOCHAT-1";
    public static final int MAX_MENSAJE = 1000;
    public static final int MAX_FILAS = 500;
    private static final int MAX_CAMPOS = 6;

    private Protocolo() {}

    // Una fila puede representar a un usuario, un grupo o un mensaje.
    // Por ejemplo, en un usuario id es su identificador, titulo es su nombre
    // y detalle es su correo. La vista nunca recibe las contraseñas almacenadas.
    public record Fila(int id, String titulo, String detalle) {
        @Override
        public String toString() {
            return titulo;
        }
    }

    public record Peticion(String accion, List<String> campos) {
        public String campo(int posicion) {
            return campos.get(posicion);
        }

        public int id(int posicion) {
            int id = Integer.parseInt(campo(posicion));
            if (id < 1) throw new IllegalArgumentException("Identificador inválido.");
            return id;
        }
    }

    public record Respuesta(boolean correcta, String aviso, List<Fila> filas) {
        public static Respuesta ok(String aviso, List<Fila> filas) {
            return new Respuesta(true, aviso, filas);
        }

        public static Respuesta ok(String aviso) {
            return ok(aviso, List.of());
        }

        public static Respuesta error(String aviso) {
            return new Respuesta(false, aviso, List.of());
        }
    }

    public static void escribirPeticion(DataOutputStream salida, String accion, String... campos)
            throws IOException {
        salida.writeUTF(VERSION);
        salida.writeUTF(accion);
        salida.writeInt(campos.length);
        for (String campo : campos) salida.writeUTF(campo);
        salida.flush();
    }

    public static Peticion leerPeticion(DataInputStream entrada) throws IOException {
        if (!VERSION.equals(entrada.readUTF())) throw new IOException("Versión incompatible.");
        String accion = entrada.readUTF();
        int cantidad = entrada.readInt();
        if (cantidad < 0 || cantidad > MAX_CAMPOS) throw new IOException("Petición inválida.");
        List<String> campos = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) campos.add(entrada.readUTF());
        return new Peticion(accion, campos);
    }

    public static void escribirRespuesta(DataOutputStream salida, Respuesta respuesta)
            throws IOException {
        salida.writeBoolean(respuesta.correcta());
        salida.writeUTF(respuesta.aviso());
        salida.writeInt(respuesta.filas().size());
        for (Fila fila : respuesta.filas()) {
            salida.writeInt(fila.id());
            salida.writeUTF(fila.titulo());
            salida.writeUTF(fila.detalle());
        }
        salida.flush();
    }

    public static Respuesta leerRespuesta(DataInputStream entrada) throws IOException {
        boolean correcta = entrada.readBoolean();
        String aviso = entrada.readUTF();
        int cantidad = entrada.readInt();
        
        if (cantidad < 0 || cantidad > MAX_FILAS) throw new IOException("Respuesta inválida.");
        List<Fila> filas = new ArrayList<>(cantidad);
        
        for (int i = 0; i < cantidad; i++) {
            filas.add(new Fila(entrada.readInt(), entrada.readUTF(), entrada.readUTF()));
        }
        return new Respuesta(correcta, aviso, filas);
    }
}
