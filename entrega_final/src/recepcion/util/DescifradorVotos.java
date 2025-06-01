package recepcion.util;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import recepcion.modelo.VotoRecibido;

/**
 * Clase responsable del descifrado de votos recibidos.
 * Implementa el patrón Singleton para garantizar una única instancia.
 */
public class DescifradorVotos {
    
    private static DescifradorVotos instancia;
    private final KeyPair parClaves;
    private final Map<String, SecretKey> clavesEstaciones;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     * Inicializa las claves de descifrado.
     */
    private DescifradorVotos() {
        try {
            // Generar par de claves RSA para descifrado
            KeyPairGenerator generadorRSA = KeyPairGenerator.getInstance("RSA");
            generadorRSA.initialize(2048);
            this.parClaves = generadorRSA.generateKeyPair();
            
            // Inicializar mapa de claves de estaciones
            this.clavesEstaciones = new HashMap<>();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al inicializar el descifrador: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene la única instancia de DescifradorVotos (patrón Singleton).
     * 
     * @return Instancia de DescifradorVotos
     */
    public static synchronized DescifradorVotos getInstancia() {
        if (instancia == null) {
            instancia = new DescifradorVotos();
        }
        return instancia;
    }
    
    /**
     * Registra una clave AES de una estación de votación.
     * 
     * @param idEstacion Identificador de la estación
     * @param claveAESCifradaBase64 Clave AES cifrada con la clave pública del servidor en Base64
     * @return true si se registró correctamente, false en caso contrario
     */
    public boolean registrarClaveEstacion(String idEstacion, String claveAESCifradaBase64) {
        try {
            // Decodificar clave AES cifrada
            byte[] claveAESCifrada = Base64.getDecoder().decode(claveAESCifradaBase64);
            
            // Descifrar clave AES con clave privada del servidor
            Cipher descifrador = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            descifrador.init(Cipher.DECRYPT_MODE, parClaves.getPrivate());
            byte[] claveAESBytes = descifrador.doFinal(claveAESCifrada);
            
            // Reconstruir clave AES
            SecretKey claveAES = new SecretKeySpec(claveAESBytes, "AES");
            
            // Almacenar clave AES
            clavesEstaciones.put(idEstacion, claveAES);
            
            return true;
        } catch (Exception e) {
            System.err.println("Error al registrar clave de estación: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Descifra un voto recibido utilizando la clave AES de la estación correspondiente.
     * 
     * @param voto Voto a descifrar
     * @param idEstacion Identificador de la estación
     * @return true si se descifró correctamente, false en caso contrario
     */
    public boolean descifrarVoto(VotoRecibido voto, String idEstacion) {
        try {
            // Obtener clave AES de la estación
            SecretKey claveAES = clavesEstaciones.get(idEstacion);
            if (claveAES == null) {
                throw new IllegalStateException("No se ha registrado la clave para la estación: " + idEstacion);
            }
            
            // Extraer IV y datos cifrados
            byte[] datosCifrados = voto.getDatosEncriptados();
            byte[] iv = new byte[16];
            byte[] datosReales = new byte[datosCifrados.length - 16];
            
            System.arraycopy(datosCifrados, 0, iv, 0, 16);
            System.arraycopy(datosCifrados, 16, datosReales, 0, datosReales.length);
            
            // Descifrar con AES
            Cipher descifrador = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            descifrador.init(Cipher.DECRYPT_MODE, claveAES, ivSpec);
            byte[] datosPlanos = descifrador.doFinal(datosReales);
            
            // Establecer ID del candidato
            String idCandidato = new String(datosPlanos);
            voto.setIdCandidato(idCandidato);
            
            return true;
        } catch (Exception e) {
            System.err.println("Error al descifrar voto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Obtiene la clave pública del servidor en formato Base64.
     * 
     * @return Clave pública del servidor en Base64
     */
    public String getClavePublicaBase64() {
        return Base64.getEncoder().encodeToString(parClaves.getPublic().getEncoded());
    }
}
