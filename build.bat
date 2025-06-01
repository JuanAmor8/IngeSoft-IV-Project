@echo off
echo ===== Compilando Sistema de Votacion Electronica =====
echo.

rem Crear directorio bin si no existe
if not exist bin mkdir bin

rem Compilar clases ICE (dependencias)
echo Compilando clases ICE...
javac -d bin src/com/zeroc/Ice/*.java

rem Compilar componentes de transmisión
echo Compilando modulo de transmision...
javac -d bin -cp bin src/transmision/ice/*.java src/transmision/modelo/*.java src/transmision/util/*.java

rem Compilar componentes de recepción
echo Compilando modulo de recepcion...
javac -d bin -cp bin src/recepcion/ice/*.java src/recepcion/modelo/*.java src/recepcion/util/*.java

rem Compilar pruebas
echo Compilando pruebas...
javac -d bin -cp bin src/test/*.java

rem Compilar clase principal
echo Compilando clase principal...
javac -d bin -cp bin src/Main.java

echo.
if %errorlevel% neq 0 (
    echo ERROR: La compilacion ha fallado.
) else (
    echo Compilacion completada exitosamente!
    echo Para ejecutar el programa: java -cp bin Main
) 