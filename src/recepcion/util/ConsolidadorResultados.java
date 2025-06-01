package recepcion.util;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.time.LocalDateTime;

import recepcion.modelo.VotoRecibido;

/**
 * Clase responsable de consolidar los resultados de la votación.
 * Implementa el patrón Singleton para garantizar una única instancia.
 */
public class ConsolidadorResultados {
    
    private static ConsolidadorResultados instancia;
    private final Map<String, AtomicInteger> votosPorCandidato;
    private final Map<String, AtomicInteger> votosPorMesa;
    private final AtomicInteger totalVotosRecibidos;
    private final AtomicInteger totalVotosContabilizados;
    private final ReadWriteLock lock;
    private final RegistroAuditoriaServidor registroAuditoria;
    private int totalVotantesRegistrados;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     */
    private ConsolidadorResultados() {
        this.votosPorCandidato = new ConcurrentHashMap<>();
        this.votosPorMesa = new ConcurrentHashMap<>();
        this.totalVotosRecibidos = new AtomicInteger(0);
        this.totalVotosContabilizados = new AtomicInteger(0);
        this.lock = new ReentrantReadWriteLock();
        this.registroAuditoria = RegistroAuditoriaServidor.getInstancia();
        this.totalVotantesRegistrados = 0;
    }
    
    /**
     * Obtiene la única instancia de ConsolidadorResultados (patrón Singleton).
     * 
     * @return Instancia de ConsolidadorResultados
     */
    public static synchronized ConsolidadorResultados getInstancia() {
        if (instancia == null) {
            instancia = new ConsolidadorResultados();
        }
        return instancia;
    }
    
    /**
     * Establece el total de votantes registrados en el sistema.
     * 
     * @param totalVotantes Número total de votantes registrados
     */
    public void setTotalVotantesRegistrados(int totalVotantes) {
        this.totalVotantesRegistrados = totalVotantes;
    }
    
    /**
     * Registra un voto recibido.
     */
    public void registrarVotoRecibido() {
        totalVotosRecibidos.incrementAndGet();
    }
    
    /**
     * Contabiliza un voto verificado.
     * 
     * @param voto Voto a contabilizar
     * @return true si se contabilizó correctamente, false en caso contrario
     */
    public boolean contabilizarVoto(VotoRecibido voto) {
        try {
            // Verificar que el voto tenga un candidato asignado
            if (voto.getIdCandidato() == null || voto.getIdCandidato().isEmpty()) {
                registroAuditoria.advertencia("Intento de contabilizar voto sin candidato: " + voto.getId());
                return false;
            }
            
            // Incrementar contador por candidato
            votosPorCandidato.computeIfAbsent(voto.getIdCandidato(), k -> new AtomicInteger(0))
                             .incrementAndGet();
            
            // Incrementar contador por mesa
            votosPorMesa.computeIfAbsent(voto.getIdMesa(), k -> new AtomicInteger(0))
                        .incrementAndGet();
            
            // Incrementar total de votos contabilizados
            totalVotosContabilizados.incrementAndGet();
            
            // Marcar voto como contabilizado
            voto.marcarComoContabilizado();
            
            // Registrar en log de auditoría
            registroAuditoria.registrarContabilizacion(
                voto.getId().toString(), 
                voto.getIdMesa(), 
                voto.getIdCandidato()
            );
            
            return true;
        } catch (Exception e) {
            registroAuditoria.error("Error al contabilizar voto: " + voto.getId(), e);
            return false;
        }
    }
    
    /**
     * Obtiene el total de votos recibidos.
     * 
     * @return Número total de votos recibidos
     */
    public int getTotalVotosRecibidos() {
        return totalVotosRecibidos.get();
    }
    
    /**
     * Obtiene el total de votos contabilizados.
     * 
     * @return Número total de votos contabilizados
     */
    public int getTotalVotosContabilizados() {
        return totalVotosContabilizados.get();
    }
    
    /**
     * Obtiene el porcentaje de participación electoral.
     * 
     * @return Porcentaje de participación (0-100)
     */
    public double getPorcentajeParticipacion() {
        if (totalVotantesRegistrados <= 0) {
            return 0.0;
        }
        return (totalVotosContabilizados.get() * 100.0) / totalVotantesRegistrados;
    }
    
    /**
     * Obtiene los resultados por candidato.
     * 
     * @return Mapa con los votos por candidato
     */
    public Map<String, Integer> getResultadosPorCandidato() {
        lock.readLock().lock();
        try {
            Map<String, Integer> resultados = new HashMap<>();
            for (Map.Entry<String, AtomicInteger> entry : votosPorCandidato.entrySet()) {
                resultados.put(entry.getKey(), entry.getValue().get());
            }
            return resultados;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtiene los resultados por mesa de votación.
     * 
     * @return Mapa con los votos por mesa
     */
    public Map<String, Integer> getResultadosPorMesa() {
        lock.readLock().lock();
        try {
            Map<String, Integer> resultados = new HashMap<>();
            for (Map.Entry<String, AtomicInteger> entry : votosPorMesa.entrySet()) {
                resultados.put(entry.getKey(), entry.getValue().get());
            }
            return resultados;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtiene el porcentaje de votos por candidato.
     * 
     * @return Mapa con los porcentajes por candidato
     */
    public Map<String, Double> getPorcentajesPorCandidato() {
        lock.readLock().lock();
        try {
            Map<String, Double> porcentajes = new HashMap<>();
            int total = totalVotosContabilizados.get();
            
            if (total > 0) {
                for (Map.Entry<String, AtomicInteger> entry : votosPorCandidato.entrySet()) {
                    double porcentaje = (entry.getValue().get() * 100.0) / total;
                    porcentajes.put(entry.getKey(), porcentaje);
                }
            }
            
            return porcentajes;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Obtiene un resumen de los resultados actuales.
     * 
     * @return Resumen de resultados
     */
    public String getResumenResultados() {
        lock.readLock().lock();
        try {
            StringBuilder resumen = new StringBuilder();
            
            resumen.append("=== RESUMEN DE RESULTADOS ===\n");
            resumen.append("Fecha y hora: ").append(LocalDateTime.now()).append("\n");
            resumen.append("Total votantes registrados: ").append(totalVotantesRegistrados).append("\n");
            resumen.append("Total votos recibidos: ").append(totalVotosRecibidos.get()).append("\n");
            resumen.append("Total votos contabilizados: ").append(totalVotosContabilizados.get()).append("\n");
            resumen.append("Porcentaje de participación: ").append(String.format("%.2f%%", getPorcentajeParticipacion())).append("\n\n");
            
            resumen.append("--- RESULTADOS POR CANDIDATO ---\n");
            Map<String, Integer> resultados = getResultadosPorCandidato();
            Map<String, Double> porcentajes = getPorcentajesPorCandidato();
            
            for (Map.Entry<String, Integer> entry : resultados.entrySet()) {
                String idCandidato = entry.getKey();
                int votos = entry.getValue();
                double porcentaje = porcentajes.getOrDefault(idCandidato, 0.0);
                
                resumen.append("Candidato ").append(idCandidato)
                       .append(": ").append(votos).append(" votos")
                       .append(" (").append(String.format("%.2f%%", porcentaje)).append(")\n");
            }
            
            return resumen.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
}
