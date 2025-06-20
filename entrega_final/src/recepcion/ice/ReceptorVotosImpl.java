package recepcion.ice;

import Ice.Current;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import recepcion.modelo.VotoRecibido;
import recepcion.util.ConsolidadorResultados;
import recepcion.util.DescifradorVotos;
import recepcion.util.DetectorDuplicados;
import recepcion.util.RegistroAuditoriaServidor;
import recepcion.util.VerificadorIntegridad;

/**
 * Implementación del servicio ICE para recepción de votos.
 * Gestiona la recepción, verificación y procesamiento de votos desde las estaciones.
 */
public class ReceptorVotosImpl implements ReceptorVotos {
    
    private final DescifradorVotos descifrador;
    private final VerificadorIntegridad verificador;
    private final DetectorDuplicados detector;
    private final ConsolidadorResultados consolidador;
    private final RegistroAuditoriaServidor registroAuditoria;
    
    /**
     * Constructor de la implementación del receptor de votos.
     */
    public ReceptorVotosImpl() {
        this.descifrador = DescifradorVotos.getInstancia();
        this.verificador = VerificadorIntegridad.getInstancia();
        this.detector = DetectorDuplicados.getInstancia();
        this.consolidador = ConsolidadorResultados.getInstancia();
        this.registroAuditoria = RegistroAuditoriaServidor.getInstancia();
    }
    
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
    @Override
    public boolean recibirVoto(String idVoto, String idMesa, String timestamp, 
                              byte[] datosEncriptados, byte[] firma, 
                              String clavePublicaEstacion, Current current) {
        try {
            // Registrar recepción
            registroAuditoria.info("Recibiendo voto: " + idVoto + " de mesa: " + idMesa);
            
            // Convertir datos
            UUID uuid = UUID.fromString(idVoto);
            LocalDateTime fechaHora = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Verificar si es un duplicado
            if (!detector.verificarYRegistrar(uuid, idMesa)) {
                registroAuditoria.advertencia("Voto duplicado detectado: " + idVoto);
                return false;
            }
            
            // Incrementar contador de votos recibidos
            consolidador.registrarVotoRecibido();
            
            // Crear objeto de voto recibido
            VotoRecibido voto = new VotoRecibido(uuid, idMesa, fechaHora, datosEncriptados, firma);
            
            // Registrar clave pública de la estación si es necesario
            verificador.registrarClavePublicaEstacion(idMesa, clavePublicaEstacion);
            
            // Verificar firma
            if (!verificador.verificarFirma(voto, idMesa)) {
                registroAuditoria.advertencia("Verificación de firma fallida para voto: " + idVoto);
                registroAuditoria.registrarVerificacion(idVoto, idMesa, false);
                return false;
            }
            
            // Descifrar voto
            if (!descifrador.descifrarVoto(voto, idMesa)) {
                registroAuditoria.advertencia("Descifrado fallido para voto: " + idVoto);
                return false;
            }
            
            // Contabilizar voto
            boolean resultado = consolidador.contabilizarVoto(voto);
            
            // Registrar resultado
            registroAuditoria.registrarRecepcion(idVoto, idMesa, resultado);
            
            return resultado;
        } catch (Exception e) {
            registroAuditoria.error("Error al procesar voto: " + idVoto, e);
            return false;
        }
    }
    
    /**
     * Verifica el estado de conexión con el servidor.
     * 
     * @return true si el servidor está operativo
     */
    @Override
    public boolean ping(Current current) {
        return true;
    }
    
    /**
     * Obtiene la clave pública del servidor en formato Base64.
     * 
     * @return Clave pública del servidor
     */
    @Override
    public String obtenerClavePublica(Current current) {
        return descifrador.getClavePublicaBase64();
    }
}
