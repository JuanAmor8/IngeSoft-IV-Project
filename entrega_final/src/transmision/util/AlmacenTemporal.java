package transmision.util;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import transmision.modelo.Voto;

/**
 * Clase responsable del almacenamiento temporal de votos en la estación de votación.
 * Implementa el patrón Singleton para garantizar una única instancia.
 * Proporciona mecanismos de persistencia y recuperación ante fallos.
 */
public class AlmacenTemporal {
    
    private static AlmacenTemporal instancia;
    private final ConcurrentHashMap<UUID, Voto> votosEnMemoria;
    private final String directorioAlmacen;
    private final ScheduledExecutorService servicioPersistencia;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     * Inicializa las estructuras de almacenamiento y el servicio de persistencia.
     */
    private AlmacenTemporal() {
        this.votosEnMemoria = new ConcurrentHashMap<>();
        this.directorioAlmacen = System.getProperty("user.home") + File.separator + "votos_temp";
        
        // Crear directorio de almacenamiento si no existe
        try {
            Files.createDirectories(Paths.get(directorioAlmacen));
        } catch (IOException e) {
            throw new RuntimeException("Error al crear directorio de almacenamiento temporal: " + e.getMessage(), e);
        }
        
        // Iniciar servicio de persistencia periódica
        this.servicioPersistencia = Executors.newSingleThreadScheduledExecutor();
        this.servicioPersistencia.scheduleAtFixedRate(
            this::persistirVotosPendientes, 
            1, 5, TimeUnit.MINUTES
        );
        
        // Recuperar votos pendientes al iniciar
        recuperarVotosPendientes();
    }
    
    /**
     * Obtiene la única instancia de AlmacenTemporal (patrón Singleton).
     * 
     * @return Instancia de AlmacenTemporal
     */
    public static synchronized AlmacenTemporal getInstancia() {
        if (instancia == null) {
            instancia = new AlmacenTemporal();
        }
        return instancia;
    }
    
    /**
     * Almacena un voto en memoria y lo persiste en disco.
     * 
     * @param voto Voto a almacenar
     * @return true si se almacenó correctamente, false en caso contrario
     */
    public boolean almacenarVoto(Voto voto) {
        try {
            // Almacenar en memoria
            votosEnMemoria.put(voto.getId(), voto);
            
            // Persistir en disco
            persistirVoto(voto);
            
            return true;
        } catch (Exception e) {
            System.err.println("Error al almacenar voto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Marca un voto como transmitido exitosamente y lo elimina del almacén.
     * 
     * @param idVoto Identificador del voto transmitido
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean marcarVotoTransmitido(UUID idVoto) {
        try {
            // Eliminar de memoria
            Voto voto = votosEnMemoria.remove(idVoto);
            
            if (voto != null) {
                // Eliminar archivo de persistencia
                Path archivoVoto = Paths.get(directorioAlmacen, idVoto.toString() + ".voto");
                Files.deleteIfExists(archivoVoto);
                
                // Registrar en log de transmitidos
                registrarVotoTransmitido(voto);
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error al marcar voto como transmitido: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Obtiene la lista de votos pendientes de transmisión.
     * 
     * @return Lista de votos pendientes
     */
    public List<Voto> getVotosPendientes() {
        return new ArrayList<>(votosEnMemoria.values());
    }
    
    /**
     * Persiste un voto individual en disco.
     * 
     * @param voto Voto a persistir
     * @throws IOException Si ocurre un error de E/S
     */
    private void persistirVoto(Voto voto) throws IOException {
        Path archivoVoto = Paths.get(directorioAlmacen, voto.getId().toString() + ".voto");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(archivoVoto)))) {
            oos.writeObject(voto);
        }
    }
    
    /**
     * Persiste todos los votos pendientes en disco.
     * Este método se ejecuta periódicamente como respaldo.
     */
    private void persistirVotosPendientes() {
        try {
            for (Voto voto : votosEnMemoria.values()) {
                persistirVoto(voto);
            }
        } catch (Exception e) {
            System.err.println("Error al persistir votos pendientes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Recupera los votos pendientes desde disco al iniciar el sistema.
     */
    private void recuperarVotosPendientes() {
        try {
            Files.list(Paths.get(directorioAlmacen))
                .filter(path -> path.toString().endsWith(".voto"))
                .forEach(path -> {
                    try (ObjectInputStream ois = new ObjectInputStream(
                            new BufferedInputStream(Files.newInputStream(path)))) {
                        Voto voto = (Voto) ois.readObject();
                        votosEnMemoria.put(voto.getId(), voto);
                    } catch (Exception e) {
                        System.err.println("Error al recuperar voto desde " + path + ": " + e.getMessage());
                    }
                });
        } catch (Exception e) {
            System.err.println("Error al recuperar votos pendientes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registra un voto transmitido en el log de auditoría.
     * 
     * @param voto Voto transmitido
     * @throws IOException Si ocurre un error de E/S
     */
    private void registrarVotoTransmitido(Voto voto) throws IOException {
        Path archivoLog = Paths.get(directorioAlmacen, "votos_transmitidos.log");
        
        String entrada = String.format("%s|%s|%s|%s%n",
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()),
                voto.getId(),
                voto.getIdMesa(),
                voto.getTimestamp());
        
        Files.write(archivoLog, entrada.getBytes(), 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    
    /**
     * Cierra el servicio de persistencia al finalizar.
     */
    public void cerrar() {
        try {
            // Persistir todos los votos pendientes antes de cerrar
            persistirVotosPendientes();
            
            // Cerrar el servicio de persistencia
            servicioPersistencia.shutdown();
            servicioPersistencia.awaitTermination(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("Error al cerrar el almacén temporal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
