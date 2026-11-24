package protocolo;

import java.io.DataInputStream; //permite leer datos de un flujo de bytes como textos, enteros y booleanos
import java.io.DataOutputStream; //permite escribir esos mismos tipos de datos en un flujo de byes
import java.io.IOException;
import java.util.ArrayList; //permite construir una lista agregango elementos uno por uno conforme llegan
import java.util.List; //tipo que el archivo usa para recibir y devolver listas

/**
 * El cliente y el servidor tienen la misma copia de esta clase.
 * Es el acuerdo sobre qué datos viajan y en qué orden se leen.
 * DataInputStream/DataOutputStream permiten enviar texto y números con Java estándar.
 */
public final class Protocolo {
    //especificaciones:
    public static final int PUERTO = 5050; //puerto predeterminado
    public static final String VERSION = "COCOCHAT-1"; //etiqueta que se envia para que el servidor reconozca el formato
    public static final int MAX_MENSAJE = 1000;
    public static final int MAX_FILAS = 500;
    private static final int MAX_CAMPOS = 6;

    private Protocolo() {}

    //FILA: un elemento que puede mostrar una pantalla
    // Una fila puede representar a un usuario, un grupo o un mensaje.
    // Por ejemplo, en un usuario id es su identificador, titulo es su nombre y detalle es su correo
    public record Fila(int id, String titulo, String detalle) {
        @Override
        public String toString() { 
            return titulo; 
        }
    }

    //PETICION: La acción que se solicita  y sus argumentos
    public record Peticion(String accion, List<String> campos) {
        //Este método recibe  el indice del dato que se quiere leer
        public String campo(int posicion) {
            return campos.get(posicion);
        }

        //Este método sirve para cuando un campo debe interpretarse como un ID
        public int id(int posicion) {
            int id = Integer.parseInt(campo(posicion)); //convierte a entero
            if (id < 1) throw new IllegalArgumentException("Identificador inválido.");
            return id;
        }
    }

    //RESPUESTA: lo que el servidor devuelve al cliente
    //Se define una respuesta con tres valores: correcta indica si la operación fue exitosa, 
    //aviso() contiene un mensaje y filas() devuelve la lista de datos
    public record Respuesta(boolean correcta, String aviso, List<Fila> filas) {
        //Este metodo sirve para crear una respuesta que incluye datos
        public static Respuesta ok(String aviso, List<Fila> filas) {
            return new Respuesta(true, aviso, filas);
        }

        //otra version de ok que recibe unicamente un aviso
        public static Respuesta ok(String aviso) {
            return ok(aviso, List.of());
        }

        //preparacion de una respuesta de rechazo
        public static Respuesta error(String aviso) {
            return new Respuesta(false, aviso, List.of());
        }
    }

    //EL CLIENTE ESCRIBE LA PETICIÓN:
    public static void escribirPeticion(DataOutputStream salida, String accion, String... campos)
            throws IOException {
        salida.writeUTF(VERSION);
        salida.writeUTF(accion); //define acción, por ejemplo BUSCAR
        salida.writeInt(campos.length); //cantidad de campos
        
        //se recorren los campos y escribe cada texto en el ordenrecibido
        for (String campo : campos) salida.writeUTF(campo); 
        salida.flush(); 
    }

    //ESTE METODO ES PARA EL SERVIDOR:
    public static Peticion leerPeticion(DataInputStream entrada) throws IOException {
        if (!VERSION.equals(entrada.readUTF())) throw new IOException("Versión incompatible.");
        
        String accion = entrada.readUTF();
        int cantidad = entrada.readInt();
        
        if (cantidad < 0 || cantidad > MAX_CAMPOS) throw new IOException("Petición inválida.");
        List<String> campos = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) campos.add(entrada.readUTF());
        return new Peticion(accion, campos);
    }

    //ESTE METODO TAMBIEN ES PARA EL SERVIDOR:
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

    //CLIENTE: SE LEE Y RECONSTRUYE LA RESPUESTA: 
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
