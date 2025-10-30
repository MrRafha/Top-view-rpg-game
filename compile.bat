@echo off
echo Compilando RPG 2D Java...
cd src
javac *.java
if %ERRORLEVEL% EQU 0 (
    echo Compilacao concluida com sucesso!
    echo Para executar o jogo, use: run.bat
) else (
    echo Erro na compilacao!
)
pause