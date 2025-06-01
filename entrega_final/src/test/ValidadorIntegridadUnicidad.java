package test;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import transmision.modelo.Voto;
import transmision.modelo.Votante;
import transmision.util.AlmacenTemporal;
import transmision.util.CifradorVotos;
import transmision.util.RegistroAuditoria;
import transmision.util.ValidadorElegibilidad;
import transmision.util.ValidadorElegibilidad.ResultadoValidacion;

/**
 * Clase para validar la integridad y unicidad de los votos.
 * Implementa pruebas para verificar que el sistema cumple con los requisitos críticos.
 */
public class ValidadorIntegridadUnicidad {
    
    private static final int NUM_VOTOS_PRUEBA = 1000;
    private static final int NUM_HILOS = 10;
    
    /**
     * Método principal para ejecutar las pruebas de validación.
     */
    public static void main(String[] args) {
        System.out.println("Iniciando validación de integridad y unicidad de votos...");
        
        // Ejecutar pruebas
        probarUnicidadVotos();
        probarIntegridadVotos();
        probarConcurrencia();
        probarToleranciaFallos();
        
        System.out.println("Validación completada con éxito.");
    }
    
    /**
     * Prueba la unicidad de los votos, asegurando que no se dupliquen.
     */
    private static void probarUnicidadVotos() {
        System.out.println("\n=== Prueba de Unicidad de Votos ===");
        
        try {
            // Inicializar validador de elegibilidad
            ValidadorElegibilidad validador = ValidadorElegibilidad.getInstancia();
            
            // Crear votantes de prueba
            for (int i = 0; i < 10; i++) {
                String documento = "DOC" + i;
                String idMesa = "MESA1";
                
                // Crear votante
                Votante votante = new Votante(documento, idMesa);
                
                // Validar elegibilidad (primera vez)
                ResultadoValidacion resultado1 = validador.validarElegibilidad(votante, idMesa);
                System.out.println("Votante " + documento + " (1ra vez): " + 
                                  (resultado1.esElegible() ? "Elegible" : "No elegible - " + resultado1.getMotivo()));
                
                if (resultado1.esElegible()) {
                    // Registrar voto
                    validador.registrarVoto(documento, idMesa);
                    votante.marcarComoVotante();
                    
                    // Validar elegibilidad (segunda vez)
                    ResultadoValidacion resultado2 = validador.validarElegibilidad(votante, idMesa);
                    System.out.println("Votante " + documento + " (2da vez): " + 
                                      (resultado2.esElegible() ? "Elegible" : "No elegible - " + resultado2.getMotivo()));
                    
                    // Verificar que no sea elegible la segunda vez
                    if (resultado2.esElegible()) {
                        throw new AssertionError("Error: Votante " + documento + " pudo votar dos veces");
                    }
                }
            }
            
            System.out.println("Prueba de unicidad completada con éxito.");
        } catch (Exception e) {
            System.err.println("Error en prueba de unicidad: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Prueba la integridad de los votos, asegurando que el cifrado y firma funcionen correctamente.
     */
    private static void probarIntegridadVotos() {
        System.out.println("\n=== Prueba de Integridad de Votos ===");
        
        try {
            // Inicializar cifrador
            CifradorVotos cifrador = CifradorVotos.getInstancia();
            
            // Crear voto de prueba
            Voto voto = new Voto("MESA1", "CANDIDATO1");
            
            // Cifrar y firmar voto
            Voto votoCifrado = cifrador.cifrarVoto(voto);
            
            // Verificar que tenga datos encriptados y firma
            if (votoCifrado.getDatosEncriptados() == null) {
                throw new AssertionError("Error: Voto sin datos encriptados");
            }
            
            if (votoCifrado.getFirma() == null) {
                throw new AssertionError("Error: Voto sin firma digital");
            }
            
            // Verificar firma
            boolean firmaValida = cifrador.verificarFirma(votoCifrado);
            System.out.println("Firma válida: " + firmaValida);
            
            if (!firmaValida) {
                throw new AssertionError("Error: Verificación de firma fallida");
            }
            
            // Modificar datos y verificar que la firma falle
            byte[] datosOriginales = votoCifrado.getDatosEncriptados().clone();
            byte[] datosModificados = votoCifrado.getDatosEncriptados().clone();
            datosModificados[0] = (byte) (datosModificados[0] + 1); // Modificar un byte
            
            votoCifrado.setDatosEncriptados(datosModificados);
            boolean firmaTamperada = cifrador.verificarFirma(votoCifrado);
            System.out.println("Firma con datos modificados: " + firmaTamperada);
            
            if (firmaTamperada) {
                throw new AssertionError("Error: La verificación de firma no detectó la modificación de datos");
            }
            
            // Restaurar datos originales
            votoCifrado.setDatosEncriptados(datosOriginales);
            
            System.out.println("Prueba de integridad completada con éxito.");
        } catch (Exception e) {
            System.err.println("Error en prueba de integridad: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Prueba la concurrencia, asegurando que el sistema funcione correctamente bajo carga.
     */
    private static void probarConcurrencia() {
        System.out.println("\n=== Prueba de Concurrencia ===");
        
        try {
            final CountDownLatch latch = new CountDownLatch(NUM_HILOS);
            final AtomicInteger votosExitosos = new AtomicInteger(0);
            final AtomicInteger votosFallidos = new AtomicInteger(0);
            
            // Crear pool de hilos
            ExecutorService executor = Executors.newFixedThreadPool(NUM_HILOS);
            
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
                            // Crear y procesar voto
                            String idMesa = "MESA" + (j % 10);
                            String idCandidato = "CANDIDATO" + (j % 5);
                            
                            Voto voto = new Voto(idMesa, idCandidato);
                            CifradorVotos cifrador = CifradorVotos.getInstancia();
                            AlmacenTemporal almacen = AlmacenTemporal.getInstancia();
                            
                            // Cifrar voto
                            voto = cifrador.cifrarVoto(voto);
                            
                            // Almacenar voto
                            boolean resultado = almacen.almacenarVoto(voto);
                            
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
            
            // Verificar resultados
            System.out.println("Votos procesados exitosamente: " + votosExitosos.get());
            System.out.println("Votos con error: " + votosFallidos.get());
            
            if (votosExitosos.get() != NUM_VOTOS_PRUEBA) {
                throw new AssertionError("Error: No todos los votos fueron procesados correctamente");
            }
            
            System.out.println("Prueba de concurrencia completada con éxito.");
        } catch (Exception e) {
            System.err.println("Error en prueba de concurrencia: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Prueba la tolerancia a fallos, asegurando que el sistema recupere votos ante fallos.
     */
    private static void probarToleranciaFallos() {
        System.out.println("\n=== Prueba de Tolerancia a Fallos ===");
        
        try {
            // Inicializar componentes
            AlmacenTemporal almacen = AlmacenTemporal.getInstancia();
            CifradorVotos cifrador = CifradorVotos.getInstancia();
            
            // Crear votos de prueba
            for (int i = 0; i < 10; i++) {
                Voto voto = new Voto("MESA_FALLO", "CANDIDATO" + i);
                voto = cifrador.cifrarVoto(voto);
                almacen.almacenarVoto(voto);
            }
            
            // Obtener votos pendientes
            int votosPendientes = almacen.getVotosPendientes().size();
            System.out.println("Votos pendientes antes de simular fallo: " + votosPendientes);
            
            // Simular cierre y reinicio del sistema
            almacen.cerrar();
            
            // Crear nueva instancia (simulando reinicio)
            AlmacenTemporal nuevoAlmacen = AlmacenTemporal.getInstancia();
            
            // Verificar recuperación de votos
            int votosRecuperados = nuevoAlmacen.getVotosPendientes().size();
            System.out.println("Votos recuperados después de simular fallo: " + votosRecuperados);
            
            if (votosRecuperados < votosPendientes) {
                throw new AssertionError("Error: No se recuperaron todos los votos pendientes");
            }
            
            System.out.println("Prueba de tolerancia a fallos completada con éxito.");
        } catch (Exception e) {
            System.err.println("Error en prueba de tolerancia a fallos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
