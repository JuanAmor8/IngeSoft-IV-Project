#pragma once

module VotacionElectronica {
    /** Secuencia de bytes para transmisión de datos binarios */
    sequence<byte> ByteSeq;
    
    /**
     * Interfaz para el receptor de votos en el servidor central.
     */
    interface ReceptorVotos {
        /**
         * Recibe un voto desde una estación de votación.
         * 
         * @param idVoto Identificador único del voto
         * @param idMesa Identificador de la mesa de votación
         * @param timestamp Fecha y hora de emisión del voto
         * @param datosEncriptados Datos del voto encriptados
         * @param firma Firma digital para verificar integridad
         * @param clavePublicaEstacion Clave pública de la estación en Base64
         * @return true si el voto fue recibido correctamente, false en caso contrario
         */
        bool recibirVoto(string idVoto, string idMesa, string timestamp, 
                        ByteSeq datosEncriptados, ByteSeq firma, 
                        string clavePublicaEstacion);
        
        /**
         * Verifica el estado de conexión con el servidor.
         * 
         * @return true si el servidor está operativo
         */
        bool ping();
        
        /**
         * Obtiene la clave pública del servidor en formato Base64.
         * 
         * @return Clave pública del servidor
         */
        string obtenerClavePublica();
    };
}; 