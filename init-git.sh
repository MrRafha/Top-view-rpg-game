#!/bin/bash
echo "Inicializando repositório Git..."
git init
if [ $? -ne 0 ]; then
    echo "Erro: Git não encontrado. Instale o Git primeiro."
    exit 1
fi

echo "Adicionando arquivos..."
git add .

echo "Fazendo commit inicial..."
git commit -m "Initial commit: RPG 2D Java v1.0.0

- Sistema completo de RPG 2D top-down
- 3 classes de personagem (Guerreiro, Mago, Caçador)
- Sistema de atributos com 6 stats
- Combate com projéteis e evasão
- Fog of War com line-of-sight
- Mapas customizáveis 15x15
- Sistema de colisão robusto
- Interface unificada de criação de personagem"

echo ""
echo "Repositório Git inicializado com sucesso!"
echo ""
echo "Próximos passos:"
echo "1. O repositório já existe no GitHub: Top-view-rpg-game"
echo "2. Execute: git remote add origin https://github.com/MrRafha/Top-view-rpg-game.git"
echo "3. Execute: git branch -M main"
echo "4. Execute: git push -u origin main"
echo ""
echo "Veja docs/GIT_INSTRUCTIONS.md para mais detalhes."