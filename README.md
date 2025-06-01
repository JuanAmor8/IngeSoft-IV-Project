# Sistema de Votación Electrónica

Este sistema implementa una solución para la transmisión y recepción segura de votos desde estaciones de votación hacia un servidor central de consolidación para la Registraduría Nacional.

## Estructura del Proyecto

El proyecto está organizado en los siguientes módulos:

```
/
├── src/                     # Código fuente
│   ├── com/zeroc/Ice/       # Implementación simulada de ICE
│   ├── java/                # Clases utilitarias 
│   ├── transmision/         # Módulo de transmisión de votos
│   │   ├── ice/             # Componentes de comunicación ICE
│   │   ├── modelo/          # Clases de modelo (Voto)
│   │   └── util/            # Utilidades (cifrado, almacenamiento)
│   ├── recepcion/           # Módulo de recepción de votos
│   │   ├── ice/             # Servidor ICE para recepción
│   │   ├── modelo/          # Clases de modelo (VotoRecibido)
│   │   └── util/            # Utilidades (descifrado, consolidación)
│   ├── test/                # Pruebas del sistema
│   └── Main.java            # Clase principal
├── bin/                     # Directorio de compilación
└── build.bat                # Script para compilar el proyecto
```

## Requisitos del Sistema

- JDK 11 o superior
- Sistema operativo: Windows, Linux o macOS
- Espacio en disco: 100 MB mínimo
- Memoria: 512 MB RAM mínimo

## Instalación y Ejecución

### Paso 1: Compilar el Proyecto

1. Abra una terminal de comandos (CMD en Windows o Terminal en Linux/Mac)
2. Navegue hasta el directorio raíz del proyecto
3. Ejecute el script de compilación:

   En Windows:
   ```
   .\build.bat
   ```

   En Linux/Mac:
   ```
   chmod +x build.sh    # Si existe build.sh, dar permisos de ejecución
   ./build.sh           # Ejecutar script de compilación
   ```

   Si no existe un script para Linux/Mac, compile manualmente:
   ```
   mkdir -p bin
   javac -d bin src/com/zeroc/Ice/*.java
   javac -d bin -cp bin src/transmision/ice/*.java src/transmision/modelo/*.java src/transmision/util/*.java
   javac -d bin -cp bin src/recepcion/ice/*.java src/recepcion/modelo/*.java src/recepcion/util/*.java
   javac -d bin -cp bin src/test/*.java
   javac -d bin -cp bin src/Main.java
   ```

### Paso 2: Ejecutar el Programa

Una vez compilado, ejecute el programa:

```
java -cp bin Main
```

### Paso 3: Usar el Sistema

El programa mostrará un menú interactivo con las siguientes opciones:

1. **Iniciar servidor de recepción**: Inicia el servidor que recibirá los votos.
2. **Probar transmisión de un voto**: Permite enviar un voto de prueba.
3. **Ejecutar pruebas de funcionamiento y seguridad**: Ejecuta pruebas automatizadas.
4. **Ver resultados consolidados**: Muestra los resultados de la votación.
5. **Salir**: Finaliza el programa.

## Flujo de Trabajo Recomendado

1. Inicie el servidor de recepción (opción 1)
2. En otra sesión o terminal, realice pruebas de transmisión (opción 2)
3. Verifique los resultados (opción 4)

## Configuración

Por defecto, el sistema utiliza la configuración:
- Endpoint del servidor: `tcp -h localhost -p 10000`

Para modificar esta configuración, cree un archivo `config.properties` en el directorio raíz con el siguiente contenido:

```
ReceptorVotos.Endpoints=tcp -h <dirección-ip> -p <puerto>
```

## Patrones de Diseño Implementados

El sistema implementa los siguientes patrones de diseño:

### 1. Patrón Singleton
Utilizado en clases como `TransmisorICE`, `AlmacenTemporal`, `ConsolidadorResultados` para garantizar instancia única.

### 2. Patrón Circuit Breaker
Implementado en `TransmisorICE` para manejar fallos de comunicación y proporcionar tolerancia a fallos.

### 3. Patrón Proxy
Implementado mediante la infraestructura ICE, particularmente en `ReceptorVotosPrx`.

### 4. Patrón Factory Method
Utilizado para la creación de objetos complejos, como en los métodos `checkedCast`.

### 5. Patrón Observer
Implementado de forma implícita en el sistema de auditoría y confirmaciones.

## Características de Seguridad

El sistema implementa:
- Cifrado asimétrico de votos
- Firmas digitales para verificación de integridad
- Detección de duplicados
- Tolerancia a fallos mediante almacenamiento temporal
- Auditoría completa de operaciones

## Solución de Problemas

Si encuentra algún problema:

1. Verifique que Java esté correctamente instalado: `java -version`
2. Asegúrese de que los directorios bin y src existan
3. Revise los logs de error en la consola

## Notas Adicionales

- Este sistema es una implementación simulada para entornos de prueba
- La implementación de ICE es mínima para permitir la compilación
- En un entorno real, se requeriría ZeroC ICE completo

## Créditos

Desarrollado para la Registraduría Nacional como parte del proyecto de votación electrónica. 




##Registraduria Nacional
#Juan Camilo Amorocho 
#Juan Esteban Ruiz
#Tomas Quintero
