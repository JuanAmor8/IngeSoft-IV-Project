package transmision.util;

import transmision.modelo.Votante;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase responsable de validar la elegibilidad de los votantes.
 * Implementa el patrón Singleton para garantizar una única instancia.
 */
public class ValidadorElegibilidad {
    
    private static ValidadorElegibilidad instancia;
    private final ConcurrentHashMap<String, Boolean> votantesRegistrados;
    private final RegistroAuditoria registroAuditoria;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     */
    private ValidadorElegibilidad() {
        this.votantesRegistrados = new ConcurrentHashMap<>();
        this.registroAuditoria = RegistroAuditoria.getInstancia();
    }
    
    /**
     * Obtiene la única instancia de ValidadorElegibilidad (patrón Singleton).
     * 
     * @return Instancia de ValidadorElegibilidad
     */
    public static synchronized ValidadorElegibilidad getInstancia() {
        if (instancia == null) {
            instancia = new ValidadorElegibilidad();
        }
        return instancia;
    }
    
    /**
     * Valida si un votante es elegible para votar en la mesa especificada.
     * 
     * @param votante Votante a validar
     * @param idMesaActual Identificador de la mesa actual
     * @return Resultado de la validación con el motivo en caso de rechazo
     */
    public ResultadoValidacion validarElegibilidad(Votante votante, String idMesaActual) {
        // Verificar si el votante tiene antecedentes criminales
        if (votante.tieneAntecedentes()) {
            registroAuditoria.registrarIntentoFraude(
                idMesaActual, 
                votante.getDocumento(), 
                "Votante con antecedentes criminales"
            );
            return new ResultadoValidacion(false, "El votante tiene antecedentes criminales");
        }
        
        // Verificar si el votante está en la mesa correcta
        if (!votante.getIdMesaAsignada().equals(idMesaActual)) {
            registroAuditoria.registrarIntentoFraude(
                idMesaActual, 
                votante.getDocumento(), 
                "Votante en mesa incorrecta"
            );
            return new ResultadoValidacion(false, "El votante debe votar en la mesa " + votante.getIdMesaAsignada());
        }
        
        // Verificar si el votante ya ha votado localmente
        if (votante.haVotado()) {
            registroAuditoria.registrarIntentoFraude(
                idMesaActual, 
                votante.getDocumento(), 
                "Votante ya ha votado"
            );
            return new ResultadoValidacion(false, "El votante ya ha emitido su voto");
        }
        
        // Verificar si el votante ya ha votado en otra mesa (registro global)
        Boolean yaVoto = votantesRegistrados.get(votante.getDocumento());
        if (yaVoto != null && yaVoto) {
            registroAuditoria.registrarIntentoFraude(
                idMesaActual, 
                votante.getDocumento(), 
                "Votante ya ha votado en otra mesa"
            );
            return new ResultadoValidacion(false, "El votante ya ha emitido su voto en otra mesa");
        }
        
        // El votante es elegible
        return new ResultadoValidacion(true, "");
    }
    
    /**
     * Registra que un votante ha emitido su voto.
     * 
     * @param documento Documento del votante
     * @param idMesa Identificador de la mesa
     */
    public void registrarVoto(String documento, String idMesa) {
        votantesRegistrados.put(documento, true);
        registroAuditoria.registrarIntentoVoto(idMesa, documento, true);
    }
    
    /**
     * Consulta si un votante ya ha emitido su voto.
     * 
     * @param documento Documento del votante
     * @return true si ya ha votado, false en caso contrario
     */
    public boolean haVotado(String documento) {
        Boolean yaVoto = votantesRegistrados.get(documento);
        return yaVoto != null && yaVoto;
    }
    
    /**
     * Clase interna para representar el resultado de la validación.
     */
    public static class ResultadoValidacion {
        private final boolean esElegible;
        private final String motivo;
        
        public ResultadoValidacion(boolean esElegible, String motivo) {
            this.esElegible = esElegible;
            this.motivo = motivo;
        }
        
        public boolean esElegible() {
            return esElegible;
        }
        
        public String getMotivo() {
            return motivo;
        }
    }
}
