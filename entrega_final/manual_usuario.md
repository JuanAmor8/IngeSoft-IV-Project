# Manual de Usuario - Sistema de Transmisión y Recepción de Votos

## Introducción

Este manual proporciona las instrucciones necesarias para la instalación, configuración y uso del sistema de transmisión y recepción de votos desarrollado para la Registraduría. El sistema garantiza que el 100% de los votos emitidos sean registrados correctamente y que ningún voto sea contado más de una vez.

## Requisitos del Sistema

### Servidor Central
- Sistema Operativo: Linux, Windows o macOS
- Java JDK 11 o superior
- Mínimo 8GB de RAM
- Espacio en disco: 100GB o superior
- Conexión a Internet estable

### Estaciones de Votación
- Sistema Operativo: Linux, Windows o macOS
- Java JDK 11 o superior
- Mínimo 4GB de RAM
- Espacio en disco: 20GB o superior
- Conexión a Internet estable

## Instalación

### Servidor Central

1. Descomprima el archivo `servidor_central.zip` en el directorio deseado.
2. Abra una terminal o línea de comandos y navegue hasta el directorio donde descomprimió los archivos.
3. Ejecute el siguiente comando para instalar las dependencias:
   ```
   ./instalar_dependencias.sh
   ```
   (En Windows, use `instalar_dependencias.bat`)

4. Configure los parámetros del servidor editando el archivo `config/servidor.properties`:
   ```
   puerto=10000
   max_conexiones=1000
   directorio_logs=/ruta/a/logs
   ```

5. Inicie el servidor con el siguiente comando:
   ```
   ./iniciar_servidor.sh
   ```
   (En Windows, use `iniciar_servidor.bat`)

### Estaciones de Votación

1. Descomprima el archivo `estacion_votacion.zip` en el directorio deseado.
2. Abra una terminal o línea de comandos y navegue hasta el directorio donde descomprimió los archivos.
3. Ejecute el siguiente comando para instalar las dependencias:
   ```
   ./instalar_dependencias.sh
   ```
   (En Windows, use `instalar_dependencias.bat`)

4. Configure los parámetros de la estación editando el archivo `config/estacion.properties`:
   ```
   id_estacion=MESA001
   servidor_central=ip_servidor:10000
   directorio_logs=/ruta/a/logs
   ```

5. Inicie la estación con el siguiente comando:
   ```
   ./iniciar_estacion.sh
   ```
   (En Windows, use `iniciar_estacion.bat`)

## Uso del Sistema

### Servidor Central

#### Panel de Administración

1. Acceda al panel de administración abriendo un navegador web y visitando:
   ```
   http://localhost:8080/admin
   ```

2. Inicie sesión con las credenciales de administrador:
   - Usuario: admin
   - Contraseña: (proporcionada por separado)

3. Desde el panel de administración podrá:
   - Monitorear el estado de las estaciones de votación
   - Ver estadísticas en tiempo real
   - Consultar logs de auditoría
   - Generar reportes de resultados

#### Monitoreo de Resultados

1. En el panel de administración, seleccione "Resultados en Tiempo Real".
2. Verá una tabla con los resultados actualizados por candidato.
3. Puede filtrar los resultados por región, ciudad o mesa.
4. Para exportar los resultados, haga clic en "Exportar" y seleccione el formato deseado (CSV, PDF, Excel).

### Estaciones de Votación

#### Proceso de Votación

1. Inicie la aplicación de la estación de votación.
2. El sistema mostrará la pantalla de identificación del votante.
3. Ingrese el número de documento del votante y haga clic en "Verificar".
4. El sistema validará la elegibilidad del votante:
   - Si es elegible, mostrará la pantalla de selección de candidatos.
   - Si no es elegible, mostrará un mensaje con el motivo.

5. El votante selecciona su candidato de preferencia.
6. Se muestra una pantalla de confirmación con el candidato seleccionado.
7. El votante confirma su elección haciendo clic en "Confirmar Voto".
8. El sistema procesa el voto:
   - Cifra y firma el voto
   - Lo almacena localmente
   - Lo transmite al servidor central
   - Muestra un mensaje de confirmación al votante

9. El sistema vuelve a la pantalla de identificación para el siguiente votante.

#### Cierre de Mesa

1. Al finalizar la jornada electoral, el administrador de la mesa debe hacer clic en "Cerrar Mesa".
2. El sistema solicitará la contraseña de administrador.
3. Una vez autenticado, el sistema:
   - Verifica que todos los votos hayan sido transmitidos
   - Genera un acta de cierre
   - Muestra un resumen de la votación en la mesa

4. Imprima el acta de cierre haciendo clic en "Imprimir Acta".
5. Cierre la aplicación haciendo clic en "Salir".

## Solución de Problemas

### Problemas de Conexión

Si una estación de votación pierde conexión con el servidor central:

1. El sistema continuará funcionando normalmente, almacenando los votos localmente.
2. Cuando se restablezca la conexión, los votos se transmitirán automáticamente.
3. Si la conexión no se restablece, al cerrar la mesa, el sistema generará un archivo de respaldo que deberá ser llevado físicamente al centro de cómputo.

### Errores Comunes

#### Error: "No se puede conectar al servidor central"

- Verifique que el servidor central esté en funcionamiento.
- Compruebe la configuración de red en el archivo `estacion.properties`.
- Verifique que no haya restricciones de firewall bloqueando la conexión.

#### Error: "Votante no encontrado"

- Verifique que el número de documento se haya ingresado correctamente.
- Compruebe que el votante esté registrado en la base de datos.
- Verifique que el votante esté asignado a la mesa correcta.

#### Error: "Votante ya ha emitido su voto"

- Verifique si el votante ya votó anteriormente.
- Si el votante asegura que no ha votado, contacte al centro de soporte.

## Contacto y Soporte

Para obtener asistencia técnica:

- Línea de soporte: 01-8000-123456
- Correo electrónico: soporte@registraduria.gov.co
- Horario de atención: 6:00 AM - 8:00 PM durante la jornada electoral

## Consideraciones de Seguridad

- No comparta las credenciales de administrador.
- No deje la estación de votación desatendida mientras esté en funcionamiento.
- Reporte inmediatamente cualquier comportamiento sospechoso.
- Siga los protocolos de seguridad establecidos por la Registraduría.

## Apéndices

### Glosario de Términos

- **Estación de Votación**: Equipo informático ubicado en una mesa de votación.
- **Servidor Central**: Sistema que recibe, verifica y consolida los votos.
- **Voto Cifrado**: Voto que ha sido transformado mediante algoritmos criptográficos para proteger su contenido.
- **Firma Digital**: Mecanismo que garantiza la autenticidad e integridad de un voto.
- **Acta de Cierre**: Documento que resume la votación en una mesa al finalizar la jornada.

### Preguntas Frecuentes

1. **¿Qué sucede si se va la luz durante la votación?**
   - Las estaciones cuentan con baterías de respaldo que permiten continuar la operación.
   - Los votos se almacenan localmente y se transmiten cuando se restablece la conexión.

2. **¿Cómo se garantiza que un voto no se cuente dos veces?**
   - El sistema implementa múltiples mecanismos de verificación para prevenir duplicados.
   - Cada voto tiene un identificador único que se verifica en el servidor central.

3. **¿Cómo se protege la privacidad del votante?**
   - El sistema separa la información del votante del contenido de su voto.
   - Los votos se cifran y no se puede relacionar un voto con un votante específico.
