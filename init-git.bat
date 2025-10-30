@echo off
echo Inicializando repositorio Git...
git init
if %ERRORLEVEL% NEQ 0 (
    echo Erro: Git nao encontrado. Instale o Git primeiro.
    pause
    exit /b 1
)

echo Adicionando arquivos...
git add .

echo Fazendo commit inicial...
git commit -m "Initial commit: RPG 2D Java v1.0.0

- Sistema completo de RPG 2D top-down
- 3 classes de personagem (Guerreiro, Mago, Cacador)
- Sistema de atributos com 6 stats
- Combate com projeteis e evasao
- Fog of War com line-of-sight
- Mapas customizaveis 15x15
- Sistema de colisao robusto
- Interface unificada de criacao de personagem"

echo.
echo Repositorio Git inicializado com sucesso!
echo.
echo Proximos passos:
echo 1. O repositorio ja existe no GitHub: Top-view-rpg-game
echo 2. Execute: git remote add origin https://github.com/MrRafha/Top-view-rpg-game.git
echo 3. Execute: git branch -M main
echo 4. Execute: git push -u origin main
echo.
echo Veja docs/GIT_INSTRUCTIONS.md para mais detalhes.
pause