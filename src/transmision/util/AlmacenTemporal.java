package transmision.util;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import transmision.modelo.Voto;

/**
 * Clase responsable del almacenamiento temporal de votos para garantizar
 * la tolerancia a fallos en la transmisión.
 * Implementa el patrón Singleton para garantizar una única instancia.
 * Proporciona mecanismos de persistencia y recuperación ante fallos.
 */
public class AlmacenTemporal {
    
    private static AlmacenTemporal instancia;
    private final Map<UUID, Voto> votos;
    private final Map<UUID, Boolean> estadoTransmision; // true = transmitido, false = pendiente
    private final String directorioAlmacen;
    private final ScheduledExecutorService servicioPersistencia;
    private final RegistroAuditoria registroAuditoria;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     * Inicializa las estructuras de almacenamiento y el servicio de persistencia.
     */
    private AlmacenTemporal() {
        this.votos = new ConcurrentHashMap<>();
        this.estadoTransmision = new ConcurrentHashMap<>();
        this.directorioAlmacen = System.getProperty("user.home") + File.separator + "votos_temp";
        
        // Crear directorio de almacenamiento si no existe
        crearDirectorioAlmacen();
        
        // Iniciar servicio de persistencia
        this.servicioPersistencia = Executors.newSingleThreadScheduledExecutor();
        this.servicioPersistencia.scheduleAtFixedRate(
            this::persistirVotosPendientes, 
            30, 60, TimeUnit.SECONDS
        );
        
        // Recuperar votos pendientes al iniciar
        recuperarVotosPendientes();
        
        this.registroAuditoria = RegistroAuditoria.getInstancia();
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
     * Almacena un voto temporalmente.
     * 
     * @param voto Voto a almacenar
     * @return true si se almacenó correctamente, false en caso contrario
     */
    public boolean almacenarVoto(Voto voto) {
        try {
            votos.put(voto.getId(), voto);
            estadoTransmision.put(voto.getId(), false); // Inicialmente pendiente
            persistirVoto(voto); // Persistir en disco
            registroAuditoria.info("Voto almacenado temporalmente: " + voto.getId());
            return true;
        } catch (Exception e) {
            registroAuditoria.error("Error al almacenar voto temporalmente: " + voto.getId(), e);
            return false;
        }
    }
    
    /**
     * Marca un voto como transmitido.
     * 
     * @param idVoto Identificador del voto
     * @return true si se marcó correctamente, false en caso contrario
     */
    public boolean marcarVotoTransmitido(UUID idVoto) {
        try {
            if (votos.containsKey(idVoto)) {
                estadoTransmision.put(idVoto, true);
                registroAuditoria.info("Voto marcado como transmitido: " + idVoto);
                return true;
            } else {
                registroAuditoria.advertencia("Intento de marcar como transmitido un voto no almacenado: " + idVoto);
                return false;
            }
        } catch (Exception e) {
            registroAuditoria.error("Error al marcar voto como transmitido: " + idVoto, e);
            return false;
        }
    }
    
    /**
     * Marca un voto como pendiente para retransmisión.
     * 
     * @param idVoto Identificador del voto
     * @return true si se marcó correctamente, false en caso contrario
     */
    public boolean marcarVotoPendiente(UUID idVoto) {
        try {
            if (votos.containsKey(idVoto)) {
                estadoTransmision.put(idVoto, false);
                registroAuditoria.info("Voto marcado como pendiente para retransmisión: " + idVoto);
                return true;
            } else {
                registroAuditoria.advertencia("Intento de marcar como pendiente un voto no almacenado: " + idVoto);
                return false;
            }
        } catch (Exception e) {
            registroAuditoria.error("Error al marcar voto como pendiente: " + idVoto, e);
            return false;
        }
    }
    
    /**
     * Obtiene la lista de votos pendientes de transmisión.
     * 
     * @return Lista de votos pendientes
     */
    public List<Voto> getVotosPendientes() {
        List<Voto> votosPendientes = new ArrayList<>();
        
        for (Map.Entry<UUID, Boolean> entry : estadoTransmision.entrySet()) {
            if (!entry.getValue()) { // Si no está transmitido (pendiente)
                UUID idVoto = entry.getKey();
                Voto voto = votos.get(idVoto);
                if (voto != null) {
                    votosPendientes.add(voto);
                }
            }
        }
        
        return votosPendientes;
    }
    
    /**
     * Obtiene la lista de votos ya transmitidos.
     * 
     * @return Lista de votos transmitidos
     */
    public List<Voto> getVotosTransmitidos() {
        return estadoTransmision.entrySet().stream()
            .filter(Map.Entry::getValue) // Solo los transmitidos (true)
            .map(entry -> votos.get(entry.getKey()))
            .filter(voto -> voto != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Elimina un voto del almacén temporal.
     * 
     * @param idVoto Identificador del voto
     * @return true si se eliminó correctamente, false en caso contrario
     */
    public boolean eliminarVoto(UUID idVoto) {
        try {
            votos.remove(idVoto);
            estadoTransmision.remove(idVoto);
            
            // Eliminar archivo de persistencia
            Path archivoVoto = Paths.get(directorioAlmacen, idVoto.toString() + ".voto");
            Files.deleteIfExists(archivoVoto);
            
            registroAuditoria.info("Voto eliminado del almacén temporal: " + idVoto);
            return true;
        } catch (Exception e) {
            registroAuditoria.error("Error al eliminar voto del almacén temporal: " + idVoto, e);
            return false;
        }
    }
    
    /**
     * Limpia votos antiguos que ya han sido transmitidos correctamente.
     * 
     * @param horasAntiguedad Horas de antigüedad para considerar un voto como antiguo
     * @return Número de votos eliminados
     */
    public int limpiarVotosAntiguos(int horasAntiguedad) {
        int eliminados = 0;
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        
        List<UUID> aEliminar = new ArrayList<>();
        
        for (Map.Entry<UUID, Voto> entry : votos.entrySet()) {
            Voto voto = entry.getValue();
            boolean transmitido = estadoTransmision.getOrDefault(voto.getId(), false);
            
            // Si está transmitido y es antiguo
            if (transmitido && voto.getTimestamp().plusHours(horasAntiguedad).isBefore(ahora)) {
                aEliminar.add(voto.getId());
            }
        }
        
        // Eliminar los votos antiguos
        for (UUID id : aEliminar) {
            if (eliminarVoto(id)) {
                eliminados++;
            }
        }
        
        registroAuditoria.info("Limpieza completada: " + eliminados + " votos antiguos eliminados");
        return eliminados;
    }
    
    /**
     * Crea el directorio de almacenamiento si no existe.
     */
    private void crearDirectorioAlmacen() {
        try {
            Files.createDirectories(Paths.get(directorioAlmacen));
        } catch (IOException e) {
            System.err.println("Error al crear directorio de almacenamiento: " + e.getMessage());
        }
    }
    
    /**
     * Persiste un voto en disco.
     * 
     * @param voto Voto a persistir
     * @throws IOException Si ocurre un error de E/S
     */
    private void persistirVoto(Voto voto) {
        try {
            Path archivoVoto = Paths.get(directorioAlmacen, voto.getId().toString() + ".voto");
            
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(archivoVoto)))) {
                oos.writeObject(voto);
            }
        } catch (Exception e) {
            System.err.println("Error al persistir voto: " + e.getMessage());
        }
    }
    
