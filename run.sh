#!/bin/bash
echo "Iniciando RPG 2D Java..."
cd src
java Game
if [ $? -ne 0 ]; then
    echo "Erro ao executar o jogo!"
    echo "Certifique-se de compilar primeiro usando: ./compile.sh"
fi