package transmision.ice;

import com.zeroc.Ice.Current;

/**
 * Interfaz ICE para el receptor de votos en el servidor central.
 * Define los métodos que deben ser implementados por el servidor.
 */
public interface ReceptorVotos extends com.zeroc.Ice.Object {
    
    /**
     * Recibe un voto desde una estación de votación.
     * 
     * @param idVoto Identificador único del voto
     * @param idMesa Identificador de la mesa de votación
     * @param timestamp Fecha y hora de emisión del voto
     * @param datosEncriptados Datos del voto encriptados
     * @param firma Firma digital para verificar integridad
     * @param clavePublicaEstacion Clave pública de la estación en Base64
     * @return true si el voto fue recibido correctamente, false en caso contrario
     */
    boolean recibirVoto(String idVoto, String idMesa, String timestamp, 
                        byte[] datosEncriptados, byte[] firma, 
                        String clavePublicaEstacion, Current current);
    
    /**
     * Verifica el estado de conexión con el servidor.
     * 
     * @return true si el servidor está operativo
     */
    boolean ping(Current current);
    
    /**
     * Obtiene la clave pública del servidor en formato Base64.
     * 
     * @return Clave pública del servidor
     */
    String obtenerClavePublica(Current current);
}
