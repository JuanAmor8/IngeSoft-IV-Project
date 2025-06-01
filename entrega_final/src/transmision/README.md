# Módulo de Transmisión de Votos

Este módulo es responsable de la captura, validación, cifrado y transmisión segura de los votos desde las estaciones de votación hacia el servidor central.

## Características principales

- Validación de elegibilidad del votante
- Cifrado de extremo a extremo de los votos
- Firma digital para garantizar autenticidad e integridad
- Almacenamiento local temporal para tolerancia a fallos
- Mecanismo de reintento automático ante fallos de red
- Registro detallado para auditoría

## Estructura del módulo

- `TransmisionVotos.java`: Clase principal del módulo
- `ValidadorElegibilidad.java`: Componente de validación de votantes
- `CifradorVotos.java`: Componente de cifrado y firma digital
- `AlmacenTemporal.java`: Componente de almacenamiento local
- `TransmisorICE.java`: Componente de comunicación mediante ICE
- `GestorSesion.java`: Componente de gestión de sesiones
- `RegistroAuditoria.java`: Componente de registro para auditoría
