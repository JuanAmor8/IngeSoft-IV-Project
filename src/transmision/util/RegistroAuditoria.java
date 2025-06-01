package transmision.util;

import java.util.logging.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase responsable del registro de eventos para auditoría.
 * Implementa el patrón Singleton para garantizar una única instancia.
 */
public class RegistroAuditoria {
    
    private static RegistroAuditoria instancia;
    private final Logger logger;
    private final String directorioLogs;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     * Inicializa el sistema de logging.
     */
    private RegistroAuditoria() {
        this.logger = Logger.getLogger("RegistroVotacion");
        this.directorioLogs = System.getProperty("user.home") + File.separator + "logs_votacion";
        
        try {
            // Crear directorio de logs si no existe
            File dir = new File(directorioLogs);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Configurar handler para archivo
            String nombreArchivo = directorioLogs + File.separator + 
                    "votacion_" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now()) + ".log";
            
            FileHandler fileHandler = new FileHandler(nombreArchivo, true);
            fileHandler.setFormatter(new SimpleFormatter());
            
            // Eliminar handlers existentes y agregar el nuevo
            logger.setUseParentHandlers(false);
            for (Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
            }
            logger.addHandler(fileHandler);
            
            // Establecer nivel de logging
            logger.setLevel(Level.ALL);
            
        } catch (IOException e) {
            System.err.println("Error al inicializar el registro de auditoría: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene la única instancia de RegistroAuditoria (patrón Singleton).
     * 
     * @return Instancia de RegistroAuditoria
     */
    public static synchronized RegistroAuditoria getInstancia() {
        if (instancia == null) {
            instancia = new RegistroAuditoria();
        }
        return instancia;
    }
    
    /**
     * Registra un evento informativo.
     * 
     * @param mensaje Mensaje a registrar
     */
    public void info(String mensaje) {
        logger.info(mensaje);
    }
    
    /**
     * Registra un evento de advertencia.
     * 
     * @param mensaje Mensaje a registrar
     */
    public void advertencia(String mensaje) {
        logger.warning(mensaje);
    }
    
    /**
     * Registra un evento de error.
     * 
     * @param mensaje Mensaje a registrar
     * @param excepcion Excepción asociada al error
     */
    public void error(String mensaje, Throwable excepcion) {
        logger.log(Level.SEVERE, mensaje, excepcion);
    }
    
    /**
     * Registra un intento de voto.
     * 
     * @param idMesa Identificador de la mesa
     * @param documento Documento del votante
     * @param exitoso Indica si el voto fue exitoso
     */
    public void registrarIntentoVoto(String idMesa, String documento, boolean exitoso) {
        // No registramos el documento completo por privacidad, solo los últimos 4 dígitos
        String documentoEnmascarado = "XXXX" + documento.substring(Math.max(0, documento.length() - 4));
        
        String mensaje = String.format("INTENTO_VOTO|%s|%s|%s",
                idMesa,
                documentoEnmascarado,
                exitoso ? "EXITOSO" : "FALLIDO");
        
        logger.info(mensaje);
    }
    
    /**
     * Registra un intento de fraude.
     * 
     * @param idMesa Identificador de la mesa
     * @param documento Documento del votante
     * @param motivo Motivo del intento de fraude
     */
    public void registrarIntentoFraude(String idMesa, String documento, String motivo) {
        // No registramos el documento completo por privacidad, solo los últimos 4 dígitos
        String documentoEnmascarado = "XXXX" + documento.substring(Math.max(0, documento.length() - 4));
        
        String mensaje = String.format("INTENTO_FRAUDE|%s|%s|%s",
                idMesa,
                documentoEnmascarado,
                motivo);
        
        logger.warning(mensaje);
    }
    
    /**
     * Registra la transmisión de un voto.
     * 
     * @param idVoto Identificador del voto
     * @param idMesa Identificador de la mesa
     * @param exitoso Indica si la transmisión fue exitosa
     */
    public void registrarTransmision(String idVoto, String idMesa, boolean exitoso) {
        String mensaje = String.format("TRANSMISION|%s|%s|%s",
                idVoto,
                idMesa,
                exitoso ? "EXITOSO" : "FALLIDO");
        
        logger.info(mensaje);
    }
}
