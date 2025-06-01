package recepcion.util;

import java.util.logging.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase responsable del registro de eventos para auditoría en el servidor central.
 * Implementa el patrón Singleton para garantizar una única instancia.
 */
public class RegistroAuditoriaServidor {
    
    private static RegistroAuditoriaServidor instancia;
    private final Logger logger;
    private final String directorioLogs;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     * Inicializa el sistema de logging.
     */
    private RegistroAuditoriaServidor() {
        this.logger = Logger.getLogger("ServidorVotacion");
        this.directorioLogs = System.getProperty("user.home") + File.separator + "logs_servidor";
        
        try {
            // Crear directorio de logs si no existe
            File dir = new File(directorioLogs);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Configurar handler para archivo
            String nombreArchivo = directorioLogs + File.separator + 
                    "servidor_" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now()) + ".log";
            
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
            System.err.println("Error al inicializar el registro de auditoría del servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene la única instancia de RegistroAuditoriaServidor (patrón Singleton).
     * 
     * @return Instancia de RegistroAuditoriaServidor
     */
    public static synchronized RegistroAuditoriaServidor getInstancia() {
        if (instancia == null) {
            instancia = new RegistroAuditoriaServidor();
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
     * Registra la recepción de un voto.
     * 
     * @param idVoto Identificador del voto
     * @param idMesa Identificador de la mesa
     * @param exitoso Indica si la recepción fue exitosa
     */
    public void registrarRecepcion(String idVoto, String idMesa, boolean exitoso) {
        String mensaje = String.format("RECEPCION|%s|%s|%s",
                idVoto,
                idMesa,
                exitoso ? "EXITOSO" : "FALLIDO");
        
        logger.info(mensaje);
    }
    
    /**
     * Registra la verificación de un voto.
     * 
     * @param idVoto Identificador del voto
     * @param idMesa Identificador de la mesa
     * @param exitoso Indica si la verificación fue exitosa
     */
    public void registrarVerificacion(String idVoto, String idMesa, boolean exitoso) {
        String mensaje = String.format("VERIFICACION|%s|%s|%s",
                idVoto,
                idMesa,
                exitoso ? "EXITOSO" : "FALLIDO");
        
        logger.info(mensaje);
    }
    
    /**
     * Registra la contabilización de un voto.
     * 
     * @param idVoto Identificador del voto
     * @param idMesa Identificador de la mesa
     * @param idCandidato Identificador del candidato
     */
    public void registrarContabilizacion(String idVoto, String idMesa, String idCandidato) {
        String mensaje = String.format("CONTABILIZACION|%s|%s|%s",
                idVoto,
                idMesa,
                idCandidato);
        
        logger.info(mensaje);
    }
    
    /**
     * Registra un intento de voto duplicado.
     * 
     * @param idVoto Identificador del voto
     * @param idMesa Identificador de la mesa
     */
    public void registrarVotoDuplicado(String idVoto, String idMesa) {
        String mensaje = String.format("DUPLICADO|%s|%s",
                idVoto,
                idMesa);
        
        logger.warning(mensaje);
    }
}
