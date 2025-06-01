package transmision.util;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.Base64;
import transmision.modelo.Voto;

/**
 * Clase responsable del cifrado y firma digital de los votos.
 * Implementa el patrón Singleton para garantizar una única instancia.
 */
public class CifradorVotos {
    
    private static CifradorVotos instancia;
    private KeyPair parClaves;
    private SecretKey claveAES;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     * Inicializa las claves de cifrado y firma.
     */
    private CifradorVotos() {
        try {
            // Generar par de claves RSA para firma digital
            KeyPairGenerator generadorRSA = KeyPairGenerator.getInstance("RSA");
            generadorRSA.initialize(2048);
            this.parClaves = generadorRSA.generateKeyPair();
            
            // Generar clave AES para cifrado simétrico
            KeyGenerator generadorAES = KeyGenerator.getInstance("AES");
            generadorAES.init(256);
            this.claveAES = generadorAES.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al inicializar el cifrador: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene la única instancia de CifradorVotos (patrón Singleton).
     * 
     * @return Instancia de CifradorVotos
     */
    public static synchronized CifradorVotos getInstancia() {
        if (instancia == null) {
            instancia = new CifradorVotos();
        }
        return instancia;
    }
    
    /**
     * Cifra los datos del voto utilizando AES.
     * 
     * @param voto Voto a cifrar
     * @return Voto con datos cifrados
     */
    public Voto cifrarVoto(Voto voto) {
        try {
            // Preparar datos a cifrar (ID candidato)
            String datosPlanos = voto.getIdCandidato();
            
            // Cifrar con AES
            Cipher cifrador = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16]; // Vector de inicialización
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            cifrador.init(Cipher.ENCRYPT_MODE, claveAES, ivSpec);
            byte[] datosCifrados = cifrador.doFinal(datosPlanos.getBytes());
            
            // Combinar IV y datos cifrados
            byte[] resultado = new byte[iv.length + datosCifrados.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(datosCifrados, 0, resultado, iv.length, datosCifrados.length);
            
            voto.setDatosEncriptados(resultado);
            
            // Firmar el voto
            return firmarVoto(voto);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar el voto: " + e.getMessage(), e);
        }
    }
    
    /**
     * Firma digitalmente el voto utilizando RSA.
     * 
     * @param voto Voto a firmar
     * @return Voto firmado
     */
    private Voto firmarVoto(Voto voto) {
        try {
            // Crear firma digital
            Signature firmador = Signature.getInstance("SHA256withRSA");
            firmador.initSign(parClaves.getPrivate());
            
            // Datos a firmar: ID voto + ID mesa + timestamp + datos cifrados
            String datosAFirmar = voto.getId().toString() + voto.getIdMesa() + 
                                 voto.getTimestamp().toString();
            
            firmador.update(datosAFirmar.getBytes());
            if (voto.getDatosEncriptados() != null) {
                firmador.update(voto.getDatosEncriptados());
            }
            
            byte[] firma = firmador.sign();
            voto.setFirma(firma);
            
            return voto;
        } catch (Exception e) {
            throw new RuntimeException("Error al firmar el voto: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica la firma digital de un voto.
     * 
     * @param voto Voto a verificar
     * @return true si la firma es válida, false en caso contrario
     */
    public boolean verificarFirma(Voto voto) {
        try {
            Signature verificador = Signature.getInstance("SHA256withRSA");
            verificador.initVerify(parClaves.getPublic());
            
            // Datos a verificar: ID voto + ID mesa + timestamp + datos cifrados
            String datosAVerificar = voto.getId().toString() + voto.getIdMesa() + 
                                    voto.getTimestamp().toString();
            
            verificador.update(datosAVerificar.getBytes());
            if (voto.getDatosEncriptados() != null) {
                verificador.update(voto.getDatosEncriptados());
            }
            
            return verificador.verify(voto.getFirma());
        } catch (Exception e) {
            throw new RuntimeException("Error al verificar la firma del voto: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene la clave pública para compartir con el servidor central.
     * 
     * @return Clave pública codificada en Base64
     */
    public String getClavePublicaBase64() {
        return Base64.getEncoder().encodeToString(parClaves.getPublic().getEncoded());
    }
    
    /**
     * Obtiene la clave AES cifrada con la clave pública del servidor.
     * 
     * @param clavePublicaServidorBase64 Clave pública del servidor en Base64
     * @return Clave AES cifrada en Base64
     */
    public String getClaveAESCifradaBase64(String clavePublicaServidorBase64) {
        try {
            // Decodificar clave pública del servidor
            byte[] bytesClavePublica = Base64.getDecoder().decode(clavePublicaServidorBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytesClavePublica);
            PublicKey clavePublicaServidor = keyFactory.generatePublic(keySpec);
            
            // Cifrar clave AES con clave pública del servidor
            Cipher cifrador = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cifrador.init(Cipher.ENCRYPT_MODE, clavePublicaServidor);
            byte[] claveAESCifrada = cifrador.doFinal(claveAES.getEncoded());
            
            return Base64.getEncoder().encodeToString(claveAESCifrada);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar la clave AES: " + e.getMessage(), e);
        }
    }
}
