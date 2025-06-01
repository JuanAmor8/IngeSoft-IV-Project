package transmision.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase que representa un voto emitido por un ciudadano.
 * Implementa Serializable para permitir su transmisión a través de ICE.
 */
public class Voto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final UUID id;                  // Identificador único del voto
    private final String idMesa;            // Identificador de la mesa de votación
    private final String idCandidato;       // Identificador del candidato elegido
    private final LocalDateTime timestamp;  // Fecha y hora de emisión del voto
    private byte[] firma;                   // Firma digital para verificar integridad
    private byte[] datosEncriptados;        // Datos del voto encriptados
    
    /**
     * Constructor para crear un nuevo voto.
     * 
     * @param idMesa Identificador de la mesa de votación
     * @param idCandidato Identificador del candidato elegido
     */
    public Voto(String idMesa, String idCandidato) {
        this.id = UUID.randomUUID();
        this.idMesa = idMesa;
        this.idCandidato = idCandidato;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Obtiene el identificador único del voto.
     * 
     * @return UUID del voto
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Obtiene el identificador de la mesa de votación.
     * 
     * @return Identificador de la mesa
     */
    public String getIdMesa() {
        return idMesa;
    }
    
    /**
     * Obtiene el identificador del candidato elegido.
     * 
     * @return Identificador del candidato
     */
    public String getIdCandidato() {
        return idCandidato;
    }
    
    /**
     * Obtiene la fecha y hora de emisión del voto.
     * 
     * @return Timestamp de emisión
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Establece la firma digital del voto.
     * 
     * @param firma Firma digital
     */
    public void setFirma(byte[] firma) {
        this.firma = firma;
    }
    
    /**
     * Obtiene la firma digital del voto.
     * 
     * @return Firma digital
     */
    public byte[] getFirma() {
        return firma;
    }
    
    /**
     * Establece los datos encriptados del voto.
     * 
     * @param datosEncriptados Datos encriptados
     */
    public void setDatosEncriptados(byte[] datosEncriptados) {
        this.datosEncriptados = datosEncriptados;
    }
    
    /**
     * Obtiene los datos encriptados del voto.
     * 
     * @return Datos encriptados
     */
    public byte[] getDatosEncriptados() {
        return datosEncriptados;
    }
    
    @Override
    public String toString() {
        return "Voto{" +
                "id=" + id +
                ", idMesa='" + idMesa + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
