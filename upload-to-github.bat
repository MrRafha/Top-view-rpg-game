@echo off
echo Conectando ao repositorio Top-view-rpg-game...

echo Verificando se Git esta inicializado...
if not exist ".git" (
    echo Inicializando Git...
    git init
)

echo Adicionando arquivos...
git add .

echo Fazendo commit...
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

echo Conectando ao repositorio remoto...
git remote remove origin 2>nul
git remote add origin https://github.com/MrRafha/Top-view-rpg-game.git

echo Configurando branch principal...
git branch -M main

echo Fazendo push para o GitHub...
git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Upload concluido com sucesso!
    echo 🔗 Repositorio: https://github.com/MrRafha/Top-view-rpg-game
    echo.
) else (
    echo.
    echo ❌ Erro no upload. Verifique suas credenciais do Git.
    echo.
)
pause