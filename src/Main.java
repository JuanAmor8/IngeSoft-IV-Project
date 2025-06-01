import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;
import recepcion.ice.ReceptorVotosImpl;
import recepcion.util.ConsolidadorResultados;
import recepcion.util.RegistroAuditoriaServidor;
import test.PruebasFuncionamientoSeguridad;
import transmision.ice.TransmisorICE;
import transmision.modelo.Voto;
import transmision.util.RegistroAuditoria;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

/**
 * Clase principal para iniciar el sistema de votación.
 * Permite probar la funcionalidad de transmisión y recepción de votos.
 */
public class Main {
    
    // Configuración por defecto
    private static final String CONFIG_FILE = "config.properties";
    private static final String DEFAULT_ENDPOINT = "tcp -h localhost -p 10000";
    private static String endpoint;
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("Sistema de Votación - Módulos de Transmisión y Recepción");
        System.out.println("===========================================");
        
        // Cargar configuración
        cargarConfiguracion();
        
        // Mostrar menú de opciones
        mostrarMenu();
    }
    
    /**
     * Carga la configuración desde el archivo properties
     */
    private static void cargarConfiguracion() {
        Properties props = new Properties();
        
        try {
            if (Files.exists(Paths.get(CONFIG_FILE))) {
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                    props.load(fis);
                    
                    // Cargar endpoint desde la configuración
                    endpoint = props.getProperty("ReceptorVotos.Endpoints", DEFAULT_ENDPOINT);
                    
                    System.out.println("Configuración cargada correctamente.");
                    System.out.println("Endpoint del servidor: " + endpoint);
                }
            } else {
                System.out.println("Archivo de configuración no encontrado. Usando valores predeterminados.");
                endpoint = DEFAULT_ENDPOINT;
            }
        } catch (Exception e) {
            System.err.println("Error al cargar configuración: " + e.getMessage());
            endpoint = DEFAULT_ENDPOINT;
        }
    }
    
    /**
     * Muestra el menú principal de opciones
     */
    private static void mostrarMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean salir = false;
        
        while (!salir) {
            System.out.println("\nSeleccione una opción:");
            System.out.println("1. Iniciar servidor de recepción");
            System.out.println("2. Probar transmisión de un voto");
            System.out.println("3. Ejecutar pruebas de funcionamiento y seguridad");
            System.out.println("4. Ver resultados consolidados");
            System.out.println("5. Salir");
            System.out.print("Opción: ");
            
            try {
                int opcion = Integer.parseInt(scanner.nextLine().trim());
                
                switch (opcion) {
                    case 1:
                        iniciarServidor();
                        break;
                    case 2:
                        probarTransmision(scanner);
                        break;
                    case 3:
                        ejecutarPruebas();
                        break;
                    case 4:
                        verResultados();
                        break;
                    case 5:
                        salir = true;
                        System.out.println("Finalizando aplicación...");
                        break;
                    default:
                        System.out.println("Opción no válida. Intente de nuevo.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Por favor, ingrese un número válido.");
            }
        }
        
        scanner.close();
    }
    
    /**
     * Inicia el servidor de recepción de votos
     */
    private static void iniciarServidor() {
        System.out.println("\n=== Iniciando Servidor de Recepción ===");
        
        // Crear hilo para servidor (para no bloquear la interfaz)
        Thread servidorThread = new Thread(() -> {
            try {
                System.out.println("Iniciando servidor en: " + endpoint);
                
                // Inicializar comunicador ICE
                Communicator comunicador = Util.initialize(new String[0]);
                
                // Crear adaptador y añadir implementación
                com.zeroc.Ice.ObjectAdapter adapter = comunicador.createObjectAdapterWithEndpoints(
                    "ReceptorVotosAdapter", endpoint);
                adapter.add(new ReceptorVotosImpl(), Util.stringToIdentity("ReceptorVotos"));
                adapter.activate();
                
                System.out.println("Servidor iniciado correctamente. Esperando conexiones...");
                System.out.println("Presione Ctrl+C para detener el servidor.");
                
                // Mantener el servidor activo
                comunicador.waitForShutdown();
            } catch (Exception e) {
                System.err.println("Error al iniciar servidor: " + e.getMessage());
            }
        });
        
        servidorThread.setDaemon(true); // Para que no bloquee el cierre de la aplicación
        servidorThread.start();
        
        System.out.println("Servidor iniciado en segundo plano.");
    }
    
    /**
     * Prueba la transmisión de un voto
     */
    private static void probarTransmision(Scanner scanner) {
        System.out.println("\n=== Prueba de Transmisión de Voto ===");
        
        try {
            // Solicitar datos para el voto
            System.out.print("Ingrese ID de la mesa de votación: ");
            String idMesa = scanner.nextLine().trim();
            
            System.out.print("Ingrese ID del candidato: ");
            String idCandidato = scanner.nextLine().trim();
            
            // Crear y transmitir voto
            Voto voto = new Voto(idMesa, idCandidato);
            System.out.println("Voto creado con ID: " + voto.getId());
            
            // Obtener instancia del transmisor
            TransmisorICE transmisor = TransmisorICE.getInstancia(endpoint);
            
            // Transmitir voto
            System.out.println("Transmitiendo voto...");
            boolean resultado = transmisor.transmitirVoto(voto);
            
            if (resultado) {
                System.out.println("Voto transmitido exitosamente.");
            } else {
                System.out.println("Fallo en la transmisión del voto. Revise los logs para más detalles.");
            }
        } catch (Exception e) {
            System.err.println("Error al transmitir voto: " + e.getMessage());
        }
    }
    
    /**
     * Ejecuta las pruebas de funcionamiento y seguridad
     */
    private static void ejecutarPruebas() {
        System.out.println("\n=== Ejecutando Pruebas de Funcionamiento y Seguridad ===");
        
        try {
            // Ejecutar pruebas
            PruebasFuncionamientoSeguridad.main(new String[0]);
        } catch (Exception e) {
            System.err.println("Error al ejecutar pruebas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Muestra los resultados consolidados
     */
    private static void verResultados() {
        System.out.println("\n=== Resultados Consolidados ===");
        
        try {
            // Obtener instancia del consolidador
            ConsolidadorResultados consolidador = ConsolidadorResultados.getInstancia();
            
            // Mostrar estadísticas
            System.out.println("Total votos recibidos: " + consolidador.getTotalVotosRecibidos());
            System.out.println("Total votos contabilizados: " + consolidador.getTotalVotosContabilizados());
            System.out.println("Porcentaje de participación: " + consolidador.getPorcentajeParticipacion() + "%");
            
            // Mostrar resultados por candidato
            System.out.println("\nResultados por candidato:");
            consolidador.getResultadosPorCandidato().forEach((candidato, votos) -> 
                System.out.println(candidato + ": " + votos + " votos"));
            
            // Mostrar resultados por mesa
            System.out.println("\nResultados por mesa de votación:");
            consolidador.getResultadosPorMesa().forEach((mesa, votos) -> 
                System.out.println(mesa + ": " + votos + " votos"));
            
        } catch (Exception e) {
            System.err.println("Error al obtener resultados: " + e.getMessage());
        }
    }
} 