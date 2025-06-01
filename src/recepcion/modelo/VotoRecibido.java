package recepcion.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase que representa un voto recibido en el servidor central.
 * Implementa Serializable para permitir su almacenamiento.
 */
public class VotoRecibido implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final UUID id;                  // Identificador único del voto
    private final String idMesa;            // Identificador de la mesa de votación
    private final LocalDateTime timestamp;  // Fecha y hora de emisión del voto
    private final LocalDateTime recepcion;  // Fecha y hora de recepción en el servidor
    private final byte[] firma;             // Firma digital para verificar integridad
    private final byte[] datosEncriptados;  // Datos del voto encriptados
    private String idCandidato;             // Identificador del candidato (después de descifrar)
    private boolean verificado;             // Indica si el voto ha sido verificado
    private boolean contabilizado;          // Indica si el voto ha sido contabilizado
    
    /**
     * Constructor para crear un nuevo voto recibido.
     * 
     * @param id Identificador único del voto
     * @param idMesa Identificador de la mesa de votación
     * @param timestamp Fecha y hora de emisión del voto
     * @param datosEncriptados Datos del voto encriptados
     * @param firma Firma digital para verificar integridad
     */
    public VotoRecibido(UUID id, String idMesa, LocalDateTime timestamp, 
                       byte[] datosEncriptados, byte[] firma) {
        this.id = id;
        this.idMesa = idMesa;
        this.timestamp = timestamp;
        this.recepcion = LocalDateTime.now();
        this.datosEncriptados = datosEncriptados;
        this.firma = firma;
        this.verificado = false;
        this.contabilizado = false;
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
     * Obtiene la fecha y hora de emisión del voto.
     * 
     * @return Timestamp de emisión
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Obtiene la fecha y hora de recepción del voto en el servidor.
     * 
     * @return Timestamp de recepción
     */
    public LocalDateTime getRecepcion() {
        return recepcion;
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
     * Obtiene los datos encriptados del voto.
     * 
     * @return Datos encriptados
     */
    public byte[] getDatosEncriptados() {
        return datosEncriptados;
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
     * Establece el identificador del candidato después de descifrar.
     * 
     * @param idCandidato Identificador del candidato
     */
    public void setIdCandidato(String idCandidato) {
        this.idCandidato = idCandidato;
    }
    
    /**
     * Verifica si el voto ha sido verificado.
     * 
     * @return true si ha sido verificado, false en caso contrario
     */
    public boolean isVerificado() {
        return verificado;
    }
    
    /**
     * Marca el voto como verificado.
     */
    public void marcarComoVerificado() {
        this.verificado = true;
    }
    
    /**
     * Verifica si el voto ha sido contabilizado.
     * 
     * @return true si ha sido contabilizado, false en caso contrario
     */
    public boolean isContabilizado() {
        return contabilizado;
    }
    
    /**
     * Marca el voto como contabilizado.
     */
    public void marcarComoContabilizado() {
        this.contabilizado = true;
    }
    
    @Override
    public String toString() {
        return "VotoRecibido{" +
                "id=" + id +
                ", idMesa='" + idMesa + '\'' +
                ", timestamp=" + timestamp +
                ", recepcion=" + recepcion +
                ", verificado=" + verificado +
                ", contabilizado=" + contabilizado +
                '}';
    }
}
