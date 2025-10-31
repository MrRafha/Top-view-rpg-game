@echo off
echo ====================================
echo     RPG 2D Java - Iniciando...
echo ====================================
echo.
echo Verificando Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERRO: Java nao encontrado!
    echo Por favor, instale o Java 8 ou superior.
    echo.
    echo Download: https://www.java.com/download/
    pause
    exit /b 1
)

echo Java encontrado! Iniciando o jogo...
echo.
echo Controles:
echo - W/A/S/D: Mover
echo - Espaco: Atacar
echo - C: Caracteristicas
echo - ESC: Pausar
echo.
echo Divirta-se! ^_^
echo.

java -jar game.jar

if %errorlevel% neq 0 (
    echo.
    echo ERRO: Falha ao executar o jogo.
    echo Certifique-se de que o arquivo game.jar esta presente.
    pause
)
