@echo off
echo ======================================
echo Ejecutando sistema de votación
echo ======================================

:: Verificar si Java está instalado
java -version >nul 2>nul
if %ERRORLEVEL% neq 0 (
  echo Java no está instalado o no está en el PATH.
  echo Por favor, instale Java desde https://www.java.com/
  pause
  exit /b 1
)

:: Comprobar si existe el archivo de configuración
if not exist "config.properties" (
  echo No se encuentra el archivo de configuración config.properties.
  pause
  exit /b 1
)

:: Definir las opciones de la JVM
set JAVA_OPTS=-Xms256m -Xmx512m -Dconfig.file=config.properties

:: Ejecutar la aplicación
echo Iniciando el sistema de votación...
java %JAVA_OPTS% -cp target/sistema-votacion-1.0-SNAPSHOT.jar com.registraduria.votacion.Main

pause 