    /**
     * Persiste todos los votos pendientes en disco.
     * Este método se ejecuta periódicamente.
     */
    private void persistirVotosPendientes() {
        try {
            for (Voto voto : votos.values()) {
                persistirVoto(voto);
            }
        } catch (Exception e) {
            System.err.println("Error al persistir votos pendientes: " + e.getMessage());
        }
    }
    
    /**
     * Recupera los votos pendientes desde disco al iniciar.
     */
    private void recuperarVotosPendientes() {
        try {
            Path dir = Paths.get(directorioAlmacen);
            if (Files.exists(dir)) {
                Files.walk(dir)
                    .filter(path -> path.toString().endsWith(".voto"))
                    .forEach(path -> {
                        try (ObjectInputStream ois = new ObjectInputStream(
                                new BufferedInputStream(Files.newInputStream(path)))) {
                            Voto voto = (Voto) ois.readObject();
                            votos.put(voto.getId(), voto);
                            estadoTransmision.put(voto.getId(), false); // Al recuperar, marcar como pendiente
                        } catch (Exception e) {
                            System.err.println("Error al recuperar voto desde " + path + ": " + e.getMessage());
                        }
                    });
            }
        } catch (Exception e) {
            System.err.println("Error al recuperar votos pendientes: " + e.getMessage());
        }
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
