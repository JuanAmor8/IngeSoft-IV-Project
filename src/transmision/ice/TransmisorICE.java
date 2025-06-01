package transmision.ice;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Connection;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import transmision.modelo.Voto;
import transmision.util.AlmacenTemporal;
import transmision.util.CifradorVotos;
import transmision.util.RegistroAuditoria;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Clase responsable de la transmisión de votos al servidor central mediante ICE.
 * Implementa el patrón Singleton para garantizar una única instancia.
 * Implementa el patrón Circuit Breaker para manejar fallos en la comunicación.
 */
public class TransmisorICE {
    
    private static TransmisorICE instancia;
    private final Communicator comunicador;
    private final String endpointServidor;
    private final AlmacenTemporal almacenTemporal;
    private final CifradorVotos cifradorVotos;
    private final RegistroAuditoria registroAuditoria;
    private final ScheduledExecutorService servicioRetransmision;
    private final Map<UUID, Boolean> confirmacionesRecibidas;
    
    // Parámetros del Circuit Breaker
    private int fallosConsecutivos;
    private boolean circuitoAbierto;
    private long tiempoReintentoMs;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     * 
     * @param endpointServidor Endpoint del servidor central
     */
    private TransmisorICE(String endpointServidor) {
        this.endpointServidor = endpointServidor;
        this.comunicador = Util.initialize(new String[0]);
        this.almacenTemporal = AlmacenTemporal.getInstancia();
        this.cifradorVotos = CifradorVotos.getInstancia();
        this.registroAuditoria = RegistroAuditoria.getInstancia();
        this.confirmacionesRecibidas = new ConcurrentHashMap<>();
        
        // Inicializar Circuit Breaker
        this.fallosConsecutivos = 0;
        this.circuitoAbierto = false;
        this.tiempoReintentoMs = 5000; // 5 segundos inicialmente
        
        // Iniciar servicio de retransmisión
        this.servicioRetransmision = Executors.newSingleThreadScheduledExecutor();
        this.servicioRetransmision.scheduleAtFixedRate(
            this::retransmitirVotosPendientes, 
            30, 60, TimeUnit.SECONDS
        );
        
        // Iniciar servicio de verificación de confirmaciones
        this.servicioRetransmision.scheduleAtFixedRate(
            this::verificarConfirmaciones, 
            45, 90, TimeUnit.SECONDS
        );
    }
    
    /**
     * Obtiene la única instancia de TransmisorICE (patrón Singleton).
     * 
     * @param endpointServidor Endpoint del servidor central
     * @return Instancia de TransmisorICE
     */
    public static synchronized TransmisorICE getInstancia(String endpointServidor) {
        if (instancia == null) {
            instancia = new TransmisorICE(endpointServidor);
        }
        return instancia;
    }
    
    /**
     * Transmite un voto al servidor central.
     * 
     * @param voto Voto a transmitir
     * @return true si la transmisión fue exitosa, false en caso contrario
     */
    public boolean transmitirVoto(Voto voto) {
        // Si el circuito está abierto, almacenar y retornar
        if (circuitoAbierto) {
            registroAuditoria.info("Circuito abierto, almacenando voto para retransmisión: " + voto.getId());
            return almacenTemporal.almacenarVoto(voto);
        }
        
        try {
            // Cifrar y firmar el voto si aún no lo está
            if (voto.getDatosEncriptados() == null || voto.getFirma() == null) {
                voto = cifradorVotos.cifrarVoto(voto);
            }
            
            // Almacenar temporalmente antes de transmitir
            almacenTemporal.almacenarVoto(voto);
            
            // Obtener proxy al servidor
            ObjectPrx base = comunicador.stringToProxy("ReceptorVotos:" + endpointServidor);
            ReceptorVotosPrx receptor = ReceptorVotosPrx.checkedCast(base);
            
            if (receptor == null) {
                throw new RuntimeException("Proxy inválido para ReceptorVotos");
            }
            
            // Transmitir voto
            boolean resultado = receptor.recibirVoto(
                voto.getId().toString(),
                voto.getIdMesa(),
                voto.getTimestamp().toString(),
                voto.getDatosEncriptados(),
                voto.getFirma(),
                cifradorVotos.getClavePublicaBase64()
            );
            
            // Procesar resultado
            if (resultado) {
                // Éxito: resetear Circuit Breaker y marcar como transmitido
                fallosConsecutivos = 0;
                circuitoAbierto = false;
                tiempoReintentoMs = 5000;
                
                // Registrar confirmación de recepción
                confirmacionesRecibidas.put(voto.getId(), true);
                
                almacenTemporal.marcarVotoTransmitido(voto.getId());
                registroAuditoria.registrarTransmision(voto.getId().toString(), voto.getIdMesa(), true);
                
                return true;
            } else {
                // Fallo lógico: mantener en almacén temporal
                registroAuditoria.advertencia("Fallo lógico al transmitir voto: " + voto.getId());
                return false;
            }
            
        } catch (Exception e) {
            // Fallo de comunicación: actualizar Circuit Breaker
            fallosConsecutivos++;
            registroAuditoria.error("Error al transmitir voto: " + voto.getId(), e);
            
            // Si superamos el umbral, abrir el circuito
            if (fallosConsecutivos >= 3) {
                circuitoAbierto = true;
                
                // Programar cierre del circuito después de un tiempo
                programarCierreCircuito();
            }
            
            return false;
        }
    }
    
