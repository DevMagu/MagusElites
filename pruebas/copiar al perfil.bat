@echo off
rem Compila el mod y copia el jar nuevo al perfil "MagusElites Pruebas" de CurseForge.
rem Cierra Minecraft antes: con el juego abierto Windows no deja borrar el jar viejo.
chcp 65001 >nul
setlocal
set "PERFIL=%USERPROFILE%\curseforge\minecraft\Instances\MagusElites Pruebas\mods"

if not exist "%PERFIL%" (
    echo No encuentro el perfil: %PERFIL%
    echo Importa primero "MagusElites Pruebas - CurseForge.zip" en CurseForge.
    pause
    exit /b 1
)

cd /d "%~dp0.."
call gradlew.bat build
if errorlevel 1 (
    echo.
    echo La compilacion ha fallado: mira el error de arriba.
    pause
    exit /b 1
)

rem Quita la version anterior para que no haya dos jars del mismo mod.
del /q "%PERFIL%\magus_elites-*.jar" 2>nul
copy /y "build\libs\magus_elites-*.jar" "%PERFIL%\" >nul
echo.
echo Copiado al perfil:
dir /b "%PERFIL%\magus_elites-*.jar"
pause
