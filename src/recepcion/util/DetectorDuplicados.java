package recepcion.util;

import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.BitSet;

/**
 * Clase responsable de detectar y prevenir votos duplicados.
 * Implementa el patrón Singleton para garantizar una única instancia.
 * Utiliza Bloom Filter para verificación rápida de duplicados.
 */
public class DetectorDuplicados {
    
    private static DetectorDuplicados instancia;
    private final Set<UUID> votosRecibidos;
    private final BloomFilter bloomFilter;
    private final RegistroAuditoriaServidor registroAuditoria;
    private final AtomicInteger contadorDuplicados;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     */
    private DetectorDuplicados() {
        this.votosRecibidos = ConcurrentHashMap.newKeySet();
        this.bloomFilter = new BloomFilter(10000000, 0.001); // 10 millones de elementos, 0.1% falsos positivos
        this.registroAuditoria = RegistroAuditoriaServidor.getInstancia();
        this.contadorDuplicados = new AtomicInteger(0);
    }
    
    /**
     * Obtiene la única instancia de DetectorDuplicados (patrón Singleton).
     * 
     * @return Instancia de DetectorDuplicados
     */
    public static synchronized DetectorDuplicados getInstancia() {
        if (instancia == null) {
            instancia = new DetectorDuplicados();
        }
        return instancia;
    }
    
    /**
     * Verifica si un voto es duplicado y lo registra si no lo es.
     * 
     * @param idVoto Identificador del voto
     * @param idMesa Identificador de la mesa
     * @return true si es un voto nuevo, false si es duplicado
     */
    public boolean verificarYRegistrar(UUID idVoto, String idMesa) {
        // Verificación rápida con Bloom Filter
        if (bloomFilter.mightContain(idVoto.toString())) {
            // Posible duplicado, verificar con exactitud
            if (votosRecibidos.contains(idVoto)) {
                // Es un duplicado confirmado
                contadorDuplicados.incrementAndGet();
                registroAuditoria.registrarVotoDuplicado(idVoto.toString(), idMesa);
                return false;
            }
        }
        
        // No es duplicado, registrarlo
        votosRecibidos.add(idVoto);
        bloomFilter.put(idVoto.toString());
        return true;
    }
    
    /**
     * Obtiene el número de votos duplicados detectados.
     * 
     * @return Contador de duplicados
     */
    public int getContadorDuplicados() {
        return contadorDuplicados.get();
    }
    
    /**
     * Clase interna que implementa un Bloom Filter para detección eficiente de duplicados.
     */
    private static class BloomFilter {
        private final BitSet bitSet;
        private final int numHashes;
        private final int bitSetSize;
        
        /**
         * Constructor del Bloom Filter.
         * 
         * @param expectedElements Número esperado de elementos
         * @param falsePositiveProbability Probabilidad aceptable de falsos positivos
         */
        public BloomFilter(int expectedElements, double falsePositiveProbability) {
            // Calcular tamaño óptimo del BitSet
            this.bitSetSize = optimalBitSetSize(expectedElements, falsePositiveProbability);
            this.bitSet = new BitSet(bitSetSize);
            
            // Calcular número óptimo de funciones hash
            this.numHashes = optimalNumHashes(expectedElements, bitSetSize);
        }
        
        /**
         * Agrega un elemento al Bloom Filter.
         * 
         * @param element Elemento a agregar
         */
        public void put(String element) {
            for (int i = 0; i < numHashes; i++) {
                int hash = hash(element, i);
                bitSet.set(Math.abs(hash % bitSetSize), true);
            }
        }
        
        /**
         * Verifica si un elemento podría estar en el Bloom Filter.
         * 
         * @param element Elemento a verificar
         * @return true si podría estar, false si definitivamente no está
         */
        public boolean mightContain(String element) {
            for (int i = 0; i < numHashes; i++) {
                int hash = hash(element, i);
                if (!bitSet.get(Math.abs(hash % bitSetSize))) {
                    return false;
                }
            }
            return true;
        }
        
        /**
         * Calcula un hash para el elemento y la semilla.
         * 
         * @param element Elemento a hashear
         * @param seed Semilla para la función hash
         * @return Valor hash
         */
        private int hash(String element, int seed) {
            int h = seed + element.hashCode();
            h ^= h >>> 16;
            h *= 0x85ebca6b;
            h ^= h >>> 13;
            h *= 0xc2b2ae35;
            h ^= h >>> 16;
            return h;
        }
        
        /**
         * Calcula el tamaño óptimo del BitSet.
         * 
         * @param n Número esperado de elementos
         * @param p Probabilidad aceptable de falsos positivos
         * @return Tamaño óptimo del BitSet
         */
        private int optimalBitSetSize(int n, double p) {
            return (int) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
        }
        
        /**
         * Calcula el número óptimo de funciones hash.
         * 
         * @param n Número esperado de elementos
         * @param m Tamaño del BitSet
         * @return Número óptimo de funciones hash
         */
        private int optimalNumHashes(int n, int m) {
            return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
        }
    }
}
