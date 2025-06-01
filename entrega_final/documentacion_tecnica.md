# Documentación del Sistema de Transmisión y Recepción de Votos

## Introducción

Este documento proporciona la documentación técnica completa del sistema de transmisión y recepción de votos desarrollado para la Registraduría. El sistema está diseñado para garantizar que el 100% de los votos emitidos sean registrados correctamente y que ningún voto sea contado más de una vez, cumpliendo con los más altos estándares de seguridad, confiabilidad y auditabilidad.

## Arquitectura del Sistema

El sistema está compuesto por dos módulos principales:

1. **Módulo de Transmisión de Votos**: Implementado en las estaciones de votación, responsable de capturar, validar, cifrar y transmitir los votos al servidor central.

2. **Módulo de Recepción y Consolidación**: Implementado en el servidor central, responsable de recibir, verificar, descifrar y consolidar los votos provenientes de las estaciones.

La comunicación entre ambos módulos se realiza mediante el protocolo ICE (Internet Communications Engine), que proporciona una capa de comunicación eficiente y segura.

### Patrones de Diseño Implementados

El sistema implementa varios patrones de diseño para garantizar su robustez y mantenibilidad:

- **Patrón Singleton**: Utilizado en componentes críticos como gestores de cifrado, almacenamiento y registro para garantizar una única instancia.
- **Patrón Observer**: Implementado para la notificación de eventos entre componentes.
- **Patrón Strategy**: Utilizado para implementar diferentes estrategias de validación y cifrado.
- **Patrón Factory Method**: Empleado para la creación de objetos relacionados con la votación.
- **Patrón Command**: Aplicado para encapsular operaciones como la emisión de votos.
- **Patrón Circuit Breaker**: Implementado para gestionar fallos en la comunicación entre estaciones y servidor central.

### Mecanismos de Seguridad

El sistema implementa múltiples capas de seguridad:

- **Cifrado de Extremo a Extremo**: Los votos se cifran en la estación de origen y solo se descifran en el servidor central.
- **Firmas Digitales**: Cada voto incluye una firma digital que garantiza su autenticidad e integridad.
- **Validación de Identidad**: Verificación multifactor de la identidad del votante.
- **Prevención de Duplicados**: Implementación de Bloom Filters para verificación rápida de duplicados.

### Mecanismos de Tolerancia a Fallos

Para garantizar que ningún voto se pierda, el sistema implementa:

- **Almacenamiento Local Temporal**: Las estaciones mantienen un registro local cifrado de los votos emitidos.
- **Confirmación de Recepción**: El servidor central emite confirmaciones para cada voto recibido correctamente.
- **Replicación de Datos**: Los datos se replican en múltiples nodos del servidor central.
- **Mecanismo de Reintento**: Retransmisión automática de votos ante fallos de comunicación.

## Módulo de Transmisión de Votos

### Componentes Principales

#### Modelo de Datos

- **Voto.java**: Representa un voto emitido por un ciudadano, con identificador único, mesa, candidato, timestamp y firma digital.
- **Votante.java**: Representa a un votante en el sistema, con documento, mesa asignada y estado de votación.

#### Utilidades

- **CifradorVotos.java**: Responsable del cifrado y firma digital de los votos utilizando algoritmos RSA y AES.
- **AlmacenTemporal.java**: Proporciona almacenamiento temporal y persistente de votos para tolerancia a fallos.
- **ValidadorElegibilidad.java**: Valida la elegibilidad de los votantes según diversos criterios.
- **RegistroAuditoria.java**: Registra eventos para auditoría y trazabilidad.

#### Comunicación

- **TransmisorICE.java**: Gestiona la comunicación con el servidor central mediante ICE, implementando el patrón Circuit Breaker.
- **ReceptorVotos.java**: Interfaz ICE que define los métodos que debe implementar el servidor.

### Flujo de Transmisión

1. El votante se identifica en la estación.
2. El sistema valida su elegibilidad mediante `ValidadorElegibilidad`.
3. El votante emite su voto, creando una instancia de `Voto`.
4. El voto se cifra y firma mediante `CifradorVotos`.
5. El voto se almacena localmente mediante `AlmacenTemporal`.
6. El voto se transmite al servidor mediante `TransmisorICE`.
7. Si la transmisión falla, se programa un reintento automático.

## Módulo de Recepción y Consolidación

### Componentes Principales

#### Modelo de Datos

- **VotoRecibido.java**: Representa un voto recibido en el servidor, con estado de verificación y contabilización.

#### Utilidades

- **DescifradorVotos.java**: Responsable del descifrado de votos utilizando las claves correspondientes.
- **VerificadorIntegridad.java**: Verifica la autenticidad e integridad de los votos mediante firmas digitales.
- **DetectorDuplicados.java**: Previene la contabilización de votos duplicados mediante Bloom Filters.
- **ConsolidadorResultados.java**: Consolida los resultados de la votación y proporciona estadísticas.
- **RegistroAuditoriaServidor.java**: Registra eventos en el servidor para auditoría.

#### Comunicación

- **ReceptorVotosImpl.java**: Implementación del servicio ICE para recepción de votos.

### Flujo de Recepción

1. El servidor recibe un voto mediante `ReceptorVotosImpl`.
2. Se verifica que no sea un duplicado mediante `DetectorDuplicados`.
3. Se verifica la firma digital mediante `VerificadorIntegridad`.
4. Se descifra el voto mediante `DescifradorVotos`.
5. Se contabiliza el voto mediante `ConsolidadorResultados`.
6. Se registra la operación para auditoría mediante `RegistroAuditoriaServidor`.

## Pruebas y Validación

El sistema ha sido sometido a rigurosas pruebas para garantizar su correcto funcionamiento:

### Pruebas de Integridad y Unicidad

- **Unicidad de Votos**: Verificación de que un votante no pueda votar más de una vez.
- **Integridad de Votos**: Verificación de que los votos no puedan ser alterados.
- **Concurrencia**: Verificación del correcto funcionamiento bajo carga concurrente.
- **Tolerancia a Fallos**: Verificación de la recuperación ante fallos del sistema.

### Pruebas de Funcionamiento y Seguridad

- **Carga Masiva**: Verificación del rendimiento bajo carga masiva de votos.
- **Detección de Duplicados**: Verificación de la correcta detección de votos duplicados.
- **Consolidación de Resultados**: Verificación de la correcta contabilización de votos.
- **Seguridad e Integridad**: Verificación de la resistencia a manipulaciones.

## Consideraciones de Despliegue

Para desplegar el sistema, se deben seguir los siguientes pasos:

1. **Configuración del Servidor Central**:
   - Instalar Java JDK 11 o superior.
   - Configurar el servidor ICE.
   - Iniciar el servicio de recepción de votos.

2. **Configuración de las Estaciones de Votación**:
   - Instalar Java JDK 11 o superior.
   - Configurar el cliente ICE.
   - Establecer la conexión con el servidor central.

3. **Consideraciones de Seguridad**:
   - Utilizar conexiones seguras (SSL/TLS).
   - Implementar firewalls y sistemas de detección de intrusiones.
   - Realizar auditorías periódicas de seguridad.

## Conclusiones

El sistema de transmisión y recepción de votos implementado cumple con los requisitos críticos de garantizar que el 100% de los votos emitidos sean registrados correctamente y que ningún voto sea contado más de una vez. La arquitectura robusta, los mecanismos de seguridad y la tolerancia a fallos proporcionan una solución confiable y segura para el proceso electoral.
