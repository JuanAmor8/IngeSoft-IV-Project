package transmision.modelo;

import java.io.Serializable;

/**
 * Clase que representa a un votante en el sistema.
 * Contiene la información necesaria para validar su elegibilidad.
 */
public class Votante implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String documento;         // Número de documento de identidad
    private final String idMesaAsignada;    // Mesa asignada para votar
    private boolean haVotado;               // Indica si ya ha votado
    private boolean tieneAntecedentes;      // Indica si tiene antecedentes criminales
    
    /**
     * Constructor para crear un nuevo votante.
     * 
     * @param documento Número de documento de identidad
     * @param idMesaAsignada Identificador de la mesa asignada
     */
    public Votante(String documento, String idMesaAsignada) {
        this.documento = documento;
        this.idMesaAsignada = idMesaAsignada;
        this.haVotado = false;
        this.tieneAntecedentes = false;
    }
    
    /**
     * Obtiene el número de documento de identidad.
     * 
     * @return Documento de identidad
     */
    public String getDocumento() {
        return documento;
    }
    
    /**
     * Obtiene el identificador de la mesa asignada.
     * 
     * @return Identificador de la mesa
     */
    public String getIdMesaAsignada() {
        return idMesaAsignada;
    }
    
    /**
     * Verifica si el votante ya ha emitido su voto.
     * 
     * @return true si ya ha votado, false en caso contrario
     */
    public boolean haVotado() {
        return haVotado;
    }
    
    /**
     * Marca al votante como que ya ha emitido su voto.
     */
    public void marcarComoVotante() {
        this.haVotado = true;
    }
    
    /**
     * Verifica si el votante tiene antecedentes criminales.
     * 
     * @return true si tiene antecedentes, false en caso contrario
     */
    public boolean tieneAntecedentes() {
        return tieneAntecedentes;
    }
    
    /**
     * Establece si el votante tiene antecedentes criminales.
     * 
     * @param tieneAntecedentes true si tiene antecedentes, false en caso contrario
     */
    public void setTieneAntecedentes(boolean tieneAntecedentes) {
        this.tieneAntecedentes = tieneAntecedentes;
    }
    
    @Override
    public String toString() {
        return "Votante{" +
                "documento='" + documento + '\'' +
                ", idMesaAsignada='" + idMesaAsignada + '\'' +
                ", haVotado=" + haVotado +
                ", tieneAntecedentes=" + tieneAntecedentes +
                '}';
    }
}
