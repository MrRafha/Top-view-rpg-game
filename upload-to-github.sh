#!/bin/bash
echo "Conectando ao repositório Top-view-rpg-game..."

echo "Verificando se Git está inicializado..."
if [ ! -d ".git" ]; then
    echo "Inicializando Git..."
    git init
fi

echo "Adicionando arquivos..."
git add .

echo "Fazendo commit..."
git commit -m "Complete RPG 2D Java Game v1.0.0

Features:
- 3 character classes (Warrior, Mage, Hunter)
- Full attribute system with 6 stats
- Combat system with projectiles and evasion
- Fog of War with line-of-sight algorithm
- Customizable 15x15 maps via .txt files
- Robust collision system
- Unified character creation interface
- Professional project structure"

echo "Conectando ao repositório remoto..."
git remote remove origin 2>/dev/null
git remote add origin https://github.com/MrRafha/Top-view-rpg-game.git

echo "Configurando branch principal..."
git branch -M main

echo "Fazendo push para o GitHub..."
git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Upload concluído com sucesso!"
    echo "🔗 Repositório: https://github.com/MrRafha/Top-view-rpg-game"
    echo ""
else
    echo ""
    echo "❌ Erro no upload. Verifique suas credenciais do Git."
    echo ""
fi