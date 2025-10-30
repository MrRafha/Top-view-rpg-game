@echo off
echo Iniciando RPG 2D Java...
cd src
java Game
if %ERRORLEVEL% NEQ 0 (
    echo Erro ao executar o jogo!
    echo Certifique-se de compilar primeiro usando: compile.bat
)
pause