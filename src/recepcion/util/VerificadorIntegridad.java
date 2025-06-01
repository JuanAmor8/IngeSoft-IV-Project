package recepcion.util;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import recepcion.modelo.VotoRecibido;

/**
 * Clase responsable de verificar la integridad y autenticidad de los votos recibidos.
 * Implementa el patrón Singleton para garantizar una única instancia.
 */
public class VerificadorIntegridad {
    
    private static VerificadorIntegridad instancia;
    private final Map<String, PublicKey> clavesPublicasEstaciones;
    private final RegistroAuditoriaServidor registroAuditoria;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     */
    private VerificadorIntegridad() {
        this.clavesPublicasEstaciones = new ConcurrentHashMap<>();
        this.registroAuditoria = RegistroAuditoriaServidor.getInstancia();
    }
    
    /**
     * Obtiene la única instancia de VerificadorIntegridad (patrón Singleton).
     * 
     * @return Instancia de VerificadorIntegridad
     */
    public static synchronized VerificadorIntegridad getInstancia() {
        if (instancia == null) {
            instancia = new VerificadorIntegridad();
        }
        return instancia;
    }
    
    /**
     * Registra la clave pública de una estación de votación.
     * 
     * @param idEstacion Identificador de la estación
     * @param clavePublicaBase64 Clave pública en formato Base64
     * @return true si se registró correctamente, false en caso contrario
     */
    public boolean registrarClavePublicaEstacion(String idEstacion, String clavePublicaBase64) {
        try {
            // Decodificar clave pública
            byte[] bytesClavePublica = Base64.getDecoder().decode(clavePublicaBase64);
            
            // Reconstruir clave pública
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytesClavePublica);
            PublicKey clavePublica = keyFactory.generatePublic(keySpec);
            
            // Almacenar clave pública
            clavesPublicasEstaciones.put(idEstacion, clavePublica);
            
            registroAuditoria.info("Clave pública registrada para estación: " + idEstacion);
            return true;
        } catch (Exception e) {
            registroAuditoria.error("Error al registrar clave pública de estación: " + idEstacion, e);
            return false;
        }
    }
    
    /**
     * Verifica la firma digital de un voto recibido.
     * 
     * @param voto Voto a verificar
     * @param idEstacion Identificador de la estación
     * @return true si la firma es válida, false en caso contrario
     */
    public boolean verificarFirma(VotoRecibido voto, String idEstacion) {
        try {
            // Obtener clave pública de la estación
            PublicKey clavePublica = clavesPublicasEstaciones.get(idEstacion);
            if (clavePublica == null) {
                registroAuditoria.advertencia("No se ha registrado la clave pública para la estación: " + idEstacion);
                return false;
            }
            
            // Crear verificador de firma
            Signature verificador = Signature.getInstance("SHA256withRSA");
            verificador.initVerify(clavePublica);
            
            // Datos a verificar: ID voto + ID mesa + timestamp + datos cifrados
            String datosAVerificar = voto.getId().toString() + voto.getIdMesa() + 
                                    voto.getTimestamp().toString();
            
            verificador.update(datosAVerificar.getBytes());
            if (voto.getDatosEncriptados() != null) {
                verificador.update(voto.getDatosEncriptados());
            }
            
            // Verificar firma
            boolean resultado = verificador.verify(voto.getFirma());
            
            if (resultado) {
                voto.marcarComoVerificado();
                registroAuditoria.info("Firma verificada correctamente para voto: " + voto.getId());
            } else {
                registroAuditoria.advertencia("Verificación de firma fallida para voto: " + voto.getId());
            }
            
            return resultado;
        } catch (Exception e) {
            registroAuditoria.error("Error al verificar firma del voto: " + voto.getId(), e);
            return false;
        }
    }
}
