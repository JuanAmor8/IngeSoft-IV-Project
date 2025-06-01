package test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

import recepcion.modelo.VotoRecibido;
import recepcion.util.ConsolidadorResultados;
import recepcion.util.DescifradorVotos;
import recepcion.util.DetectorDuplicados;
import recepcion.util.RegistroAuditoriaServidor;
import recepcion.util.VerificadorIntegridad;

/**
 * Clase para realizar pruebas de funcionamiento y seguridad del sistema de votación.
 * Implementa pruebas para verificar el correcto funcionamiento bajo diferentes escenarios.
 */
public class PruebasFuncionamientoSeguridad {
    
    private static final int NUM_VOTOS_PRUEBA = 5000;
    private static final int NUM_HILOS = 20;
    
    /**
     * Método principal para ejecutar las pruebas de funcionamiento y seguridad.
     */
    public static void main(String[] args) {
        System.out.println("Iniciando pruebas de funcionamiento y seguridad...");
        
        // Ejecutar pruebas
        probarCargaMasiva();
        probarDeteccionDuplicados();
        probarConsolidacionResultados();
        probarSeguridadIntegridad();
        
        System.out.println("Pruebas completadas con éxito.");
    }
    
    /**
     * Prueba de carga masiva para verificar el rendimiento del sistema.
     */
    private static void probarCargaMasiva() {
        System.out.println("\n=== Prueba de Carga Masiva ===");
        
        try {
            final CountDownLatch latch = new CountDownLatch(NUM_HILOS);
            final AtomicInteger votosExitosos = new AtomicInteger(0);
            final AtomicInteger votosFallidos = new AtomicInteger(0);
            
            // Inicializar componentes
            final DetectorDuplicados detector = DetectorDuplicados.getInstancia();
            final RegistroAuditoriaServidor registro = RegistroAuditoriaServidor.getInstancia();
            
            // Crear pool de hilos
            ExecutorService executor = Executors.newFixedThreadPool(NUM_HILOS);
            
            // Registrar tiempo de inicio
            long tiempoInicio = System.currentTimeMillis();
            
            // Iniciar hilos
            for (int i = 0; i < NUM_HILOS; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        // Cada hilo procesa un conjunto de votos
                        int votosPerThread = NUM_VOTOS_PRUEBA / NUM_HILOS;
                        int inicio = threadId * votosPerThread;
                        int fin = inicio + votosPerThread;
                        
                        for (int j = inicio; j < fin; j++) {
                            // Generar UUID único para cada voto
                            UUID idVoto = UUID.randomUUID();
                            String idMesa = "MESA" + (j % 50);
                            
                            // Verificar y registrar voto
                            boolean resultado = detector.verificarYRegistrar(idVoto, idMesa);
                            
                            if (resultado) {
                                votosExitosos.incrementAndGet();
                            } else {
                                votosFallidos.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error en hilo " + threadId + ": " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Esperar a que todos los hilos terminen
            latch.await();
            executor.shutdown();
            
            // Calcular tiempo total
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
            
            // Verificar resultados
            System.out.println("Votos procesados exitosamente: " + votosExitosos.get());
            System.out.println("Votos con error: " + votosFallidos.get());
            System.out.println("Tiempo total: " + tiempoTotal + " ms");
            System.out.println("Votos por segundo: " + (NUM_VOTOS_PRUEBA * 1000.0 / tiempoTotal));
            
            if (votosExitosos.get() != NUM_VOTOS_PRUEBA) {
                throw new AssertionError("Error: No todos los votos fueron procesados correctamente");
            }
            
            System.out.println("Prueba de carga masiva completada con éxito.");
        } catch (Exception e) {
            System.err.println("Error en prueba de carga masiva: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Prueba de detección de duplicados para verificar que no se contabilicen votos duplicados.
     */
    private static void probarDeteccionDuplicados() {
        System.out.println("\n=== Prueba de Detección de Duplicados ===");
        
        try {
            // Inicializar detector de duplicados
            DetectorDuplicados detector = DetectorDuplicados.getInstancia();
            
            // Generar votos con IDs duplicados
            for (int i = 0; i < 10; i++) {
                UUID idVoto = UUID.randomUUID();
                String idMesa = "MESA_TEST";
                
                // Primera vez (debe aceptar)
                boolean resultado1 = detector.verificarYRegistrar(idVoto, idMesa);
                System.out.println("Voto " + idVoto + " (1ra vez): " + 
                                  (resultado1 ? "Aceptado" : "Rechazado"));
                
                // Segunda vez (debe rechazar)
                boolean resultado2 = detector.verificarYRegistrar(idVoto, idMesa);
                System.out.println("Voto " + idVoto + " (2da vez): " + 
                                  (resultado2 ? "Aceptado" : "Rechazado"));
                
                if (resultado2) {
                    throw new AssertionError("Error: Voto duplicado no fue detectado");
                }
            }
            
            System.out.println("Prueba de detección de duplicados completada con éxito.");
        } catch (Exception e) {
            System.err.println("Error en prueba de detección de duplicados: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Prueba de consolidación de resultados para verificar la correcta contabilización de votos.
     */
    private static void probarConsolidacionResultados() {
        System.out.println("\n=== Prueba de Consolidación de Resultados ===");
        
        try {
            // Inicializar consolidador
            ConsolidadorResultados consolidador = ConsolidadorResultados.getInstancia();
            
            // Establecer total de votantes
            consolidador.setTotalVotantesRegistrados(1000);
            
            // Generar votos para diferentes candidatos
            int[] votosPorCandidato = new int[5];
            
            for (int i = 0; i < 100; i++) {
                // Crear voto
                UUID idVoto = UUID.randomUUID();
                String idMesa = "MESA_CONSOLIDACION";
                java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
                
                // Seleccionar candidato (distribuir entre 5 candidatos)
                int indiceCandidato = i % 5;
                String idCandidato = "CANDIDATO" + indiceCandidato;
                votosPorCandidato[indiceCandidato]++;
                
                // Crear voto recibido
                VotoRecibido voto = new VotoRecibido(idVoto, idMesa, timestamp, new byte[0], new byte[0]);
                voto.setIdCandidato(idCandidato);
                
                // Registrar voto recibido
                consolidador.registrarVotoRecibido();
                
                // Contabilizar voto
                boolean resultado = consolidador.contabilizarVoto(voto);
                
                if (!resultado) {
                    throw new AssertionError("Error: No se pudo contabilizar el voto");
                }
            }
            
            // Verificar resultados
            System.out.println("Total votos recibidos: " + consolidador.getTotalVotosRecibidos());
            System.out.println("Total votos contabilizados: " + consolidador.getTotalVotosContabilizados());
            System.out.println("Porcentaje de participación: " + consolidador.getPorcentajeParticipacion() + "%");
            
            // Verificar distribución por candidato
            Map<String, Integer> resultados = consolidador.getResultadosPorCandidato();
            for (int i = 0; i < 5; i++) {
                String idCandidato = "CANDIDATO" + i;
                int votosEsperados = votosPorCandidato[i];
                int votosContabilizados = resultados.getOrDefault(idCandidato, 0);
                
                System.out.println("Candidato " + idCandidato + ": " + votosContabilizados + " votos");
                
                if (votosContabilizados != votosEsperados) {
                    throw new AssertionError("Error: Votos contabilizados incorrectamente para " + idCandidato);
                }
            }
            
            // Mostrar resumen
            System.out.println("\nResumen de resultados:");
            System.out.println(consolidador.getResumenResultados());
            
            System.out.println("Prueba de consolidación de resultados completada con éxito.");
        } catch (Exception e) {
            System.err.println("Error en prueba de consolidación de resultados: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Prueba de seguridad e integridad para verificar la resistencia a manipulaciones.
     */
    private static void probarSeguridadIntegridad() {
        System.out.println("\n=== Prueba de Seguridad e Integridad ===");
        
        try {
            // Inicializar componentes
            DescifradorVotos descifrador = DescifradorVotos.getInstancia();
            VerificadorIntegridad verificador = VerificadorIntegridad.getInstancia();
            
            // Obtener clave pública del servidor
            String clavePublicaServidor = descifrador.getClavePublicaBase64();
            System.out.println("Clave pública del servidor generada: " + clavePublicaServidor.substring(0, 20) + "...");
            
            // Simular registro de clave pública de estación
            String idEstacion = "ESTACION_SEGURIDAD";
            
            // Generar par de claves para la estación (simulado)
            java.security.KeyPairGenerator generador = java.security.KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            java.security.KeyPair parClaves = generador.generateKeyPair();
            
            // Convertir clave pública a Base64
            String clavePublicaEstacion = java.util.Base64.getEncoder().encodeToString(
                parClaves.getPublic().getEncoded());
            
            // Registrar clave pública
            boolean registroExitoso = verificador.registrarClavePublicaEstacion(idEstacion, clavePublicaEstacion);
            System.out.println("Registro de clave pública: " + (registroExitoso ? "Exitoso" : "Fallido"));
            
            if (!registroExitoso) {
                throw new AssertionError("Error: No se pudo registrar la clave pública de la estación");
            }
            
            // Crear firma para un voto (simulado)
            UUID idVoto = UUID.randomUUID();
            String idMesa = idEstacion;
            java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
            
            // Datos a firmar
            String datosAFirmar = idVoto.toString() + idMesa + timestamp.toString();
            
            // Crear firma
            java.security.Signature firmador = java.security.Signature.getInstance("SHA256withRSA");
            firmador.initSign(parClaves.getPrivate());
            firmador.update(datosAFirmar.getBytes());
            byte[] firma = firmador.sign();
            
            // Crear voto recibido con la firma
            VotoRecibido voto = new VotoRecibido(idVoto, idMesa, timestamp, new byte[10], firma);
            
            // Verificar firma (debe ser válida)
            boolean firmaValida = verificador.verificarFirma(voto, idEstacion);
            System.out.println("Verificación de firma válida: " + firmaValida);
            
            if (!firmaValida) {
                throw new AssertionError("Error: No se pudo verificar una firma válida");
            }
            
            // Modificar firma y verificar nuevamente (debe fallar)
            byte[] firmaModificada = firma.clone();
            firmaModificada[0] = (byte) (firmaModificada[0] + 1);
            
            VotoRecibido votoModificado = new VotoRecibido(idVoto, idMesa, timestamp, new byte[10], firmaModificada);
            
            boolean firmaInvalida = verificador.verificarFirma(votoModificado, idEstacion);
            System.out.println("Verificación de firma inválida: " + firmaInvalida);
            
            if (firmaInvalida) {
                throw new AssertionError("Error: Se verificó como válida una firma manipulada");
            }
            
            System.out.println("Prueba de seguridad e integridad completada con éxito.");
        } catch (Exception e) {
            System.err.println("Error en prueba de seguridad e integridad: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
