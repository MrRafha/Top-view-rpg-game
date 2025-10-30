#!/bin/bash
echo "Compilando RPG 2D Java..."
cd src
javac *.java
if [ $? -eq 0 ]; then
    echo "Compilacao concluida com sucesso!"
    echo "Para executar o jogo, use: ./run.sh"
else
    echo "Erro na compilacao!"
fi