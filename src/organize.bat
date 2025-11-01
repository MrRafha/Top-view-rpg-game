@echo off
echo Organizando arquivos do projeto RPG...

REM Limpar arquivos .class antigos
del *.class 2>nul

REM Criar estrutura de diretorios
mkdir com\rpggame\core 2>nul
mkdir com\rpggame\entities 2>nul  
mkdir com\rpggame\ui 2>nul
mkdir com\rpggame\world 2>nul
mkdir com\rpggame\systems 2>nul

echo Estrutura de diretorios criada!
echo.
echo Agora compile com: javac -encoding UTF-8 -cp . com/rpggame/core/*.java com/rpggame/entities/*.java com/rpggame/ui/*.java com/rpggame/world/*.java com/rpggame/systems/*.java
echo Execute com: java com.rpggame.core.Game
pause