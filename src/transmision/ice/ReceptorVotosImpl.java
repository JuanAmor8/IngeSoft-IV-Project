package transmision.ice;

import com.zeroc.Ice.Current;

/**
 * Implementación mínima para ReceptorVotos.
 * Esta es una clase stub para permitir la compilación del proyecto.
 */
public class ReceptorVotosImpl implements ReceptorVotos {
    
    @Override
    public boolean recibirVoto(String idVoto, String idMesa, String timestamp, 
                             byte[] datosEncriptados, byte[] firma, 
                             String clavePublicaEstacion, Current current) {
        // Implementación simulada para compilación
        return true;
    }
    
    @Override
    public boolean ping(Current current) {
        // Implementación simulada para compilación
        return true;
    }
    
    @Override
    public String obtenerClavePublica(Current current) {
        // Implementación simulada para compilación
        return "";
    }
} 