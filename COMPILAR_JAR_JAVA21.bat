@echo off
setlocal
cd /d "%~dp0"
echo ===== DEDsafio4Ruleta - Fabric 1.21.1 (Java 21) =====
echo Verifica que Java sea version 21:
java -version
if errorlevel 1 (
  echo ERROR: Java no esta instalado ni accesible en PATH.
  pause
  exit /b 1
)
echo.
echo Compilando el mod con Gradle...
call gradlew.bat build --no-daemon
if errorlevel 1 (
  echo.
  echo ERROR: No se ha compilado el JAR. Revisa la salida de Gradle.
  pause
  exit /b 1
)
echo.
echo BUILD SUCCESSFUL. Archivos de build/libs:
dir "build\libs\*.jar"
start "" "%~dp0build\libs"
pause
