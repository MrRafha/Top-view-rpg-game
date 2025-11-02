# Script para organizar arquivos do projeto RPG
Write-Host "Organizando projeto RPG..." -ForegroundColor Green

# Definir mapeamentos de arquivos para pastas
$coreFiles = @("Game.java", "GamePanel.java")
$entityFiles = @("Player.java", "Enemy.java", "Goblin.java", "GoblinFamily.java", "GoblinPersonality.java", "Projectile.java", "Structure.java")
$uiFiles = @("AttributeCustomizationScreen.java", "CharacterCreationScreen.java", "CharacterScreen.java", "CombinedCharacterScreen.java")
$worldFiles = @("TileMap.java", "TileType.java", "MapLoader.java", "Camera.java", "FogOfWar.java")
$systemFiles = @("CharacterStats.java", "ExperienceSystem.java", "EnemyManager.java", "FloatingText.java")

Write-Host "Criando estrutura de pastas..." -ForegroundColor Yellow

Write-Host "Arquivos organizados! Agora precisa ajustar os packages manualmente." -ForegroundColor Green
Write-Host "Para compilar: javac -encoding UTF-8 -cp . com/rpggame/*/*.java" -ForegroundColor Cyan
Write-Host "Para executar: java com.rpggame.core.Game" -ForegroundColor Cyan