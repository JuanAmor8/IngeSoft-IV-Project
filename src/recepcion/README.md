# Módulo de Recepción y Consolidación de Votos

Este módulo es responsable de recibir, validar, descifrar y consolidar los votos provenientes de las estaciones de votación, garantizando que cada voto sea contabilizado exactamente una vez.

## Características principales

- Recepción segura de votos mediante ICE
- Verificación de autenticidad e integridad mediante firmas digitales
- Prevención de duplicados mediante estructuras de datos eficientes
- Descifrado seguro de votos
- Consolidación de resultados en tiempo real
- Registro detallado para auditoría

## Estructura del módulo

- `ReceptorVotosImpl.java`: Implementación del servicio ICE para recepción de votos
- `ConsolidadorResultados.java`: Componente de consolidación y estadísticas
- `VerificadorIntegridad.java`: Componente de verificación de firmas y autenticidad
- `DetectorDuplicados.java`: Componente para prevención de duplicados
- `DescifradorVotos.java`: Componente para descifrado seguro
- `RegistroAuditoriaServidor.java`: Componente de registro para auditoría
- `GestorPersistencia.java`: Componente para almacenamiento persistente