    /**
     * Programa el cierre del circuito después de un tiempo de espera.
     * Implementa backoff exponencial para los reintentos.
     */
    private void programarCierreCircuito() {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            registroAuditoria.info("Intentando cerrar circuito después de " + tiempoReintentoMs + "ms");
            circuitoAbierto = false;
            
            // Duplicar el tiempo para el próximo reintento (backoff exponencial)
            tiempoReintentoMs = Math.min(tiempoReintentoMs * 2, 300000); // Máximo 5 minutos
        }, tiempoReintentoMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Retransmite los votos pendientes almacenados temporalmente.
     * Este método se ejecuta periódicamente.
     */
    private void retransmitirVotosPendientes() {
        if (circuitoAbierto) {
            registroAuditoria.info("Circuito abierto, posponiendo retransmisión de votos pendientes");
            return;
        }
        
        List<Voto> votosPendientes = almacenTemporal.getVotosPendientes();
        
        if (!votosPendientes.isEmpty()) {
            registroAuditoria.info("Retransmitiendo " + votosPendientes.size() + " votos pendientes");
            
            for (Voto voto : votosPendientes) {
                transmitirVoto(voto);
                
                // Si el circuito se abre durante la retransmisión, detener
                if (circuitoAbierto) {
                    registroAuditoria.info("Circuito abierto durante retransmisión, deteniendo proceso");
                    break;
                }
            }
        }
    }
    
    /**
     * Verifica que todos los votos transmitidos hayan sido confirmados.
     * Si no hay confirmación, programa una retransmisión.
     */
    private void verificarConfirmaciones() {
        try {
            List<Voto> votosTransmitidos = almacenTemporal.getVotosTransmitidos();
            for (Voto voto : votosTransmitidos) {
                // Si no hay confirmación para un voto transmitido, programar retransmisión
                if (!confirmacionesRecibidas.getOrDefault(voto.getId(), false)) {
                    registroAuditoria.advertencia("Voto transmitido sin confirmación: " + voto.getId() + ". Programando retransmisión.");
                    almacenTemporal.marcarVotoPendiente(voto.getId());
                }
            }
        } catch (Exception e) {
            registroAuditoria.error("Error al verificar confirmaciones de votos", e);
        }
    }
    
    /**
     * Registra una confirmación de recepción del servidor central.
     * 
     * @param idVoto Identificador del voto confirmado
     */
    public void registrarConfirmacion(UUID idVoto) {
        confirmacionesRecibidas.put(idVoto, true);
        registroAuditoria.info("Confirmación recibida para voto: " + idVoto);
    }
    
    /**
     * Cierra el transmisor y libera recursos.
     */
    public void cerrar() {
        try {
            // Detener servicio de retransmisión
            servicioRetransmision.shutdown();
            servicioRetransmision.awaitTermination(1, TimeUnit.MINUTES);
            
            // Cerrar comunicador ICE
            if (comunicador != null) {
                comunicador.destroy();
            }
        } catch (Exception e) {
            registroAuditoria.error("Error al cerrar el transmisor ICE", e);
        }
    }
}
